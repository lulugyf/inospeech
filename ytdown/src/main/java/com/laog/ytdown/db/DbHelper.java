package com.laog.ytdown.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
    public DbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, context.getExternalFilesDir("0down").getAbsolutePath()+"/my.db", null, 1);
    }
    @Override
    //数据库第一次创建时被调用
    public void onCreate(SQLiteDatabase db) {

        //订阅的channel,
        db.execSQL("CREATE TABLE channels(chid INTEGER PRIMARY KEY AUTOINCREMENT, name text, url text, json text)");

        // channel 查询到的媒体列表
        db.execSQL("CREATE TABLE chstreams(chid INTEGER, sid text, name text, json text)");


        // 查询历史
        db.execSQL("CREATE TABLE queries(qid INTEGER PRIMARY KEY AUTOINCREMENT, qstr text, qtime text)");

    }
    //软件版本号发生改变时调用
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("ALTER TABLE person ADD phone VARCHAR(12)");
    }
}