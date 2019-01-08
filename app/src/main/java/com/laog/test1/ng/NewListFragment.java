package com.laog.test1.ng;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.arch.persistence.room.Room;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

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

/**
 * Created by Jay on 2015/9/6 0006.
 */
public class NewListFragment extends Fragment implements AdapterView.OnItemClickListener {
    private FragmentManager fManager;
    private List<FeedItem> datas;
    private ListView list_news;

    private String tmpDir;

    public NewListFragment() {}

    @SuppressLint("ValidFragment")
    public NewListFragment(FragmentManager fManager, String tmpDir, List<FeedItem> datas) {
        this.tmpDir = tmpDir;
        this.fManager = fManager;
        this.datas = datas;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ng_newslist, container, false);
        list_news = (ListView) view.findViewById(R.id.list_news);
        MyAdapter myAdapter = new MyAdapter(datas, getActivity());
        list_news.setAdapter(myAdapter);
        list_news.setOnItemClickListener(this);
        return view;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FragmentTransaction fTransaction = fManager.beginTransaction();
        NewContentFragment ncFragment = new NewContentFragment();

        FeedItem fi = datas.get(position);
        if(fi.rawContent == null) {
            try {
                fi.loadContent(tmpDir);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Bundle bd = new Bundle();
        bd.putString("rawContent", fi.rawContent);
        bd.putString("yearMonth", fi.getYearmonth());
        bd.putString("articleid", fi.getId());
        ncFragment.setArguments(bd);
        //获取Activity的控件
        TextView txt_title = (TextView) getActivity().findViewById(R.id.txt_title);
        txt_title.setText(fi.getTitle());
        //加上Fragment替换动画
//        fTransaction.setCustomAnimations(R.anim.fragment_slide_left_enter, R.anim.fragment_slide_left_exit);
        fTransaction.replace(R.id.fl_content, ncFragment);
        //调用addToBackStack将Fragment添加到栈中
        fTransaction.addToBackStack(null);
        fTransaction.commit();
    }
}