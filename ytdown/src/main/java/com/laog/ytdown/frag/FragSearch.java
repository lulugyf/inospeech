package com.laog.ytdown.frag;

import android.Manifest;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.laog.ytdown.R;
import com.laog.ytdown.dt.CustomAdapter;
import com.laog.ytdown.service.DownService;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;

import java.io.File;
import java.util.ArrayList;

import gyf.test.YoutubeTest;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class FragSearch extends Fragment implements View.OnClickListener{

    private final String TAG = "YT";

    YoutubeTest yt;
    ArrayList<InfoItem> dataModels;
    private Context mContext;

    private static CustomAdapter adapter;

    EditText ytQuery;
    ListView listView;
    TextView tvStatus;

    public void setYt(YoutubeTest y) { yt = y; }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_search, container, false);
        mContext = getContext();

        bindViews(view);

        loadList();

        return view;
    }

    private void bindViews(View v) {
        ytQuery = (EditText)v.findViewById(R.id.ytQuery);
        listView = (ListView)v.findViewById(R.id.list);
        tvStatus = (TextView)v.findViewById(R.id.tvStatus);

        v.findViewById(R.id.button_query).setOnClickListener(this);
    }

    private String rootDir;
    private void loadList() {
        dataModels = new ArrayList<>();
        adapter = new CustomAdapter(dataModels, getContext().getApplicationContext());

        String searchStr = yt.readSearchStr();
        if(searchStr != null){
            ytQuery.setText(searchStr);
            dataModels.addAll(yt.readSearchItems());
        }

        listView.setAdapter(adapter);
        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) ->{
            cur_si = dataModels.get(position);
            showDetail(view);
//                Snackbar.make(view, dataModel.getName()+"\n"+dataModel.getType()+" API: "+dataModel.getVersion_number(), Snackbar.LENGTH_LONG)
//                        .setAction("No action", null).show();
        });
    }
    private InfoItem cur_si;

    void showDetail(View v) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_popup, null, false);
        //1.构造一个PopupWindow，参数依次是加载的View，宽高
        final PopupWindow popWindow = new PopupWindow(view,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        popWindow.setAnimationStyle(R.anim.anim_pop);  //设置加载动画
        popWindow.setTouchable(true);
        popWindow.setTouchInterceptor((View v1, MotionEvent event) -> {
            return false;        // 这里如果返回true的话，touch事件将被拦截
            // 拦截后 PopupWindow的onTouchEvent不被调用，这样点击外部区域无法dismiss
        });
        popWindow.setBackgroundDrawable(new ColorDrawable(0xff666666));    //要为popWindow设置一个背景才有效
        ((TextView) view.findViewById(R.id.tvName)).setText(cur_si.getName());
        popWindow.showAsDropDown(v, 50, -20);

        view.findViewById(R.id.btn_download).setOnClickListener((View v1) -> {
            download();
            popWindow.dismiss();
        });
        view.findViewById(R.id.btn_fav).setOnClickListener((View v1) -> {
            if(cur_si instanceof ChannelInfoItem) {
                boolean ret = yt.addChannel((ChannelInfoItem)cur_si);
                toast("add channel return: "+ret);
            }
            popWindow.dismiss();
        });
    }

    private void toast(String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }
    final int REQUESTED = 12;
    private void download() {
        int permissionCheck = ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck== PackageManager.PERMISSION_GRANTED){
            //this means permission is granted and you can do read and write
            beginDown();
        }else{
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUESTED);
        }
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

        }else{
            toast("write external storage not allowed!");
        }
    }
    private void beginDown() {
        String path = mContext.getExternalFilesDir("0down").getAbsolutePath();
        try{
            File f = new File(path);
            f.mkdirs();
            final Intent intent = new Intent(mContext, DownService.class);
            intent.setAction("com.laog.service.DownService");
            intent.putExtra("path", path);
            intent.putExtra("url", cur_si.getUrl());
            intent.putExtra("name", cur_si.getName());
            intent.putExtra("isAudio", true);
            intent.putExtra("fmt", "m4a");
            intent.putExtra("solution", "");
            mContext.startService(intent);
        }catch(Exception e){
            toast("mkdirs failed:"+e.getMessage());
            Log.e(TAG, "startService failed", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUESTED: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    beginDown();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    void queryTask() {
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
                    dataModels.clear();
                    yt.writeSearchStr(ytQuery.getText().toString());
                    yt.writeSearchItems(list);
                    return Observable.fromIterable(list);
                })
                .subscribe((info) -> {
                    dataModels.add(info);
                    adapter.notifyDataSetChanged();
                });
    }


    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if(vid == R.id.button_query){
//            Log.d(TAG, "---  begin query ---");
            queryTask();
        }
    }
}
