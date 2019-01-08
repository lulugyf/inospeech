package com.laog.ytdown.frag;

import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.laog.ytdown.R;
import com.laog.ytdown.dt.CustomAdapter;
import com.laog.ytdown.service.DownService;

import org.schabi.newpipe.extractor.InfoItem;

import java.io.File;
import java.util.ArrayList;

import gyf.test.YoutubeTest;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class FragChannel extends Fragment{

    private final String TAG = "YT";

    YoutubeTest yt;
    ArrayList<InfoItem> dataModels;
    private static CustomAdapter adapter;

    ListView listView;

    private String name;
    private String url;

    public void setYt(YoutubeTest y) { yt = y; }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_channel, container, false);
        Bundle bd = getArguments();
        name = bd.getString("name");
        url = bd.getString("url");

        bindViews(view);
        loadList();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(name);
    }

    private void bindViews(View v) {
        listView = v.findViewById(R.id.list);
        SwipeRefreshLayout pullToRefresh = v.findViewById(R.id.pullToRefresh);

        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
                pullToRefresh.setRefreshing(false);
            }
        });
    }

    private InfoItem item;
    private void loadList() {
        dataModels = new ArrayList<>();
        adapter = new CustomAdapter(dataModels, getContext().getApplicationContext());

        dataModels.addAll(yt.readChannelItems(url));

        listView.setAdapter(adapter);
        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) ->{
            item = dataModels.get(position);
            Snackbar.make(view, item.getName()+"\n"+item.getUrl(), Snackbar.LENGTH_LONG)
                    .setAction("Download", new View.OnClickListener(){
                        public void onClick(View v) {
                            beginDown();
                        }
                    }).show();
        });

        if(dataModels.size() == 0)
            refresh();
    }

    private void beginDown() {
        Context mContext = getContext();
        String path = mContext.getExternalFilesDir("0down").getAbsolutePath();
        try{
            File f = new File(path);
            f.mkdirs();
            final Intent intent = new Intent(mContext, DownService.class);
            intent.setAction("com.laog.service.DownService");
            intent.putExtra("path", path);
            intent.putExtra("url", item.getUrl());
            intent.putExtra("name", item.getName());
            intent.putExtra("isAudio", true);
            intent.putExtra("fmt", "m4a");
            intent.putExtra("solution", "");
            mContext.startService(intent);
        }catch(Exception e){
            Log.e(TAG, "startService failed", e);
        }
    }

    private void refresh() {
        Observable.fromCallable(() -> {
            try {
                return yt.channel(url);
            } catch (Exception e) {
                Log.e("Network request", "Failure", e);
            }
            return null;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( (list) ->{
                    yt.writeChannelItems(list, url);
                    dataModels.clear();
                    dataModels.addAll(list);
                    adapter.notifyDataSetChanged();
                });
    }
}
