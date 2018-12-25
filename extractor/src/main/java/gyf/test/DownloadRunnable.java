package gyf.test;

//import android.util.Log;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

import static org.schabi.newpipe.BuildConfig.DEBUG;

/**
 * Runnable to download blocks of a file until the file is completely downloaded,
 * an error occurs or the process is stopped.
 */
public class DownloadRunnable implements Runnable {

    private static final String TAG = DownloadRunnable.class.getSimpleName();

    private final DownloadMission mMission;
    private final int mId;
    private boolean running = true;

    public DownloadRunnable(DownloadMission mission, int id) {
        if (mission == null) throw new NullPointerException("mission is null");
        mMission = mission;
        mId = id;
    }

    private int position;
    protected int currentBlock() {
        return position;
    }
    protected boolean isRunning() { return running; }

    @Override
    public void run() {
        running = true;
        while (mMission.errCode == -1 && mMission.running )
        {
            Integer pos = mMission.reqBlock();
            if(pos == null)
                break;
            position = pos.intValue();
            Log.d(TAG, mId + ":preserving position " + position);

            long start = position * mMission.BLOCK_SIZE;
            long end = start + mMission.BLOCK_SIZE - 1;

            if (end >= mMission.length) {
                end = mMission.length - 1;
            }

            HttpURLConnection conn = null;
            int total = 0;
            try {
                conn = mMission.getConn();
                conn.setRequestProperty("Range", "bytes=" + start + "-" + end);

                if (DEBUG) {
//                    Log.d(TAG, mId + ":" + conn.getRequestProperty("Range"));
//                    Log.d(TAG, mId + ":Content-Length=" + conn.getContentLength() + " Code:" + conn.getResponseCode());
                }

                // A server may be ignoring the range request
                if (conn.getResponseCode() != 206) {
                    conn.disconnect();
                    mMission.errCode = DownloadMission.ERROR_SERVER_UNSUPPORTED;
                    notifyError();
                    break;
                }

                java.io.InputStream ipt = conn.getInputStream();
                byte[] buf = new byte[64*1024];

                while (start < end && mMission.running) {
                    int len = ipt.read(buf, 0, buf.length);

                    if (len == -1) {
                        break;
                    } else {
                        mMission.writeFile(buf, len, start);
                        start += len;
                        total += len;
                        notifyProgress(len);
                    }
                }

                if (DEBUG && mMission.running) {
                    Log.d(TAG, mId + ":position " + position + " finished, total length " + total);
                }
                ipt.close();

                mMission.blockFinish(position);
            } catch (Exception e) {
                notifyProgress(-total);
                mMission.blockFailed(position);
                if (DEBUG) {
                    Log.e(TAG, mId + ":position " + position + " retrying: "+e.getMessage());
                }
                try{
                    if(conn != null) conn.disconnect();
                }catch (Exception ee){}
            }
        }

        if (DEBUG) {
            Log.d(TAG, "thread " + mId + " exited main loop");
        }

        if (mMission.errCode == -1 && mMission.running) {
            if (DEBUG) {
                Log.d(TAG, "no error has happened, notifying");
            }
            notifyFinished();
        }
        running = false;
    }

    private void notifyProgress(final long len) {
        synchronized (mMission) {
            mMission.notifyProgress(len);
        }
    }

    private void notifyError() {
        synchronized (mMission) {
            mMission.notifyError(DownloadMission.ERROR_SERVER_UNSUPPORTED);
            mMission.pause();
        }
    }

    private void notifyFinished() {
        synchronized (mMission) {
            mMission.notifyFinished();
        }
    }
}
