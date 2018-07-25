package com.laog.test1

import android.app.Activity
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

import java.util.LinkedList
import java.util.Locale

import android.widget.TextView
import android.widget.Toast

import com.laog.test1.inoreader.Article
import com.laog.test1.inoreader.InoreaderAn
import com.laog.test1.inoreader.Utils

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
    private var task: FetchTask? = null
    private var reading = false

    private val message_type = "gyf.laog.test.SHOW_CONTENT"
    private var myReceiver: ReceiveMessages? = null
    private var myReceiverIsRegistered: Boolean? = false

    override fun onDestroy() {
        if (tts!!.isSpeaking)
            tts!!.stop()
        tts!!.shutdown()
        Log.d("", "destroy, tts shutdown!")
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            task = FetchTask()
            task!!.execute(null as Void?)
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
        task = FetchTask()
        //task.initLoad();
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

            //tts.setSpeechRate(2.0f);
            if (task != null)
                task!!.initLoad()
        }
    }

    override fun onResume() {
        super.onResume()
        if (! myReceiverIsRegistered!! ) {
            registerReceiver(myReceiver, IntentFilter(message_type))
            myReceiverIsRegistered = true
        }
    }

    override fun onPause() {
        super.onPause()
        if (myReceiverIsRegistered!!) {
            unregisterReceiver(myReceiver)
            myReceiverIsRegistered = false
        }
    }

    private inner class ReceiveMessages : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action!!.equals(message_type)) {
                ed2!!.text = task!!.content
                ed2!!.scrollTo(0, 0)
                ed1!!.text = task!!.idx.toString() + " / " + task!!.la!!.size
            }
        }
    }

    private inner class MyUtteranceProgressListener : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {
//            Log.d("", "speak start!")
        }
        override fun onDone(utteranceId: String) {
//            Log.d("", "speak done!")
            if (task != null)
                task!!.next()
        }
        override fun onError(utteranceId: String) {}
    }


    private inner class FetchTask : AsyncTask<Void, Void, Boolean>() {
        var content: String? = null
        var la: List<Article>? = null
        var idx: Int = 0
        private val ll = LinkedList<String>()

        internal fun initLoad() {
            try {
                la = inoreader!!.initLoadFile()
                if (la == null)
                    return
                onPostExecute(true)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        fun back() {
            if (la == null) return
            if (idx <= 1) return
            ll.clear()
            idx -= 2
            if (tts!!.isSpeaking)
                tts!!.stop()
            next()
        }

        fun forward() {
            if (la == null) return
            if (idx >= la!!.size) return
            ll.clear()
            if (tts!!.isSpeaking)
                tts!!.stop()
            next()
        }

        fun read_or_stop() {
            if (reading) {
                if (tts!!.isSpeaking)
                    tts!!.stop()
                reading = false
                bt1!!.setText(R.string.str_start)
            } else {
                reading = true
                bt1!!.setText(R.string.str_stop)
                next()
            }
        }

        /**
         * 一段读完后自动转为下一段
         */
        operator fun next() {
            if (ll.size > 0 && reading ) {
                tts!!.speak(ll.removeFirst(), TextToSpeech.QUEUE_FLUSH, null, "p")
            } else {
                if (la == null || idx < 0 || idx >= la!!.size)
                    return
                val art = la!![idx]
                idx += 1

                speed = java.lang.Float.parseFloat(edSpeed!!.text.toString())
                tts!!.setSpeechRate(speed)
//                Utils.log("=========speechRate= $speed")

                content = art.title + "\n\n" + art.author + "\n\n" + art.content
                val i = Intent(message_type)
                ed2!!.context.sendBroadcast(i)
                Log.d("", "article: "+art.id + " " + art.title)
                ll.clear();
                ll.add("标题")
                ll.add(art.title)
                ll.add(art.author)
                for (s in art.content.split("。".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())
                    ll.add(s)
                if (reading) {
                    tts!!.speak(ll.removeFirst(), TextToSpeech.QUEUE_FLUSH, null, "p")
                }
            }
        }

        override fun doInBackground(vararg voids: Void): Boolean? {
            try {
                inoreader!!.start()
                la = inoreader!!.fetch()
            } catch (ex: Exception) {
                content = ex.message
            }

            return if (la == null) false else true
        }

        override fun onPostExecute(success: Boolean?) {
            if (success!!) {
                idx = 0
                next()
            } else {
                Toast.makeText(this@MainActivity,
                        "failed",
                        Toast.LENGTH_LONG).show()
                ed2!!.text = content
            }
            bt2!!.isEnabled = true
        }
    }
}
