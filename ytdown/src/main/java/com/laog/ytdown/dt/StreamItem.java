package com.laog.ytdown.dt;


import android.content.Context;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import gyf.test.YoutubeTest;

public class StreamItem {

    String version_number;
    String feature;

    String name;
    String type;
    String url;
    String updateDate;
    String desc;
    String duration;

    YoutubeTest.Stream stream;
    boolean loading = false;

    public String toString() {
        return name + ";"+type+";"+url+";"+updateDate+";"+desc +";"+duration;
    }
    public static StreamItem fromString(String s) {
        String[] x = s.split(";");
        StreamItem i = new StreamItem(x[0], x[1], "", "");
        i.duration = x[5];
        i.url = x[2];
        i.updateDate = x[3];
        i.desc = x[4];
        return i;
    }

    public StreamItem(String name, String type, String version_number, String feature ) {
        this.name=name;
        this.type=type;
        this.version_number=version_number;
        this.feature=feature;
    }

    public static void saveList(Context ctx, ArrayList<StreamItem> list) {
        String path = ctx.getExternalFilesDir("0down").getAbsolutePath();
        path = path +"/savelist.txt";
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(path));
            for(StreamItem i: list){
                pw.append(i.toString()).append('\n');
            }
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void loadList(Context ctx, ArrayList<StreamItem> list) {
        String path = ctx.getExternalFilesDir("0down").getAbsolutePath();
        path = path +"/savelist.txt";
        try {
            BufferedReader bf = new BufferedReader(new FileReader(path));
            while(true){
                String line = bf.readLine();
                if(line == null)
                    break;
                list.add(StreamItem.fromString(line.trim()));
            }
            bf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public YoutubeTest.Stream getStream() {
        return stream;
    }

    public void setStream(YoutubeTest.Stream stream) {
        this.stream = stream;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getVersion_number() {
        return version_number;
    }

    public String getFeature() {
        return feature;
    }

}

