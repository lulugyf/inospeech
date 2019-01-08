package com.laog.test1;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.laog.test1.ag.CustomAdapter;
import com.laog.test1.ag.DataModel;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.services.youtube.YoutubeService;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.utils.Localization;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import gyf.test.YTDownloader;
import gyf.test.YoutubeTest;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class YTListActivity extends Activity {

    private final String TAG = "YT";

    ArrayList<DataModel> dataModels;
    ListView listView;
    private static CustomAdapter adapter;

    @BindView(R.id.ytQuery) EditText ytQuery;
//    @BindView(R.id.button_clickme) Button clickme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ag_activity_main);
        ButterKnife.bind(this);

        listView = (ListView)findViewById(R.id.list);

        dataModels= new ArrayList<>();

//        dataModels.add(new DataModel("Apple Pie", "Android 1.0", "1","September 23, 2008"));
//        dataModels.add(new DataModel("Banana Bread", "Android 1.1", "2","February 9, 2009"));


        adapter = new CustomAdapter(dataModels, getApplicationContext());

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

//                DataModel dataModel= dataModels.get(position);
//
//                Snackbar.make(view, dataModel.getName()+"\n"+dataModel.getType()+" API: "+dataModel.getVersion_number(), Snackbar.LENGTH_LONG)
//                        .setAction("No action", null).show();
            }
        });

//        clickme.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ag_menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if(id == R.id.action_rx){

        }

        return super.onOptionsItemSelected(item);
    }


    private void toast(String msg) {
        Toast.makeText(YTListActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    YoutubeTest yt = new YoutubeTest();

    @OnClick(R.id.button_clickme)
    void taskTest() {
        Observable.fromCallable(() -> {
            try {
                return yt.search(ytQuery.getText().toString());
            } catch (Exception e) {
                Log.e("Network request", "Failure", e);
            }
            return null;
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .flatMap( (list) ->{
            return Observable.fromIterable(list);
        })
        .subscribe((info) -> {
            System.out.println("----- return:"+info.getName());
            if(info instanceof StreamInfoItem) {
                StreamInfoItem i = (StreamInfoItem)info;
//                        System.out.println(i.getName() + "---Stream date:" + i.getUploadDate() + " dur:" + i.getDuration());
                dataModels.add(new DataModel(i.getName(), "Stream", i.getUploadDate(), i.getUrl()));
            }else if(info instanceof ChannelInfoItem) {
                ChannelInfoItem i = (ChannelInfoItem)info;
                dataModels.add(new DataModel(i.getName(), "Channel", i.getDescription(), i.getUrl()));
//                        System.out.println(i.getName() + "---Channel");
            }
            adapter.notifyDataSetChanged();
        });
    }

    /**
     * https://www.coderefer.com/rxandroid-tutorial-getting-started/
     * https://www.coderefer.com/rxandroid-example-tutorial/
     * https://medium.com/@kurtisnusbaum/rxandroid-basics-part-1-c0d5edcf6850
     */
//
    void rxtest1() {
        Observable mObservable;
        Observer<Integer> mObserver;
        mObservable = Observable.create(e -> {
            for(int i=1; i<=5;i++){
                e.onNext(i);
            }
            e.onComplete();
        });
        mObserver = new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                toast("onSubscribe called");
            }

            @Override
            public void onNext(Integer integer) {
                toast("onNext called: " + integer);
            }

            @Override
            public void onError(Throwable e) {
                toast("onError called");
            }

            @Override
            public void onComplete() {
                toast("onComplete called");
            }
        };
        mObservable.subscribe(mObserver);
    }
}
