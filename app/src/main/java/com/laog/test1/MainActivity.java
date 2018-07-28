package com.laog.test1;

import android.app.Activity;
import android.arch.persistence.room.Room;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.method.MovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.laog.test1.db.AppDatabase;
import com.laog.test1.inoreader.InoApi;
import com.laog.test1.inoreader.InoreaderAn;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public final class MainActivity extends Activity implements OnInitListener {
    private TextToSpeech tts;
    private TextView ed1;
    private TextView ed2;
    private TextView edSpeed;
    private Button bt1;
    private Button bt2;
    private float speed = 2.5F;
    private boolean isInit;
    private FeedBundle task;
    private boolean reading;
    private final String message_type = "gyf.laog.test.SHOW_CONTENT";
    private MainActivity.ReceiveMessages myReceiver;
    private Boolean myReceiverIsRegistered = false;
    private AppDatabase db;
    private InoApi inoApi;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(1);
        this.setContentView(R.layout.activity_main);

        ed1 = (TextView) findViewById(R.id.textView2);
        ed2 = (TextView) findViewById(R.id.textView);
        ed2.setMovementMethod(new ScrollingMovementMethod());

        edSpeed = (TextView) findViewById(R.id.ed_speed);
        edSpeed.setText(speed + "");

        tts = new TextToSpeech(this.getApplicationContext(), (OnInitListener) this);
        tts.setOnUtteranceProgressListener((UtteranceProgressListener) (new MyUtteranceProgressListener()));

        bt1 = (Button) findViewById(R.id.button_stop);
        bt1.setOnClickListener((OnClickListener) (new OnClickListener() {
            public final void onClick(View it) {
                if (task != null) task.read_or_stop();
            }
        }));
        bt2 = (Button) findViewById(R.id.button_down);
        bt2.setOnClickListener((OnClickListener) (new OnClickListener() {
            public final void onClick(View it) {
                bt2.setEnabled(false);
                ed2.setText("downloading...");
                task.download();
            }
        }));
        findViewById(R.id.button_back).setOnClickListener((OnClickListener) (new OnClickListener() {
            public final void onClick(View it) {
                if (task != null) task.back();
            }
        }));
        findViewById(R.id.button_forward).setOnClickListener((OnClickListener) (new OnClickListener() {
            public final void onClick(View it) {
                if (task != null) task.forward();
            }
        }));
        findViewById(R.id.bt_spdup).setOnClickListener((OnClickListener) (new OnClickListener() {
            public final void onClick(View it) {
                speed += 0.1F;
                edSpeed.setText(String.format("%.2f", speed));
                tts.setSpeechRate(MainActivity.this.speed);
            }
        }));
        findViewById(R.id.bt_spddown).setOnClickListener((OnClickListener) (new OnClickListener() {
            public final void onClick(View it) {
                speed -= 0.1F;
                edSpeed.setText(String.format("%.2f", speed));
                tts.setSpeechRate(MainActivity.this.speed);
            }
        }));
        // getExternalStoragePublicDirectory()
        // getExternalFilesDir()
//        String rootdir = getFilesDir().getAbsolutePath().toString();
//        inoreader = new InoreaderAn(rootdir, false);
//      ed1.setText(rootdir);

        myReceiver = new ReceiveMessages();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mn_backup:
                Log.d("", "menu backup clicked");
                new FetchTask(5).execute();
                break;
            case R.id.mn_archive:
                Log.d("", "menu archive clicked");
                new FetchTask(4).execute();
                break;
            case R.id.mn_help:
                Log.d("", "menu help clicked");
                new FetchTask(3).execute();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        /* act  0-load file  1- load old items 2- download  3- state  4- archive 5- backup*/
        return true;
    }

    public final boolean isExternalStorageWritable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public final boolean isExternalStorageReadable() {
        final String v = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(v) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(v);
    }

    public final void runTask(int tp) {
        new FetchTask(tp).execute();
    }

    public final void sendMessage() {
        Intent i = new Intent(this.message_type);
        this.sendBroadcast(i);
    }

    public void onInit(int status) {
        if (status != TextToSpeech.ERROR) {
            //               tts.setLanguage(Locale.UK);
            int result = tts.setLanguage(Locale.CHINESE);
            if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ed2.setText("ERROR:  LANG_NOT_SUPPORTED");
            } else if (result == TextToSpeech.LANG_MISSING_DATA) {
                Intent installTTSIntent = new Intent();
                installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTTSIntent);
            } else {
                isInit = true;
            }
            task = new FeedBundle(tts, bt1, this);
            new FetchTask(0).execute();
        }
    }

    protected void onResume() {
        super.onResume();
        if (!myReceiverIsRegistered) {
            registerReceiver(myReceiver, new IntentFilter(message_type));
            myReceiverIsRegistered = true;
        }
        myReceiver.onReceive(this, new Intent(message_type));
    }

    protected void onPause() {
        super.onPause();
        if (myReceiverIsRegistered) {
            this.unregisterReceiver((BroadcastReceiver) this.myReceiver);
            this.myReceiverIsRegistered = false;
        }

    }

    protected void onDestroy() {
        if (tts.isSpeaking())
            tts.stop();
        tts.shutdown();
        Log.d("", "destroy, tts shutdown!");
        if (db != null)
            db.close();

        super.onDestroy();
    }

    public final void toast(String msg) {
        Toast.makeText((Context) this, (CharSequence) msg, Toast.LENGTH_LONG).show();
    }


    private final class ReceiveMessages extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (message_type.equals(action)) {
                if (MainActivity.this.task == null) {
                    return;
                }
                ed2.setText(task.getContent());
                ed2.scrollTo(0, 0);
                ed1.setText(task.indicate());
            }
        }
    }

    private final class MyUtteranceProgressListener extends UtteranceProgressListener {
        public void onStart(String utteranceId) {     }
        public void onDone(String utteranceId) {
            if (task != null) {
                boolean ret = task.next();
                if (!ret) {
                    MainActivity.this.reading = false;
                }
            }
        }
        public void onError(String utteranceId) {  }
    }


    private class FetchTask extends AsyncTask<Void, Void, Boolean> {
        private List lf;
        private String content;
        private final int act;

        /* act  0-load file  1- load old items 2- download  3- state  4- archive 5- backup*/
        public FetchTask(int act) {
            this.act = act;
        }
        protected void onProgressUpdate(Void... values) {
            ed2.setText((CharSequence) this.content);
        }

        protected Boolean doInBackground(Void... voids) {
            try {
                if (this.act == 0) {
                    if (db == null)
                        db = Room.databaseBuilder(getApplicationContext(),
                                AppDatabase.class, "inofeeds").build();
                    if (inoApi == null)
                        inoApi = new InoApi(getFilesDir().getAbsolutePath().toString(), db.feedItemDao(), false);
                    lf = inoApi.loadnew();
                } else if (this.act == 1) {
                    inoApi.loadold();
                } else if(act == 2){
                    __download();
                }else if(act == 3)
                    inoApi.state();
                else if(act == 4)
                    inoApi.archive(getExternalFilesDir("arch").getAbsolutePath());
                else if(act == 5)
                    inoApi.backup(MainActivity.this.getExternalFilesDir("back").getAbsolutePath());
            } catch (Exception var4) {
                this.content = var4.getMessage();
                var4.printStackTrace();
            }

            return this.lf == null ? false : true;
        }

        protected void onPostExecute(Boolean success) {
            if (success) {
                task.loaded(lf);
            } else {
                if(content != null)
                    ed2.setText((CharSequence) this.content);
            }
            bt2.setEnabled(true);
        }

        public final int getAct() {
            return this.act;
        }
        private void __download() throws Exception {
            String c = null;
            while (true) {
                String[] r = inoApi.download(c);
                if (r == null) {
                    break;
                }
                c = r[0];
                content = "download count: " + r[1] + " continue with: " + c;
                if (c == null) {
                    break;
                }
                this.publishProgress(new Void[0]);
            }
            lf = inoApi.loadnew();
        }
    }
}
