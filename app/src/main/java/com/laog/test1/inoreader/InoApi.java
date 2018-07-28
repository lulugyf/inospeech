package com.laog.test1.inoreader;

import android.util.Log;

import com.laog.test1.db.FeedItem;
import com.laog.test1.db.FeedItemDao;
import com.laog.test1.db.State;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Consts;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpHost;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.methods.HttpRequestBase;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.impl.conn.DefaultProxyRoutePlanner;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;


public class InoApi {
    private final static String base_url = "https://www.inoreader.com";
	private final static String api_url = "https://www.inoreader.com/reader/api/0/";
	private String auth_file = null;

	private CloseableHttpClient httpclient;

	private String rootDir;
	private String tmpDir;
	private FeedItemDao dao;

	private String realPath(String p) {
		return tmpDir + p;
	}

	public static void main(String[] args) throws Exception {
		InoApi n = new InoApi("/tmp", null, false);
//		n.login();
//        n.auth = "ZyNFL1R22Irs7uRXUlu3sf_4EUsD_ni7";
		n.content("user%2F-%2Fstate%2Fcom.google%2Freading-list?r=o&n=5&ot="+(System.currentTimeMillis()/1000-3600*24));
	}


	public InoApi(String rootdir, FeedItemDao dao, boolean useProxy) {
		this.rootDir = rootdir;
		this.dao = dao;
		tmpDir = rootDir + File.separator + "tmp" + File.separator;
		File f = new File(tmpDir);
		if(!f.exists() ) f.mkdirs();
		auth_file = tmpDir + File.separator + "auth_file";
		f = new File(auth_file);
		if(f.exists()) {
            try {
                auth = Utils.fileToString(auth_file).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

		if(useProxy){
			HttpHost proxy = new HttpHost("172.22.0.23", 8989);
			DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
			httpclient = HttpClients.custom()
					.setRoutePlanner(routePlanner)
					.build();
		}else
			httpclient = HttpClients.createDefault();
	}

	/**
	 * 统计库中的记录信息和 文件的情况
	 * @throws Exception
	 */
	public void state() throws Exception{
		if(dao != null){
			log("=========state in feeditems");
			for(State st: dao.state()) {
				log("   "+st.getFeeday() + "  " + st.getCt());
			}
		}
		log("==== files in "+tmpDir);
		File ft = new File(tmpDir);
		for(File f: ft.listFiles()) {
			if(f.isDirectory()) continue;
			log("  " + f.getName() + " len: "+f.length() + " lastmodify: "+Utils.timeToStr(f.lastModified()));
		}
	}

	public void archive(String path) throws  Exception {

	}

	public void backup(String path) throws Exception {

	}

    /**
     * 下载数据
     * @return
     * @throws Exception
     */
	public int download() throws Exception {
	    if(dao == null)
	        return -1;
	    JsonUtil ju = new JsonUtil();
	    long maxTime = dao.findMaxTime();
	    if(maxTime > 0L)
	    	maxTime -= 3600; //往后推一个小时, 这个参数是feeds开始的时间
	    int count = 0;
        log("begin downloading, maxtime: "+maxTime);
	    String feed = "user%2F-%2Fstate%2Fcom.google%2Freading-list?r=o&n=50&ot="+maxTime;
	    while(true) {
            String url = api_url + "stream/contents/" + feed;
            String content = get(url);
            List<FeedItem> lst = ju.parseStream(content);
            for(FeedItem fi: lst){
                try {
                    dao.insert(fi); // 先插入记录, 如果有重复就略过了, 避免内容文件里有重复的, 但表中没有
					fi.saveContent(tmpDir);
					dao.updateItem(fi);
                }catch (Exception e){ Log.e("", "dulplicate item:"+fi.getId()); }
            }
            count += lst.size();
            String c = ju.getC();
            log("download "+c + " count:"+lst.size() + " " + lst.get(lst.size()-1).getS_published());
	        if(c == null)
	            break;
            feed = "user%2F-%2Fstate%2Fcom.google%2Freading-list?r=o&n=50&ot="+maxTime+"&c="+c;
        }
        return count;
    }

    public String[] download(String c) throws Exception {
        if(dao == null) {
            log("download failed: dao is null");
            return null;
        }
        String feed = null;
        if(c == null){
            long maxTime = dao.findMaxTime();
            if(maxTime > 0L)
                maxTime -= 3600; //往后推一个小时, 这个参数是feeds开始的时间
            feed = "user%2F-%2Fstate%2Fcom.google%2Freading-list?r=o&n=50&ot="+maxTime;
        }else{
            feed = "user%2F-%2Fstate%2Fcom.google%2Freading-list?r=o&n=50&ot=0&c="+c;
        }
        String url = api_url + "stream/contents/" + feed;
        String content = get(url);
        JsonUtil ju = new JsonUtil();
        List<FeedItem> lst = ju.parseStream(content);
        for(FeedItem fi: lst){
            try {
                dao.insert(fi); // 先插入记录, 如果有重复就略过了, 避免内容文件里有重复的, 但表中没有
                fi.saveContent(tmpDir);
                dao.updateItem(fi);
            }catch (Exception e){ Log.e("", "dulplicate item:"+fi.getId()); }
        }
        Log.d("", "download:"+feed+" size:"+lst.size());
        Log.d("", "download: max date:"+lst.get(lst.size()-1).getS_published());
        c = ju.getC();
        return new String[]{c, String.valueOf(lst.size())};
    }

    private int pagesize = 20;
	private long mintime = 0L;
	private long maxtime = 0L;
	private long oneday = 24 * 3600;
    public List<FeedItem> loadnew() throws Exception {
	    if(dao == null)
	        return null;
        if(mintime <= 0L)
            mintime = System.currentTimeMillis() / 1000 - oneday;
        List<FeedItem> lst = dao.getPage(mintime, pagesize);
        if(lst == null || lst.size() == 0)
            return null;
        for(FeedItem fi: lst) {
            fi.loadContent(tmpDir);
        }
        mintime = lst.get(lst.size()-1).getPublished();
        maxtime = lst.get(0).getPublished();
	    return lst;
    }

    public List<FeedItem> loadold() throws Exception {
        if(dao == null)
            return null;

        maxtime = mintime-oneday;
        List<FeedItem> lst = dao.getPage(maxtime, mintime, pagesize);
        if(lst == null || lst.size() == 0)
            return null;
        for(FeedItem fi: lst) {
            fi.loadContent(tmpDir);
        }
        mintime = lst.get(lst.size()-1).getPublished();
        maxtime = lst.get(0).getPublished();
        return lst;
    }

    public void content(String feed) throws Exception {
        // https://www.inoreader.com/reader/api/0/stream/contents/user%2F-%2Fstate%2Fcom.google%2Fread
        if(auth == null)
            login();
        String url = api_url + "stream/contents/" + feed;
        String content = get(url);
        if(content != null) {
            JsonUtil ju = new JsonUtil();
            List<FeedItem> lst = ju.parseStream(content);
        }

    }


    public void login() throws Exception {
        log("----login");
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("Email", "gyf_freedom@sohu.com"));
        formparams.add(new BasicNameValuePair("Passwd", "helloworld"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
        URI uri = new URI(base_url + "/accounts/ClientLogin");
        HttpPost post = new HttpPost(uri);
        post.setEntity(entity);
        addDefHeaders(post);

        CloseableHttpResponse resp = httpclient.execute(post);
        int statusCode = resp.getStatusLine().getStatusCode();
        if(200 == statusCode) {
            //printResp(resp);
            String s = EntityUtils.toString(resp.getEntity());
            auth = strMid(s, "Auth=", "\n");
            Utils.stringToFile(auth_file, auth);
        }else {
            log("login failed");
        }
    }
    private final String strMid(String s, String b, String e) {
	    if(s == null) return null;
	    int p = s.indexOf(b);
	    if(p < 0) return null;
	    p += b.length();
	    int p1 = s.indexOf(e, p);
	    if(p1 > 0) return s.substring(p, p1);
	    return s.substring(p);
    }

	private void printResp(HttpResponse resp) throws IOException{
		log("==response: "+resp.getStatusLine());
		for(Header h: resp.getAllHeaders()) {
			log("   "+ h.getName() + ": " + h.getValue());
		}
		log(EntityUtils.toString(resp.getEntity()));
	}

	private void get(String url, String fname) throws Exception {
		HttpGet get = new HttpGet(url);
		addDefHeaders(get);
		CloseableHttpResponse resp = httpclient.execute(get);
		log("execute get return "+resp.getStatusLine());
		saveResp(resp, fname);
	}

	private String get(String url) throws Exception {
		HttpGet get = new HttpGet(url);
		addDefHeaders(get);
		CloseableHttpResponse resp = httpclient.execute(get);
		final int rcode = resp.getStatusLine().getStatusCode();
		if(200 == rcode) {
            return EntityUtils.toString(resp.getEntity());
        }else if(401 == rcode){
		    login();
		    return get(url);
        }else {
			log("get failed:" + resp.getStatusLine());
			return null;
		}
	}

	private void saveResp(CloseableHttpResponse resp, String fname) throws Exception {
		if(200 != resp.getStatusLine().getStatusCode()) {
			log("saveResp failed, request failed with:"+resp.getStatusLine());
			return;
		}
		fname = realPath(fname);

		OutputStream fw = new FileOutputStream(fname);
		String ctype = resp.getFirstHeader("Content-Type").getValue();
		if(ctype.indexOf("application/json") > 0 && !fname.endsWith(".json"))
			fname = fname + ".json";
		else if(ctype.indexOf("text/html") > 0 && !fname.endsWith(".html"))
			fname = fname + ".html";
		else if(ctype.indexOf("application/javascript") > 0 && !fname.endsWith(".js"))
			fname = fname + ".js";
		try {
			HttpEntity entity = resp.getEntity();

			entity.writeTo(fw);
			log("response save to "+fname);
		} finally {
			resp.close();
			fw.close();
		}
	}

	private static void log(Object msg) {
		System.out.println(msg);
	}

	private String auth = null;
	private void addDefHeaders(HttpRequestBase req) {
	    if(auth != null)  // Authorization: GoogleLogin auth=
	        req.addHeader("Authorization", "GoogleLogin auth="+auth);
		req.addHeader("AppId", "1000000383");
		req.addHeader("AppKey", "qjkqDw3PXoacHrpahEL2OSDKgeiExZfu");
		req.addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
		req.addHeader("accept-encoding", "gzip, deflate, br");
		req.addHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
		req.addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");

	}

}
