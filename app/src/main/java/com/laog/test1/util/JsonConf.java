package com.laog.test1.util;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.laog.test1.inoreader.Utils;

import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.util.Map;

public class JsonConf {

    private String confPath;
    private JSONObject conf = null;
    public static final String SPEECH_RATE = "speech_rate";

    public JsonConf(Context ctx, String conf) {
        confPath = ctx.getFilesDir().getAbsolutePath().toString() + File.separator + conf;
    }

    public String get(String key) {
        try {
            if (conf == null)
                conf = JSON.parseObject(Utils.fileToString(confPath));
            return conf.getString(key);
        }catch (Exception e) {
            Log.e("", "load conf failed", e);
        }
        return null;
    }
    public void set(String key, String val) {
        try {
            if (conf == null) {
                try {
                    conf = JSON.parseObject(Utils.fileToString(confPath));
                }catch (Exception e){}
            }
            if(conf == null)
                conf = new JSONObject();
            conf.put(key, val);
            Utils.stringToFile(confPath, conf.toJSONString());
        }catch (Exception e) {
            Log.e("", "save conf failed", e);
        }
    }
}
