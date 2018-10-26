package com.laog.test1.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {FeedItem.class}, version=1, exportSchema=false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract FeedItemDao feedItemDao();
}
