package com.laog.test1.ng;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.laog.test1.R;
import com.laog.test1.db.FeedItem;

import java.util.List;

public class MyAdapter extends BaseAdapter {

    private List<FeedItem> mData;
    private Context mContext;

    public MyAdapter(List<FeedItem> mData, Context mContext) {
        this.mData = mData;
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if(convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.ng_list_item,parent,false);
            viewHolder = new ViewHolder();
            viewHolder.txt_item_title = (TextView) convertView.findViewById(R.id.txt_item_title);
            viewHolder.txt_date = (TextView) convertView.findViewById(R.id.date);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.txt_item_title.setText(mData.get(position).getTitle());
        viewHolder.txt_date.setText(mData.get(position).getS_published());
        return convertView;
    }

    private class ViewHolder{
        TextView txt_item_title;
        TextView txt_date;
    }

}
