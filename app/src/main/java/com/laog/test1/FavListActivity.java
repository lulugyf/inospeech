package com.laog.test1;

import android.arch.persistence.room.Room;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.laog.test1.adapter.ArticleArrayAdapter;
import com.laog.test1.db.AppDatabase;
import com.laog.test1.db.FavDBHelper;
import com.laog.test1.db.FeedItem;
import com.laog.test1.db.FeedItemDao;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

// http://www.vogella.com/tutorials/AndroidListView/article.html
// https://www.mkyong.com/android/android-listview-example/

public class FavListActivity extends Activity {
    private ListView lvfav;
    private List<FeedItem> content;
    private ArticleArrayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fav_list);

        lvfav = (ListView)findViewById(R.id.lvFav);

//        int item = 10;
        content = new ArrayList<>();
//        for(int i=0; i<item; i++ ) {
//            content[i] = new FeedItem();
//            content[i].setTitle("title "+i);
//            content[i].setS_published("published: "+i);
//        }
        adapter = new ArticleArrayAdapter(this, content);
        lvfav.setAdapter(adapter);

        lvfav.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedValue = content.get(i).getTitle();
                Toast.makeText(FavListActivity.this, selectedValue, Toast.LENGTH_SHORT).show();
            }
        });
//        adapter.notifyDataSetChanged();
        loadList();
    }

    private volatile AppDatabase db;
    private void loadList() {
        Observable.fromCallable(() ->{
            db = Room.databaseBuilder(getApplicationContext(),
                    AppDatabase.class, "inofeeds")
                    .addMigrations(FavDBHelper.MIGRATION_1_2)
                    .build();
            FeedItemDao dao = db.feedItemDao();
//            long maxtm = dao.findMaxTime();
//            return dao.getPage(maxtm - 24 * 3600, 20);
            return dao.listFav(0, 100);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap((list) ->{
                    content.clear();
                    return Observable.fromIterable(list);
                        }
                ).subscribe((fi) -> {
                    content.add(fi);
                    adapter.notifyDataSetChanged();
        });

    }
}
