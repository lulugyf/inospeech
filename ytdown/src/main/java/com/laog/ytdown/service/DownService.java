package com.laog.ytdown.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import gyf.test.DownloadMission;
import gyf.test.YoutubeTest;

import static java.lang.Thread.sleep;

public class DownService extends Service {
    private final String TAG = "DownService";
    private YoutubeTest yt;
    private ConcurrentHashMap<String, VInfo> dlist = new ConcurrentHashMap<>();
    private DownBinder binder;
    private String path;

    public class DownBinder extends Binder {
        public long getTotal() { return total; }
        public long getDone() { return done; }
        public String getVid() {
            if(current != null)
                return current.vid;
            else
                return null;
        }
        public void stop() {
            running = false;
        }
    }

    private static class VInfo{
        String path;String url;boolean isAudio;String fmt; String solution;String name;
        String vid;
        static VInfo from(Intent i) {
            if(!i.hasExtra("url"))
                return null;
            VInfo v = new VInfo();
            v.path = i.getStringExtra("path");
            v.url = i.getStringExtra("url");
            v.isAudio = i.getBooleanExtra("isAudio", true);
            v.fmt = i.getStringExtra("fmt");
            v.solution = i.getStringExtra("solution");
            v.name = i.getStringExtra("name");
            return v;
        }
    }

    private volatile VInfo current;

    //必须要实现的方法
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind方法被调用!");
        return binder;
    }

    private volatile boolean running;
    //Service被创建时调用
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate方法被调用!");
        yt = new YoutubeTest();
        super.onCreate();
        running = true;
        new Thread(() -> {
            while(running) {
                if(dlist.size() == 0){
                    try {
                        sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                String url = dlist.keys().nextElement();
                VInfo v = dlist.get(url);
                try{
                    downfunc(v);
                    Log.w(TAG, "url download success!" + url);
                }catch(Exception ex) {
                    ex.printStackTrace();
                    continue;
                }
                dlist.remove(url);
                current = null;
                total = -1;
                done = -1;
            }
            Log.d(TAG, "download thread exit!");
        }).start();
        binder = new DownBinder();
    }



    private long total;
    private volatile long done;
    private void downfunc(VInfo v) throws Exception {
        YoutubeTest.Stream stream = yt.getDetail(v.url, v.name, "");
        v.name = stream.name;
        String durl = null;
        String reslo = null;
        for(YoutubeTest.FormatItem fi : stream.items){
            if(v.fmt.equals(fi.fmt)) { // && v.solution.equals(v.solution)
                durl = fi.url;
                reslo = fi.resolution;
                break;
            }
        }
        if(durl == null)
            throw new Exception("format "+v.fmt + " not found for "+v.url);
        current = v;
        v.vid = v.url.substring(v.url.lastIndexOf('=')+1);
        Log.d(TAG, "found url:: "+durl);
        DownloadMission dm = new DownloadMission(v.vid + "-"+reslo+"." + v.fmt, durl, v.path);
//        dm.setProxy("127.0.0.1", 8083);
        dm.setTimeout(60000);
        dm.addListener(new DownloadMission.MissionListener(){
            public void onProgressUpdate(DownloadMission downloadMission, long done, long total) {
//                DownService.this.total = total;
//                DownService.this.done = done;
//                System.out.printf("finish: %.2f %%\n", (done*100.0)/total);
            }
            public void onFinish(DownloadMission downloadMission) {
                System.out.println("Finished!");
            }
            public void onError(DownloadMission downloadMission, int errCode) {
                System.out.println("Failed:"+errCode);
            }
        });
        dm.start();

        long last_done = 0;
        while(! dm.isFinished() ){
            Thread.sleep(2000L);
            done = dm.done;
            total = dm.length;
            System.out.printf("finish: %.2f %%  %.2f kbps\n", (done*100.0)/total, (done-last_done) /1024.0/2.0);
            last_done = done;
        }
        File f1 = new File(v.path + "/" + v.vid + "-"+reslo+"." + v.fmt);
        File f2 = new File(v.path + "/" + v.name+"-"+ v.vid + "-"+reslo+"." + v.fmt);
        f1.renameTo(f2);
    }

    //Service被启动时调用
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand方法被调用!");
        // need parameters: path, url, isAudio, fmt, solution
        VInfo v = VInfo.from(intent);
        if(v != null) {
            if (dlist.containsKey(v.url)) {
                Log.e(TAG, v.url + " already in down list");
            } else {
                dlist.put(v.url, v);
                Log.w(TAG, "download " + v.url);
            }
        }else{
            try {
                path = intent.getStringExtra("path");
                reloadUnfinishedTask(path);
            }catch (Throwable e){
                Log.e(TAG, "reload failed", e);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 加载没有完成文件下载任务
     */
    private void reloadUnfinishedTask(String path) throws Exception{
        Log.d(TAG, "--: " + path);
        File f = new File(path);
        for(String fname: f.list()) {
            if(fname.endsWith(".pos")){
                // DMkmvxYJUUQ-128.m4a.pos
                String[] ff = fname.split("[-|.]");
                VInfo v = new VInfo();
                v.url = "https://www.youtube.com/watch?v=" + ff[0];
                v.name = "";
                v.isAudio = true;
                v.solution = ff[1];
                v.fmt = ff[2];
                v.path = this.path;
                v.vid = ff[0];
                dlist.put(v.url, v);
                Log.d(TAG, "   reload: "+v.url);
            }
        }

    }
    //Service被关闭之前回调
    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestory方法被调用!");
        running = false;
        super.onDestroy();
    }


    public static boolean checkServiceRunning(Context ctx) {
//        System.out.println("----" + DownService.class.getName());
        String sname = DownService.class.getName();
        ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            System.out.println(service.service.getClassName()  + " "+ sname);
            if(sname.equals(service.service.getClassName()))
                return true;
        }
        return false;
    }

    public static void main(String[] args) {
        String fname = "DMkmvxYJUUQ-128.m4a.pos";
        String[] ff = fname.split("[-|.]");
        System.out.println(ff.length + " "+ff[0]);
    }
}
