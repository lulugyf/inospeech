package com.laog.test1.db;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
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

    @Query("select * from feeditems order by published desc")
    public List<FeedItem> getAll();

    @Query("select min(published) as mnp, max(published) as mxp from feeditems")
    public List<MN> getmn();

    @Insert
    public long insert(FeedItem item);

    @Query("select max(published) from feeditems")
    public long findMaxTime();

    @Update
    public int updateItem(FeedItem v);

    @Query("select count(*) from feeditems")
    public int getCount();

    @Query("delete from feeditems where published<:maxtime")
    public int deleteByPubTime(long maxtime);

    @Query("select substr(s_published, 1, 10) as feeday, count(*) as ct from feeditems group by substr(s_published, 1, 10)")
    public List<State> state();

    @Query("select * from feeditems where fav=1 order by published desc limit :limit_start, :pagesize")
    public List<FeedItem> listFav(int limit_start, int pagesize);
}
