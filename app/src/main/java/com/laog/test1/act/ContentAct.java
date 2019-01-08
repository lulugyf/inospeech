package com.laog.test1.act;

import android.app.Activity;
import android.arch.persistence.room.Room;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.laog.test1.R;
import com.laog.test1.db.AppDatabase;
import com.laog.test1.db.FavDBHelper;
import com.laog.test1.db.FeedItem;
import com.laog.test1.db.FeedItemDao;

import java.io.File;
import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ContentAct extends Activity implements View.OnClickListener {

    private Button btn_back;
    private TextView txt_title;
    private Button btn_top;
    private Button btn_prev;
    private Button btn_next;
    private Button btn_refresh;
    private MyWebView wView;
    private long exitTime = 0;

    private Button btn_icon;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content);
        bindViews();
    }

    private void bindViews() {
        btn_back = (Button) findViewById(R.id.btn_back);
        txt_title = (TextView) findViewById(R.id.txt_title);
        btn_refresh = (Button) findViewById(R.id.btn_refresh);
        wView = (MyWebView) findViewById(R.id.wView);

        btn_prev = (Button) findViewById(R.id.btn_prev);
        btn_next = (Button) findViewById(R.id.btn_next);
        btn_prev.setOnClickListener(this);
        btn_next.setOnClickListener(this);

        btn_back.setOnClickListener(this);
        btn_refresh.setOnClickListener(this);

        btn_icon = (Button) findViewById(R.id.btn_icon);

//        wView.loadUrl("http://www.baidu.com");
        wView.setWebChromeClient(new WebChromeClient() {
            //这里设置获取到的网站title
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                //txt_title.setText(title);
            }
        });

        //比如这里做一个简单的判断，当页面发生滚动，显示那个Button
        wView.setOnScrollChangedCallback(new MyWebView.OnScrollChangedCallback() {
            @Override
            public void onScroll(int dx, int dy) {
//                if (dy > 0) {
//                    btn_icon.setVisibility(View.VISIBLE);
//                } else {
//                    btn_icon.setVisibility(View.GONE);
//                }
            }
        });

        WebSettings settings = wView.getSettings();
//        settings.setUseWideViewPort(true);//设定支持viewport
//        settings.setLoadWithOverviewMode(true);   //自适应屏幕
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);//设定支持缩放


        wView.setWebViewClient(new WebViewClient() {
            //在webview里打开新链接
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        loadList();
    }

    private ArrayList<FeedItem> content = new ArrayList<>();
    private int idx;
    private String tmpDir;
    private FeedItem fi;
    private void loadList() {
        String rootDir = getFilesDir().getAbsolutePath().toString();
        tmpDir = rootDir + File.separator + "tmp" + File.separator;
        Observable.fromCallable(() ->{
            AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                    AppDatabase.class, "inofeeds")
                    .addMigrations(FavDBHelper.MIGRATION_1_2)
                    .build();
            FeedItemDao dao = db.feedItemDao();
            long maxtm = dao.findMaxTime();
            return dao.getPage(maxtm - 24 * 3600, 100);
//            return dao.listFav(0, 100);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((list) -> {
            content.addAll(list);
            loadContent(0);
//            adapter.notifyDataSetChanged();
        });

    }
    private void loadContent(int n) {
        if(n < 0 || n >= content.size())
            return;
        idx = n;
        fi = content.get(idx);
        if(fi.rawContent == null) {
            try {
                fi.loadContent(tmpDir);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(fi.rawContent != null) {
            wView.loadData(fi.rawContent, "text/html; charset=utf-8", "UTF-8");
            txt_title.setText(fi.getTitle());
            btn_refresh.setText(String.format("%d/%d", idx, content.size()));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_back:
                finish();          //关闭当前Activity
                break;
            case R.id.btn_refresh:
                wView.reload();    //刷新当前页面
                break;
//            case R.id.btn_top:
//                wView.setScrollY(0);   //滚动到顶部
//                break;
            case R.id.btn_next:
                loadContent(idx+1);
                break;
            case R.id.btn_prev:
                loadContent(idx-1);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (wView.canGoBack()) {
            wView.goBack();
        } else {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toast.makeText(getApplicationContext(), "再按一次退出程序",
                        Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
            }

        }
    }
}
