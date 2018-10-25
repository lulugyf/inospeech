package com.laog.test1.db;

//   http://www.codebind.com/android-tutorials-and-examples/android-sqlite-tutorial-example/

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

public class FavDBHelper extends SQLiteOpenHelper {

    private static FavDBHelper sInstance;

    private static final String DATABASE_NAME = "fav.db";
    private static String DB_PATH = Environment.getExternalStorageDirectory().toString()+"/data/inospeech/fav.db";
    private static final String DATABASE_TABLE = "tableName";
    private static final int DATABASE_VERSION = 1;

    public static synchronized FavDBHelper getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new FavDBHelper(context.getApplicationContext());
        }
        return sInstance;
    }
    /**
     * Constructor should be private to prevent direct instantiation.
     * make call to static method "getInstance()" instead.
     */
    private FavDBHelper(Context context) {
//        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        super(context, DB_PATH, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("create table feeditems(itemid text primary key, title text not null, s_published text, published long, href text, yearmonth text, filepos long);");
        database.execSQL("create index idx_1 on feeditems(published);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(FavDBHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS feeditems");
        onCreate(database);
    }
}

class MyDB{

    private FavDBHelper dbHelper;

    private SQLiteDatabase database;

    public final static String EMP_TABLE="MyEmployees"; // name of table

    public final static String EMP_ID="_id"; // id value for employee
    public final static String EMP_NAME="name";  // name of employee

    /**
     *
     * @param context
     */
    public MyDB(Context context){
        dbHelper = FavDBHelper.getInstance(context);
        database = dbHelper.getWritableDatabase();
    }


    public long createRecords(String id, String name){
        ContentValues values = new ContentValues();
        values.put(EMP_ID, id);
        values.put(EMP_NAME, name);
        return database.insert(EMP_TABLE, null, values);
    }

    public Cursor selectRecords() {
        String[] cols = new String[] {EMP_ID, EMP_NAME};
        Cursor mCursor = database.query(true, EMP_TABLE,cols,null
                , null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }
}