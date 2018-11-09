package com.laog.test1;

import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;

import com.laog.test1.db.FeedItem;

import java.util.LinkedList;
import java.util.List;

public class FeedBundle {
    private String content;
    private List<FeedItem> lf;
    private int idx = 0;
    private LinkedList<String> ll = new LinkedList<String>();
    private int pageidx = 0;
    private boolean reading;
    private Button bt1;
    private MainActivity act;
    private volatile FeedItem cur_fi;

    private TextToSpeech tts;
    public FeedBundle(TextToSpeech tts, Button bt,  MainActivity act){
        this.act = act;
        this.tts = tts;
        bt1 = bt;
    }

    public String getContent(){ return content; }
    public void back() {
        if (lf == null) return;
        if (idx <= 1) return;
                ll.clear();
        idx -= 2;
        if (tts.isSpeaking())
            tts.stop();
        next();
    }
    public void forward() {
        if (lf == null) return;
                //if (idx >= lf.size) return
        ll.clear();
        if (tts.isSpeaking())
            tts.stop();
        next();
    }
    public FeedItem curItem() { return cur_fi; }

    public String indicate() {
        if(lf == null)
            return "";
        return idx + " / " + lf.size() + "  " + pageidx;
    }
    public void read_or_stop() {
        if (reading) {
            if (tts.isSpeaking())
                tts.stop();
            reading = false;
            bt1.setText(R.string.str_start);
        } else {
            reading = true;
            bt1.setText(R.string.str_stop);
            next();
        }
    }
    /**
     * 一段读完后自动转为下一段
     */
    public boolean next() {
        if (ll.size() > 0 && reading ) {
            tts.speak(ll.removeFirst(), TextToSpeech.QUEUE_FLUSH, null, "p");
        } else {
            if (lf == null || idx < 0)
                return false;
            if( idx >= lf.size() ) {
                act.runTask(1);
                pageidx += 1;
                return true;
            }
            FeedItem art = lf.get(idx);
            idx += 1;
            final String fav = "1".equals(art.getFav()) ? " [FAV]" : "";
            content = art.getTitle() + fav+"\n\n" + art.getAuthor() + " " + art.getS_published() +"\n\n" + art.content;
            act.sendMessage();  //发送广播消息, 更新gui
            Log.d("", "article: "+art.getS_published() + " " + art.getTitle());
            ll.clear();
            ll.add("标题");
            ll.add(art.getTitle());
            ll.add(art.getAuthor());
            for (String s:  art.content.split("。") )
                ll.add(s);
            if (reading) {
                tts.speak(ll.removeFirst(), TextToSpeech.QUEUE_FLUSH, null, "p");
            }
            this.cur_fi = art;
        }
        return true;
    }
    public void download() {
        act.runTask(2);
        pageidx = 0;
    }

    public void loaded(List<FeedItem> v) {
        idx = 0;
        lf = v;
        next();
    }
}
