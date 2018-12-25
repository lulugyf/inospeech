package com.laog.test1;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.laog.test1.adapter.ArticleArrayAdapter;
import com.laog.test1.db.FeedItem;

// http://www.vogella.com/tutorials/AndroidListView/article.html
// https://www.mkyong.com/android/android-listview-example/

public class FavListActivity extends Activity {
    private ListView lvfav;
    private FeedItem[] content;
    private ArticleArrayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fav_list);

        lvfav = (ListView)findViewById(R.id.lvFav);

        int item = 10;
        content = new FeedItem[item];
        for(int i=0; i<item; i++ ) {
            content[i] = new FeedItem();
            content[i].setTitle("title "+i);
            content[i].setS_published("published: "+i);
        }
        adapter = new ArticleArrayAdapter(this, content);
        lvfav.setAdapter(adapter);

        lvfav.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedValue = content[i].getTitle();
                Toast.makeText(FavListActivity.this, selectedValue, Toast.LENGTH_SHORT).show();
            }
        });
        adapter.notifyDataSetChanged();
    }
}
