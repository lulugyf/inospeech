package com.laog.test1.db;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.laog.test1.inoreader.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

@Entity(tableName = "feeditems", indices = {@Index("published")})
public class FeedItem {
    @PrimaryKey
    @ColumnInfo(name="itemid")
    @NonNull
    private String id;

    @ColumnInfo(name="title")
    private String title;

    @ColumnInfo(name="s_published")
    private String s_published;

    @ColumnInfo(name="published")
    private long published;

    @ColumnInfo(name="href")
    private String href;

    @ColumnInfo(name="author")
    private String author;

    @ColumnInfo(name="streamid")
    private String streamid;

    @ColumnInfo(name="yearmonth")
    private String yearmonth;

    @ColumnInfo(name="filepos")
    private long filepos;  //文件开始的位置

    @Ignore
    public String content;

    @Ignore
    public String rawContent;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getS_published() {
        return s_published;
    }

    public void setS_published(String s_published) {
        this.s_published = s_published;
    }

    public long getPublished() {
        return published;
    }

    public void setPublished(long published) {
        this.published = published;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getStreamid() {
        return streamid;
    }

    public void setStreamid(String streamid) {
        this.streamid = streamid;
    }

    public String getYearmonth() {
        return yearmonth;
    }

    public void setYearmonth(String yearmonth) {
        this.yearmonth = yearmonth;
    }

    public long getFilepos() {
        return filepos;
    }

    public void setFilepos(long filepos) {
        this.filepos = filepos;
    }

    private static final SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd HH:mm:SS");
    public static FeedItem fromJSON(JSONObject j) throws JSONException {
        final FeedItem f = new FeedItem();
        f.id = j.getString("id");
        f.title = j.getString("title");
        f.published = j.getLongValue("published");
        f.s_published = sdf.format(new Date(f.published * 1000));
        f.yearmonth = f.s_published.substring(0, 7);
        f.author = j.getString("author");
        f.href = j.getJSONArray("canonical").getJSONObject(0).getString("href");
        f.streamid = j.getJSONObject("origin").getString("streamId");
        f.rawContent = j.getJSONObject("summary").getString("content");
        return f;
    }

    public void print() {
        Utils.log("title: "+title);
        Utils.log("published:"+s_published);
        Utils.log("author: "+author);
        Utils.log("href: "+href);
    }

    public void loadContent(String rootdir) throws Exception {
        String fname = rootdir + File.separator + yearmonth;
        rawContent = Utils.loadContent(fname, filepos);
        if(rawContent != null){
            content = Utils.extractTextFromHtml(rawContent);
        }else{
            content = "[load failed]";
        }
    }
}
