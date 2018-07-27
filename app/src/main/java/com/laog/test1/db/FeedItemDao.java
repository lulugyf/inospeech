package com.laog.test1.db;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface FeedItemDao {

    @Query("select * from feeditems where published>:mintime order by published desc limit :pagesize")
    public List<FeedItem> getPage(long mintime, int pagesize) ;

    @Query("select * from feeditems where published>=:mintime and published<:maxtime order by published desc limit :pagesize")
    public List<FeedItem> getPage(long mintime, long maxtime, int pagesize) ;

    @Insert
    public long[] insertAll(FeedItem ... items);

    @Query("select max(published) from feeditems")
    public long findMaxTime();

    @Query("select count(*) from feeditems")
    public int getCount();
}
