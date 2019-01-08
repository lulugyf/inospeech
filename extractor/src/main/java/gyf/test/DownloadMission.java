package gyf.test;

import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//import us.shandian.giga.util.Utility;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import static org.schabi.newpipe.BuildConfig.DEBUG;

public class DownloadMission implements Serializable {
    private static final long serialVersionUID = 0L;
    protected int BLOCK_SIZE = 512 * 1024;

    private static final String TAG = DownloadMission.class.getSimpleName();

    public interface MissionListener {
        HashMap<MissionListener, Handler> handlerStore = new HashMap<>();

        void onProgressUpdate(DownloadMission downloadMission, long done, long total);

        void onFinish(DownloadMission downloadMission);

        void onError(DownloadMission downloadMission, int errCode);
    }

    public static final int ERROR_SERVER_UNSUPPORTED = 206;
    public static final int ERROR_UNKNOWN = 233;

    /**
     * The filename
     */
    public String name;

    /**
     * The url of the file to download
     */
    public String url;

    /**
     * The directory to store the download
     */
    public String location;

    /**
     * Number of blocks the size of { BLOCK_SIZE}
     */
    public long blocks;

    /**
     * Number of bytes
     */
    public long length = -1;

    private List<DownloadRunnable> threads; // 保存在运行的线程对象
    private RandomAccessFile af; // 打开的写文件

    public volatile long done; //Number of bytes downloaded
    public int threadCount = 3;
    public int finishCount;
    public boolean running;
    volatile public boolean finished;
    public boolean fallback;
    public int errCode = -1;
    public long timestamp;

    public transient boolean recovered;

    private transient ArrayList<WeakReference<MissionListener>> mListeners = new ArrayList<>();
    private transient boolean mWritingToFile;

    private static final int NO_IDENTIFIER = -1;

    public DownloadMission() {
    }

    public DownloadMission(String name, String url, String location) {
        if (name == null) throw new NullPointerException("name is null");
        if (name.isEmpty()) throw new IllegalArgumentException("name is empty");
        if (url == null) throw new NullPointerException("url is null");
        if (url.isEmpty()) throw new IllegalArgumentException("url is empty");
        if (location == null) throw new NullPointerException("location is null");
        if (location.isEmpty()) throw new IllegalArgumentException("location is empty");
        this.url = url;
        this.name = name;
        this.location = location;
    }

    private String proxy_host = null;
    private int proxy_port = 8083;
    private int timeout = -1;
    public void setProxy(String host, int port) {
        this.proxy_host = host;
        this.proxy_port = port;
    }
    public void setTimeout(int t) { this.timeout = t; }


    private void checkBlock(long block) {
        if (block < 0 || block >= blocks) {
            throw new IllegalArgumentException("illegal block identifier");
        }
    }

    protected HttpURLConnection getConn() throws IOException {
        URL url = new URL(this.url);
        HttpURLConnection conn = null;
        if(proxy_host != null) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy_host, proxy_port));
//            System.out.println("--DownloadRunnable: use proxy "+proxy_host+":"+proxy_port);
            conn = (HttpURLConnection) url.openConnection(proxy);
        }else {
            conn = (HttpURLConnection) url.openConnection();
        }
        if(timeout > 0)
            conn.setReadTimeout(timeout);
        return conn;
    }

    /**
     * 保存下载断点:
     *  文件名: localtion + "/" + name + ".pos"
     *  内容格式:
     *      length,blocksize,minblock
     *      文件的大小, 块大小, 当前下载完成的最小块序号
     */
    private void savePos(int pos) {
        File f = new File(location + "/" + name + ".pos");
        String s = length + "," + BLOCK_SIZE + ","+pos;
        try {
            OutputStream fout = new FileOutputStream(f);
            fout.write(s.getBytes());
            fout.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    private int loadPos() {
        File f = new File(location + "/" + name + ".pos");
        if(!f.exists())
            return 0;
        int pos = 0;
        try{
            InputStream fin = new FileInputStream(f);
            byte[] buf = new byte[64];
            int r = fin.read(buf);
            if(r > 0){
                String s = new String(buf, 0, r);
                String[] fs = s.split(",");
                length = Long.parseLong(fs[0]);
                BLOCK_SIZE = Integer.parseInt(fs[1]);
                pos = Integer.parseInt(fs[2])+1;
            }
            fin.close();
        }catch(IOException e){
            e.printStackTrace();
        }
        return pos;
    }

    LinkedBlockingDeque<Integer> block_q = new LinkedBlockingDeque<Integer>(); //待下载的块序号
    public int checkLength() {
        int pos = loadPos();
        if(length <= 0) {
            try { // 检测是否支持断点续传
                HttpURLConnection conn = getConn();
//            conn.setRequestProperty("Range", "bytes=" + (length - 10) + "-" + length);
                conn.setRequestProperty("Range", "bytes=0-");

                if (conn.getResponseCode() != 206) {
                    // Fallback to single thread if no partial content support
                    fallback = true;
                    Log.d(TAG, "falling back");
                } else {
                    try {
                        length = Long.parseLong(conn.getHeaderField("Content-Length"));
                        System.out.println("---length:" + length);
                    } catch (NumberFormatException e) {
                        System.out.println("get content-length failed: " + e);
                        conn.disconnect();
                        return -2;
                    }
                }
                conn.disconnect();

                if (!fallback) {
                    new File(location).mkdirs();
                    new File(location + "/" + name).createNewFile();
                    RandomAccessFile af = new RandomAccessFile(location + "/" + name, "rw");
                    af.setLength(length);
                    af.close();
                }
            } catch (Throwable e) {
                Log.e(TAG, "checkLength failed", e);
                return -2;
            }
        }
        if(length > 0) {
            blocks = length / BLOCK_SIZE;

            if (threadCount > blocks) {
                threadCount = (int) blocks;
            }
            if (threadCount <= 0) {
                threadCount = 1;
            }

            if (blocks * BLOCK_SIZE < length) {
                blocks++;
            }
            done = pos * BLOCK_SIZE;
            for(; pos<blocks; pos++)
                block_q.add(pos);
        }
        return 0;
    }

    // 取下一个下载块序号
    protected Integer reqBlock() {
        return block_q.poll();
    }
    // 写文件
    protected void writeFile(byte[] buf, int len, long start) {
        synchronized (block_q){
            try {
                af.seek(start);
                af.write(buf, 0, len);
//                done += len;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // 一块下载完成
    protected void blockFinish(int last_block) {
        // 检查完成的块, 判断是否保存进度
        synchronized (block_q) {
            boolean shouldSave = true;
            for (DownloadRunnable dr : threads) {
                if (dr.currentBlock() < last_block) {
                    shouldSave = false;
                    break;
                }
            }
            if (shouldSave) {
                savePos(last_block);
                Log.d(TAG, "--- save block pos:"+last_block);
                // 如果有一块落在后边比较远的完成了, 保存的进度就失真严重,
                // 所以这种方式相对保守, 不稳定网络下会有较多重复传输的数据
            }
        }
    }
    // 一块下载失败, 退回队列继续等待下载, 放在头上
    protected void blockFailed(int last_block) {
        block_q.addFirst(last_block);
    }
    public boolean isRunning() {
        for(DownloadRunnable dr: threads){
            if(dr.isRunning())
                return true;
        }
        return false;
    }
    public boolean isFinished() {
        return !isRunning() && block_q.size() == 0;
    }

    public synchronized void notifyProgress(long deltaLen) {
        done += deltaLen;

        if (done > length) {
            done = length;
        }
//        Log.d(TAG, String.format("finish: %.2f %%", (done*100.0)/length));

        for (WeakReference<MissionListener> ref : mListeners) {
            final MissionListener listener = ref.get();
            if (listener != null) {
//                MissionListener.handlerStore.get(listener).post(new Runnable() {
//                    @Override
//                    public void run() {
//                        listener.onProgressUpdate(DownloadMission.this, done, length);
//                    }
//                });
                listener.onProgressUpdate(DownloadMission.this, done, length);
            }
        }
    }

    /**
     * Called by a download thread when it finished.
     */
    public synchronized void notifyFinished() {
        if (errCode > 0) return;

        finishCount++;

        if (finishCount == threadCount) {
            onFinish();
        }
    }

    /**
     * Called when all parts are downloaded
     */
    private void onFinish() {
        if (errCode > 0) return;

        if (DEBUG) {
            Log.d(TAG, "onFinish");
        }

        running = false;
        finished = true;
        try{ //删除断点文件
            File f = new File(location + "/" + name + ".pos");
            if(f.exists())
                f.delete();
        }catch (Exception e){
            e.printStackTrace();
        }

        for (WeakReference<MissionListener> ref : mListeners) {
            final MissionListener listener = ref.get();
            if (listener != null) {
//                MissionListener.handlerStore.get(listener).post(new Runnable() {
//                    @Override
//                    public void run() {
//                        listener.onFinish(DownloadMission.this);
//                    }
//                });
                listener.onFinish(this);
            }
        }
    }

    public synchronized void notifyError(int err) {
        errCode = err;

        for (WeakReference<MissionListener> ref : mListeners) {
            final MissionListener listener = ref.get();
            MissionListener.handlerStore.get(listener).post(new Runnable() {
                @Override
                public void run() {
                    listener.onError(DownloadMission.this, errCode);
                }
            });
        }
    }

    public synchronized void addListener(MissionListener listener) {
//        Handler handler = new Handler(Looper.getMainLooper());
//        MissionListener.handlerStore.put(listener, handler);
        mListeners.add(new WeakReference<>(listener));
    }

    public synchronized void removeListener(MissionListener listener) {
        for (Iterator<WeakReference<MissionListener>> iterator = mListeners.iterator();
             iterator.hasNext(); ) {
            WeakReference<MissionListener> weakRef = iterator.next();
            if (listener != null && listener == weakRef.get()) {
                iterator.remove();
            }
        }
    }

    /**
     * Start downloading with multiple threads.
     */
    public void start() {
        if(length <= 0)
            checkLength();
        if (!running && !finished) {
            running = true;

            if (!fallback) {
                try {
                    af = new RandomAccessFile(location + "/" + name, "rw");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    errCode = 15;
                    finished = true;
                    return;
                }
                threads = new ArrayList<>(threadCount);
                for (int i = 0; i < threadCount; i++) {
                    DownloadRunnable run = new DownloadRunnable(this, i);
                    threads.add(run);
                    new Thread(run).start();
                }
            } else {
                // In fallback mode, resuming is not supported.
                threadCount = 1;
                done = 0;
                blocks = 0;
//                new Thread(new DownloadRunnableFallback(this)).start(); //guanyf
            }
        }
    }
}
