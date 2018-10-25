package com.laog.test1.demo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import com.laog.test1.R;
import com.laog.test1.inoreader.Article;
import com.laog.test1.inoreader.InoApi;

import java.io.File;
import java.util.List;

public class Zoomexample extends Activity {
    /** Called when the activity is first created. */
    final int REQUESTED = 12;
    private Zoom z;
    private String rawContent;
    private String yearMonth;
    private LinearLayout layout;
    private TextView tv;
    private int idHash;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        rawContent = getIntent().getStringExtra("rawContent");
        yearMonth = getIntent().getStringExtra("yearMonth");
        idHash = getIntent().getStringExtra("articleid").hashCode();

        //z = new Zoom(this, null);
        //        setContentView(z);
        this.requestWindowFeature(1);
        setContentView(R.layout.image_viewer);

        layout = (LinearLayout)findViewById(R.id.layout_images);
        tv = (TextView)findViewById(R.id.textView);


        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck== PackageManager.PERMISSION_GRANTED){
            //this means permission is granted and you can do read and write
            loadImages();
        }else{
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUESTED);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUESTED: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    loadImages();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    private File basePath;
    private void loadImages() {
        File sdcard = Environment.getExternalStorageDirectory();
        basePath = new File(sdcard, "ino/"+yearMonth);
        if(!basePath.exists()){
            basePath.mkdirs();
        }
        tv.setText("rootDir:"+basePath.toString());

        try {
            List<String> imgs = Article.parseImageLinks(rawContent);
            if(imgs != null) {
                tv.setText("loading images "+imgs.size());
                new FetchTask().execute(imgs);
            }else{
                tv.setText("no images found!!");
            }
        }catch(Exception ex){
            Log.e("", "error parsing", ex);
        }
//        File file = new File(basePath,"demo2.png");
//        z.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
    }

    private class FetchTask extends AsyncTask<List<String>, String, Boolean> {
        private volatile String msg;

        protected void onProgressUpdate(String... values) {
            Zoom z1 = new Zoom(Zoomexample.this, null);
            z1.setImageBitmap(BitmapFactory.decodeFile(values[0]));
//            layout.addView(z1, (int)z1.getOrigWidth(), (int)z1.getOrigHeight());
            LayoutParams LParams = new LayoutParams(LayoutParams.MATCH_PARENT, (int)z1.getOrigHeight());
            layout.addView(z1, LParams);
            if(msg != null)
                tv.setText(msg);
        }

        protected Boolean doInBackground(List<String>... voids) {
            InoApi n = new InoApi(null, null, false);
            int total = voids[0].size();
            int nn = 0;
            for(String imglink: voids[0]) {
                String fname = imglink.substring(imglink.lastIndexOf('/')+1);
                nn ++;
                msg = "loaded images: "+nn + " / " + total;
                if(fname.indexOf('?') > 0)
                    fname = fname.substring(0, fname.indexOf('?'));
                String fpath = basePath.getAbsolutePath() + "/" +idHash +"-" + fname;
                File f1 = new File(fpath);
                if(f1.exists()) {
                    publishProgress(f1.getAbsolutePath());
                    continue;
                }
                try {
                    n.get(imglink, fpath);
                    if(f1.exists()){
                        this.publishProgress(fpath);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            tv.setText("all loaded!");
        }
    }
}