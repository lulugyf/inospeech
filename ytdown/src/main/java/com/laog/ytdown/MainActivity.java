package com.laog.ytdown;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.laog.ytdown.frag.FragChannels;
import com.laog.ytdown.frag.FragSearch;
import com.laog.ytdown.service.DownService;

import java.util.concurrent.TimeUnit;

import gyf.test.YoutubeTest;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final String TAG = "YT";

    YoutubeTest yt = null;

    DownService.DownBinder binder;

    private ServiceConnection serviceConnection;

    private FragmentManager fManager = null;

    private Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mContext = this.getApplicationContext();

        fManager = getSupportFragmentManager();

        yt = new YoutubeTest();
        String rootDir = mContext.getExternalFilesDir("conf").getAbsolutePath();
        yt.setRootDir(rootDir); //设置数据目录

        navChg(R.id.nav_channels);
    }

    private void init() {
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                binder = (DownService.DownBinder)iBinder;
            }
            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                binder = null;
            }
        };

        timerTest();
    }

    private void timerTest() {
//        Observable.timer(2, TimeUnit.SECONDS)  //延迟2秒 执行(单次)
        Observable.interval(2, TimeUnit.SECONDS) //每个2秒执行, 重复
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                               public void onSubscribe(Disposable d) {
                               }
                               public void onNext(Long aLong) {
                                   if(binder != null){
//                                        String st = String.format("%s %.2f %% done: %.2f KB", binder.getVid(), binder.getDone()*100.0/binder.getTotal(), binder.getDone()/1024.0);
//                                        tvStatus.setText(st);
                                    }
                               }
                               public void onError(Throwable e) { }
                               public void onComplete(){}
                           }
                );
    }

    @Override
    protected void onDestroy() {
//        RxTimerUtil.cancel();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        return navChg(id);
    }
    private boolean navChg(int id){
        if (id == R.id.nav_search) {
            setTitle("Search");
            FragSearch frag = new FragSearch();
            frag.setYt(yt);
            FragmentTransaction ft = fManager.beginTransaction();
            ft.replace(R.id.fl_content, frag);
            ft.commit();
        } else if (id == R.id.nav_channels) {
            setTitle("Channels");
            FragChannels frag = new FragChannels();
            frag.setYt(yt);
            frag.setfManager(fManager);
            FragmentTransaction ft = fManager.beginTransaction();
            ft.replace(R.id.fl_content, frag);
            ft.commit();
        } else if (id == R.id.nav_slideshow) {
        } else if (id == R.id.nav_manage) {
        } else if (id == R.id.nav_share) {
        } else if (id == R.id.nav_send) {
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


}
