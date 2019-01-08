package com.laog.ytdown.dt;


import android.content.Context;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.laog.ytdown.R;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.ArrayList;

import gyf.test.Utils;

public class CustomAdapter extends ArrayAdapter<InfoItem> implements View.OnClickListener{

    private ArrayList<InfoItem> dataSet;
    Context mContext;

    // View lookup cache
    private static class ViewHolder {
        TextView txtName;
        TextView txtType;
        TextView txtUploadTime;
        TextView txtDuration;
        ImageView info;
    }

    public CustomAdapter(ArrayList<InfoItem> data, Context context) {
        super(context, R.layout.ag_row_item, data);
        this.dataSet = data;
        this.mContext=context;

    }

    @Override
    public void onClick(View v) {

        int position=(Integer) v.getTag();
        Object object= getItem(position);
        InfoItem item = (InfoItem)object;

        switch (v.getId())
        {
            case R.id.item_info:
                Snackbar.make(v, "URL: " +item.getUrl(), Snackbar.LENGTH_LONG)
                        .setAction("No action", null).show();
                break;
        }
    }

    private int lastPosition = -1;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        InfoItem item = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.ag_row_item, parent, false);
            viewHolder.txtName = (TextView) convertView.findViewById(R.id.name);
            viewHolder.txtType = (TextView) convertView.findViewById(R.id.type);
            viewHolder.txtUploadTime = (TextView) convertView.findViewById(R.id.uploadtime);
            viewHolder.txtDuration = (TextView) convertView.findViewById(R.id.duration);
            viewHolder.info = (ImageView) convertView.findViewById(R.id.item_info);

            result=convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result=convertView;
        }

        Animation animation = AnimationUtils.loadAnimation(mContext, (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
        result.startAnimation(animation);
        lastPosition = position;

        viewHolder.txtName.setText(item.getName());
        viewHolder.txtType.setText(item.getInfoType().toString());
        if(item instanceof StreamInfoItem) {
            StreamInfoItem sitem = (StreamInfoItem)item;
            viewHolder.txtUploadTime.setText(sitem.getUploadDate());
            viewHolder.txtDuration.setText(Utils.dura(sitem.getDuration()));
        }else{
            viewHolder.txtUploadTime.setText("");
            viewHolder.txtDuration.setText("");
        }
        viewHolder.info.setOnClickListener(this);
        viewHolder.info.setTag(position);
        // Return the completed view to render on screen
        return convertView;
    }
}

