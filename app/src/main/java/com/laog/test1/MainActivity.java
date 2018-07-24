package com.laog.test1;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.view.View;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.laog.test1.inoreader.Article;
import com.laog.test1.inoreader.Inoreader;
import com.laog.test1.inoreader.Utils;
import com.laog.test1.util.FloatPickerWidget;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener{

    private TextToSpeech tts;
    private TextView ed1;
    private TextView ed2;
//    private FloatPickerWidget edSpeed;
    private TextView edSpeed;
    private Button bt1;
    private Button bt2;
    private float speed = 2.5f;

    private Inoreader inoreader;
    private boolean isInit = false;
    private FetchTask task;
    private boolean reading = false;

    @Override
    protected void onDestroy() {
        if(tts.isSpeaking())
            tts.stop();
        tts.shutdown();
        Log.d("", "destroy, tts shutdown!");
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ed1=(TextView)findViewById(R.id.textView2);
        ed2 = (TextView) findViewById(R.id.textView);
        ed2.setMovementMethod(new ScrollingMovementMethod());

        edSpeed = (TextView) findViewById(R.id.ed_speed);
        edSpeed.setText(String.valueOf(speed));
//        edSpeed = (FloatPickerWidget) findViewById(R.id.ed_speed);
//        edSpeed.setValue(speed);
//        edSpeed.onValueChange(new FloatPickerWidget.ChangeListener(){
//            public void onValueChange(FloatPickerWidget fpw, float value) {
//                speed = value;
//            }
//        });

        tts =new TextToSpeech(getApplicationContext(), this);
        tts.setOnUtteranceProgressListener(new MyUtteranceProgressListener());

        bt1 = (Button)findViewById(R.id.button_stop);
        bt1.setOnClickListener(new View.OnClickListener() {
         public void onClick(View v) {
            if(task != null) task.read_or_stop();
//             showDialog();
         }
      });

        bt2 = (Button)findViewById(R.id.button_down);
        bt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bt2.setEnabled(false);
                ed2.setText("Downloading...");
                task = new FetchTask();
                task.execute((Void)null);
            }
        });
        ((Button)findViewById(R.id.button_back)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(task != null) task.back();
            }
        });
        ((Button)findViewById(R.id.button_forward)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(task != null) task.forward();
            }
        });
        ((Button)findViewById(R.id.bt_spdup)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speed += 0.1f;
                edSpeed.setText(String.valueOf(speed));
            }
        });
        ((Button)findViewById(R.id.bt_spddown)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speed -= 0.1f;
                edSpeed.setText(String.valueOf(speed));
            }
        });

        String rootdir = getFilesDir().getAbsolutePath();
        inoreader = new Inoreader(rootdir);
        ed1.setText(rootdir);

        myReceiver = new ReceiveMessages();
        task = new FetchTask();
        //task.initLoad();
    }

    /**
     *  Of TextToSpeech.OnInitListener
     * @param status
     */
    @Override
    public void onInit(int status) {
        if(status != TextToSpeech.ERROR) {
//               tts.setLanguage(Locale.UK);
            int result = tts.setLanguage(Locale.CHINESE);
            if(result == TextToSpeech.LANG_NOT_SUPPORTED){
                ed2.setText("ERROR:  LANG_NOT_SUPPORTED");
            }else if(result == TextToSpeech.LANG_MISSING_DATA){
                Intent installTTSIntent = new Intent();
                installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTTSIntent);
            }else{
                isInit = true;
            }

            //tts.setSpeechRate(2.0f);
            if(task != null)
                task.initLoad();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!myReceiverIsRegistered) {
            registerReceiver(myReceiver, new IntentFilter(message_type));
            myReceiverIsRegistered = true;
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (myReceiverIsRegistered) {
            unregisterReceiver(myReceiver);
            myReceiverIsRegistered = false;
        }
    }

    private final String message_type = "gyf.laog.test.SHOW_CONTENT";
    private ReceiveMessages myReceiver = null;
    private Boolean myReceiverIsRegistered = false;
    private class ReceiveMessages extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if(action.equalsIgnoreCase(message_type)){
                ed2.setText(task.content);
                ed2.scrollTo(0, 0);
                ed1.setText(task.idx + " / " + task.la.size());
            }
        }
    }

    private class MyUtteranceProgressListener extends UtteranceProgressListener{
        @Override
        public void onStart(String utteranceId) { Log.d("", "speak start!"); }

        @Override
        public void onDone(String utteranceId) {
            Log.d("", "speak done!");
            if(task != null)
                task.next();
        }

        @Override
        public void onError(String utteranceId) { }
    }


    private class FetchTask extends AsyncTask<Void, Void, Boolean> {
        private String content;
        private List<Article> la;
        private int idx;
        private LinkedList<String> ll = new LinkedList<>();

        void initLoad() {
            try {
                la = inoreader.initLoadFile();
                if(la == null)
                    return;
                Collections.reverse(la);
                onPostExecute(true);
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }

        protected void back() {
            if(la == null) return;
            if(idx <= 1) return;
            ll.clear();
            idx -= 2;
            if(tts.isSpeaking())
                tts.stop();
            next();
        }

        protected void forward() {
            if(la == null) return;
            if(idx >= la.size()) return;
            ll.clear();
            if(tts.isSpeaking())
                tts.stop();
            next();
        }
        protected void read_or_stop() {
            if(reading) {
                if (tts.isSpeaking())
                    tts.stop();
                reading = false;
                bt1.setText(R.string.str_start);
            }else{
                reading = true;
                bt1.setText(R.string.str_stop);
                next();
            }
        }

        /**
         *  一段读完后自动转为下一段
         */
        protected void next() {
            if(ll.size() > 0){
                tts.speak(ll.removeFirst(), TextToSpeech.QUEUE_FLUSH, null, "p");
            }else{
                if(la == null || idx < 0 || idx >= la.size())
                    return;
                Article art = la.get(idx); idx += 1;

                speed = Float.parseFloat(edSpeed.getText().toString());
                tts.setSpeechRate(speed);
                Utils.log("=========speechRate= "+speed);

                content = art.title + "\n\n" + art.content;
                Intent i = new Intent(message_type);
                ed2.getContext().sendBroadcast(i);
//                ed2.setText(art.title + "\n\n" + art.content);
//                ed2.scrollTo(0, 0);
//                ed1.setText(idx + " / " + la.size());

                if(reading) {
                    tts.speak(art.title, TextToSpeech.QUEUE_FLUSH, null, "idtitle");
                    for (String s : art.content.split("。"))
                        ll.add(s);
                }
            }
        }
        @Override
        protected Boolean doInBackground(Void... voids) {
            try{
                inoreader.start();
                la = inoreader.fetch();
                Collections.reverse(la);
            }catch(Exception ex) {
                content = ex.getMessage();
            }
            if(la == null)
                return false;
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                idx = 0;
                next();
            } else {
                Toast.makeText(MainActivity.this,
                        "failed",
                        Toast.LENGTH_LONG).show();
                ed2.setText(content);
            }
            bt2.setEnabled(true);
        }
    }
}
