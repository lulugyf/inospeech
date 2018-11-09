package com.laog.test1.db;

//   http://www.codebind.com/android-tutorials-and-examples/android-sqlite-tutorial-example/

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

public class FavDBHelper extends SQLiteOpenHelper {

    private static FavDBHelper sInstance;

    private static final String DATABASE_NAME = "fav.db";
    private static String DB_PATH = Environment.getExternalStorageDirectory().toString()+"/data/inospeech/fav.db";
    private static final String DATABASE_TABLE = "tableName";
    private static final int DATABASE_VERSION = 2;

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
        database.execSQL("create table feeditems(itemid text primary key, title text not null, s_published text, published long, href text, yearmonth text, filepos long, fav INTEGER);");
        database.execSQL("create index idx_1 on feeditems(published);");
        database.execSQL("create index idx_2 on feeditems(fav);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        /*Log.w(FavDBHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        if(oldVersion == 1) {
            database.execSQL("alter table feeditems add column fav int;");
            database.execSQL("create index idx_2 on feeditems(fav);");
        }else {
            database.execSQL("DROP TABLE IF EXISTS feeditems");
            onCreate(database);
        } */
    }

    /*
    11-06 13:28:01.116 1356-1383/com.laog.test1 W/System.err: java.lang.IllegalStateException: Migration didn't properly handle feeditems(com.laog.test1.db.FeedItem).
     Expected:
TableInfo{name='feeditems', columns={s_published=Column{name='s_published', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0}, itemid=Column{name='itemid', type='TEXT', affinity='2', notNull=true, primaryKeyPosition=1}, filepos=Column{name='filepos', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}, streamid=Column{name='streamid', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0}, author=Column{name='author', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0}, fav=Column{name='fav', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}, published=Column{name='published', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}, href=Column{name='href', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0}, yearmonth=Column{name='yearmonth', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0}, title=Column{name='title', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0}}, foreignKeys=[], indices=[Index{name='index_feeditems_published', unique=false, columns=[published]}, Index{name='index_feeditems_fav', unique=false, columns=[fav]}]}
     Found:
TableInfo{name='feeditems', columns={s_published=Column{name='s_published', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0}, itemid=Column{name='itemid', type='TEXT', affinity='2', notNull=true, primaryKeyPosition=1}, filepos=Column{name='filepos', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}, streamid=Column{name='streamid', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0}, author=Column{name='author', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0}, fav=Column{name='fav', type='INTEGER', affinity='3', notNull=false, primaryKeyPosition=0}, published=Column{name='published', type='INTEGER', affinity='3', notNull=true, primaryKeyPosition=0}, href=Column{name='href', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0}, yearmonth=Column{name='yearmonth', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0}, title=Column{name='title', type='TEXT', affinity='2', notNull=false, primaryKeyPosition=0}}, foreignKeys=[], indices=[Index{name='index_feeditems_published', unique=false, columns=[published]}, Index{name='index_feeditems_fav', unique=false, columns=[fav]}]}
        at com.laog.test1.db.AppDatabase_Impl$1.validateMigration(AppDatabase_Impl.java:84)
        at android.arch.persistence.room.RoomOpenHelper.onUpgrade(RoomOpenHelper.java:87)
     */
    static public final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("alter table feeditems add column fav text;");
            database.execSQL("create index index_feeditems_fav on feeditems(fav);");
        }
    };
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