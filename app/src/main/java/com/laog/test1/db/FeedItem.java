package com.laog.test1.db;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.laog.test1.inoreader.Utils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
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

    /**
     * 从以yearmonth 为名称的文件中读取内容部分, 记录中保存有文件中的偏移量, 偏移位置的前8个字节是内容长度
     * @param rootdir
     * @throws Exception
     */
    public void loadContent(String rootdir) throws Exception {
        String fname = rootdir + File.separator + yearmonth;
        rawContent = Utils.loadContent(fname, filepos);
        if(rawContent != null){
            content = Utils.extractTextFromHtml(rawContent);
        }else{
            content = "[load failed]";
        }
    }

    /**
     * 保存内容部分保存到 以yearmonth为名称的文件中
     * @param rootdir
     * @throws Exception
     */
    public void saveContent(String rootdir) throws Exception {
        final String fpath = rootdir + File.separator + yearmonth;
        filepos = Utils.saveContent(fpath, rawContent);
    }

    /**
     * 把记录保存到文件中, 便于备份, sqlite 中存记录过多的话会影响性能
     * @param outstream
     * @throws Exception
     */
    public void saveRecord(OutputStream outstream) throws Exception {
        JSONObject j = new JSONObject();
        j.put("id", id);
        j.put("title", title);
        j.put("published", published);
        j.put("s_published", s_published);
        j.put("yearmonth", yearmonth);
        j.put("author", author);
        j.put("href", href);
        j.put("streamid", streamid);
        j.put("filepos", filepos);
        byte[] bytes = j.toJSONString().getBytes("UTF-8");
        outstream.write(String.format("% 8d", bytes.length).getBytes());
        outstream.write(bytes);
    }
    public boolean readRecord(InputStream instream) throws Exception{
        byte[] bytes = new byte[8];
        int r = instream.read(bytes);
        if(r != bytes.length) {
            Log.e("", "file size not enough");
            return false;
        }
        int len = Integer.parseInt(new String(bytes).trim());
        bytes = new byte[len];
        r = instream.read(bytes);
        if(r != bytes.length){
            Log.e("", "readRecord failed, record bytes not enough");
            return false;
        }
        JSONObject j = JSON.parseObject(new String(bytes, "UTF-8"));
        id = j.getString("id");
        title = j.getString("title");
        published = j.getLongValue("published");
        s_published = j.getString("s_published");
        yearmonth = j.getString("yearmonth");
        author = j.getString("author");
        href = j.getString("href");
        streamid = j.getString("streamid");
        filepos = j.getLong("filepos");
        return true;
    }
}
