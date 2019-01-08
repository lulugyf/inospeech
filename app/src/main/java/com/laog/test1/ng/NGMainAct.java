package com.laog.test1.ng;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.os.Bundle;
import android.app.FragmentManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.laog.test1.R;
import com.laog.test1.db.AppDatabase;
import com.laog.test1.db.FavDBHelper;
import com.laog.test1.db.FeedItem;
import com.laog.test1.db.FeedItemDao;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class NGMainAct extends Activity {

    private TextView txt_title;
    private FrameLayout fl_content;
    private Context mContext;
    private FragmentManager fManager = null;
    private long exitTime = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(1); //no-titlebar
        setContentView(R.layout.ng_main);
        mContext = NGMainAct.this;
        fManager = getFragmentManager();
        bindViews();

        loadList();
    }

    private void bindViews() {
        txt_title = (TextView) findViewById(R.id.txt_title);
        fl_content = (FrameLayout) findViewById(R.id.fl_content);
    }


    //点击回退键的处理：判断Fragment栈中是否有Fragment
    //没，双击退出程序，否则像是Toast提示
    //有，popbackstack弹出栈
    @Override
    public void onBackPressed() {
        if (fManager.getBackStackEntryCount() == 0) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toast.makeText(getApplicationContext(), "再按一次退出程序",
                        Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                super.onBackPressed();
            }
        } else {
            fManager.popBackStack();
            txt_title.setText("新闻列表");
        }
    }

    private void loadFragment(List<FeedItem> datas) {
        String rootDir = getFilesDir().getAbsolutePath().toString();
        String tmpDir = rootDir + File.separator + "tmp" + File.separator;
        NewListFragment nlFragment = new NewListFragment(fManager, tmpDir, datas);
        FragmentTransaction ft = fManager.beginTransaction();
        ft.replace(R.id.fl_content, nlFragment);
        ft.commit();
    }

    private void loadList() {
        String rootDir = getFilesDir().getAbsolutePath().toString();

        Observable.fromCallable(() ->{
            AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                    AppDatabase.class, "inofeeds")
                    .addMigrations(FavDBHelper.MIGRATION_1_2)
                    .build();
            FeedItemDao dao = db.feedItemDao();
            long maxtm = dao.findMaxTime();
            return dao.listFav(0, 100);
//            return dao.listFav(0, 100);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((list) -> {
//                    content.addAll(list);
//                    loadContent(0);
                    loadFragment(list);
//            adapter.notifyDataSetChanged();
                });

    }
}