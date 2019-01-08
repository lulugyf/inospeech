package com.laog.ytdown.frag;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.laog.ytdown.R;
import com.laog.ytdown.dt.CustomAdapter;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;

import java.util.ArrayList;

import gyf.test.YoutubeTest;

public class FragChannels extends Fragment{

    private final String TAG = "YT";

    YoutubeTest yt;
    ArrayList<InfoItem> dataModels;
    private Context mContext;
    private FragmentManager fManager = null;

    private static CustomAdapter adapter;

    ListView listView;

    public void setYt(YoutubeTest y) { yt = y; }
    public void setfManager(FragmentManager f) { fManager = f; }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_channels, container, false);
        mContext = getContext();

        bindViews(view);
        loadList();
        return view;
    }

    private void bindViews(View v) {
        listView = (ListView)v.findViewById(R.id.list);
    }

    private String rootDir;
    private void loadList() {
        dataModels = new ArrayList<>();
        adapter = new CustomAdapter(dataModels, getContext().getApplicationContext());

        dataModels.addAll(yt.readChannelList());

        listView.setAdapter(adapter);
        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) ->{
            cur_si = (ChannelInfoItem)dataModels.get(position);
//            Snackbar.make(view, cur_si.getName()+"\n"+cur_si.getDescription(), Snackbar.LENGTH_LONG)
//                    .setAction("No action", null).show();
            openChannel();
        });
    }
    private ChannelInfoItem cur_si;

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle("Channels");
    }

    private void openChannel() {
        Bundle bd = new Bundle();
        bd.putString("url", cur_si.getUrl());
        bd.putString("name", cur_si.getName());
        FragChannel frag = new FragChannel();
        frag.setArguments(bd);
        frag.setYt(yt);
        FragmentTransaction ft = fManager.beginTransaction();
        ft.replace(R.id.fl_content, frag);
        ft.addToBackStack("channels");
        ft.commit();
    }
}
