package com.laog.test1

import android.app.Activity
import android.arch.persistence.room.Room
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Button
import android.view.View
import android.view.Window

import java.util.LinkedList
import java.util.Locale

import android.widget.TextView
import android.widget.Toast
import com.laog.test1.db.AppDatabase
import com.laog.test1.db.FeedItem

import com.laog.test1.inoreader.Article
import com.laog.test1.inoreader.InoApi
import com.laog.test1.inoreader.InoreaderAn
import com.laog.test1.inoreader.Utils
import kotlin.math.max

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ed1: TextView? = null
    private var ed2: TextView? = null
    //    private FloatPickerWidget edSpeed;
    private var edSpeed: TextView? = null
    private var bt1: Button? = null
    private var bt2: Button? = null
    private var speed = 2.5f

    private var inoreader: InoreaderAn? = null
    private var isInit = false
    private var task: FeedBundle? = null
    private var reading = false

    private val message_type = "gyf.laog.test.SHOW_CONTENT"
    private var myReceiver: ReceiveMessages? = null
    private var myReceiverIsRegistered: Boolean? = false

    private var db: AppDatabase? = null
    private var inoApi: InoApi? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)

        ed1 = findViewById<View>(R.id.textView2) as TextView
        ed2 = findViewById<View>(R.id.textView) as TextView
        ed2!!.movementMethod = ScrollingMovementMethod()

        edSpeed = findViewById<View>(R.id.ed_speed) as TextView
        edSpeed!!.text = speed.toString()

        tts = TextToSpeech(applicationContext, this)
        tts!!.setOnUtteranceProgressListener(MyUtteranceProgressListener())

        bt1 = findViewById<View>(R.id.button_stop) as Button
        bt1!!.setOnClickListener {
            if (task != null) task!!.read_or_stop()
            //             showDialog();
        }

        bt2 = findViewById<View>(R.id.button_down) as Button
        bt2!!.setOnClickListener {
            bt2!!.isEnabled = false
            ed2!!.text = "Downloading..."
            //task = FetchTask()
            task!!.download()
        }
        (findViewById<View>(R.id.button_back) as Button).setOnClickListener { if (task != null) task!!.back() }
        (findViewById<View>(R.id.button_forward) as Button).setOnClickListener { if (task != null) task!!.forward() }
        (findViewById<View>(R.id.bt_spdup) as Button).setOnClickListener {
            speed += 0.1f
            edSpeed!!.text = "%.2f".format(speed)
            tts!!.setSpeechRate(speed);
        }
        (findViewById<View>(R.id.bt_spddown) as Button).setOnClickListener {
            speed -= 0.1f
            edSpeed!!.text = "%.2f".format(speed)
            tts!!.setSpeechRate(speed);
        }

        // getExternalStoragePublicDirectory()
        // getExternalFilesDir()
        //
        val rootdir = filesDir.absolutePath
        inoreader = InoreaderAn(rootdir, false)
        ed1!!.text = rootdir

        myReceiver = ReceiveMessages()
    }

    /* Checks if external storage is available for read and write */
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    /* Checks if external storage is available to at least read */
    fun isExternalStorageReadable(): Boolean {
        return Environment.getExternalStorageState() in
                setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
    }

    fun runTask(tp: Int): Unit {
        FetchTask(tp).execute()
    }
    fun sendMessage(){
        val i = Intent(message_type)
        sendBroadcast(i)
    }

    /**
     * Of TextToSpeech.OnInitListener
     * @param status
     */
    override fun onInit(status: Int) {
        if (status != TextToSpeech.ERROR) {
            //               tts.setLanguage(Locale.UK);
            val result = tts!!.setLanguage(Locale.CHINESE)
            if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ed2!!.text = "ERROR:  LANG_NOT_SUPPORTED"
            } else if (result == TextToSpeech.LANG_MISSING_DATA) {
                val installTTSIntent = Intent()
                installTTSIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                startActivity(installTTSIntent)
            } else {
                isInit = true
            }
            task = FeedBundle(tts, bt1, this)
            FetchTask(0).execute()
        }
    }

    override fun onResume() {
        super.onResume()
        if (! myReceiverIsRegistered!! ) {
            registerReceiver(myReceiver, IntentFilter(message_type))
            myReceiverIsRegistered = true
        }
        myReceiver!!.onReceive(this, Intent(message_type));
    }

    override fun onPause() {
        super.onPause()
        if (myReceiverIsRegistered!!) {
            unregisterReceiver(myReceiver)
            myReceiverIsRegistered = false
        }
    }

    override fun onDestroy() {
        if (tts!!.isSpeaking)
            tts!!.stop()
        tts!!.shutdown()
        Log.d("", "destroy, tts shutdown!")
        if(db != null)
            db!!.close()
        super.onDestroy()
    }

    private inner class ReceiveMessages : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action!!.equals(message_type)) {
                if(task == null)
                    return;
                ed2!!.text = task!!.content
                ed2!!.scrollTo(0, 0)
                ed1!!.text = task!!.indicate()
            }
        }
    }

    private inner class MyUtteranceProgressListener : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {
//            Log.d("", "speak start!")
        }
        override fun onDone(utteranceId: String) {
//            Log.d("", "speak done!")
            if (task != null) {
                val ret = task!!.next()
                if(! ret)
                    reading = false;
            }
        }
        override fun onError(utteranceId: String) {}
    }


      /**
     * act:  0-- first load 1-- load-old  2-- download
     */
    protected inner class FetchTask(val act: Int=0) : AsyncTask<Void, Void, Boolean>() {
        private var lf: List<FeedItem>? = null
        private var content: String? = null

        override fun doInBackground(vararg voids: Void): Boolean? {
            try {
                if(act == 0) {
                    if (db == null)
                        db = Room.databaseBuilder(applicationContext,
                                AppDatabase::class.java, "inofeeds").build()
                    if (inoApi == null)
                        inoApi = InoApi(filesDir.absolutePath, db!!.feedItemDao(), false)
                    lf = inoApi!!.loadnew()
                }else if(act == 1){
                    lf = inoApi!!.loadold()
                }else {
                    inoApi!!.download()
                    lf = inoApi!!.loadnew()
                }
            } catch (ex: Exception) {
                content = ex.message
                ex.printStackTrace()
            }

            return if (lf == null) false else true
        }


        override fun onPostExecute(success: Boolean?) {
            if (success!!) {
                task!!.loaded(lf!!)
            } else {
                Toast.makeText(this@MainActivity,
                        "failed",
                        Toast.LENGTH_LONG).show()
                ed2!!.text = content
            }
            bt2!!.isEnabled = true

        }
    }

    fun toast(msg: String?): Unit {
        Toast.makeText(this@MainActivity,
                msg,
                Toast.LENGTH_LONG).show()
    }
}
