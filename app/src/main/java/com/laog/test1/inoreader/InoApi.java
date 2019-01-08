package com.laog.test1.inoreader;

import android.util.Log;

import com.laog.test1.db.FeedItem;
import com.laog.test1.db.FeedItemDao;
import com.laog.test1.db.MN;
import com.laog.test1.db.State;
import com.laog.test1.util.TextUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Files;
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

	private volatile String rootDir;
	private volatile String tmpDir;
	private volatile FeedItemDao dao;

	private String realPath(String p) {
		if(p.charAt(0) == '/' || p.charAt(1) == ':')
			return p;
		return tmpDir + p;
	}

	public static void main(String[] args) throws Exception {
		InoApi n = new InoApi("d:/tmp/ino", null, "127.0.0.1:8083");
		String testOP = "list";
		String recordFile = "D:/tmp/ino/record.dat";
		String outDir = "D:/tmp/ino";
		if("down".equals(testOP)) {
			n.download2File(recordFile, outDir);
		}else if("list".equals(testOP)) {
			n.listRecords(recordFile, outDir);
		}

//		n.login();
//        n.auth = "ZyNFL1R22Irs7uRXUlu3sf_4EUsD_ni7";
//		n.content("user%2F-%2Fstate%2Fcom.google%2Freading-list?r=o&n=5&ot="+(System.currentTimeMillis()/1000-3600*24));
	}
	private void listRecords(String recordFile, String outDir) throws Exception {
		InputStream fin = new FileInputStream(recordFile);
		while(true) {
			FeedItem fi = FeedItem.readRecord(fin);
			if(fi == null)
				break;
			//fi.loadContent(outDir);
//			fi.print();
			if("tag:google.com,2005:reader/item/000000041fdba90d".equals(fi.getId())){
				fi.loadContent(outDir);
				fi.print();
				for(String img: Article.parseImageLinks(fi.rawContent)){
					System.out.println("    " + img);
					String fname = img.substring(img.lastIndexOf('/')+1);
					if(fname.indexOf('?') > 0)
						fname = fname.substring(0, fname.indexOf('?'));
					get(img, fname);
				}
			}
		}
		fin.close();
	}

	/**
	 * 下载数据, 并保存到文件中,  只是pc上的测试代码
	 * @return
	 * @throws Exception
	 */
	private int download2File(String recordFile, String outDir) throws Exception {
		JsonUtil ju = new JsonUtil();
		long maxTime = System.currentTimeMillis()/1000 - 24*3600; //往后推一天, 这个参数是feeds开始的时间, 单位是秒
		int count = 0;
		log("begin downloading, maxtime: "+maxTime);
		String feed = "user%2F-%2Fstate%2Fcom.google%2Freading-list?r=o&n=50&ot="+maxTime;
		OutputStream pw = new FileOutputStream(recordFile);
		while(true) {
			String url = api_url + "stream/contents/" + feed;
			String content = get(url);
			List<FeedItem> lst = ju.parseStream(content);
			if(lst == null) {
				System.out.println("--- not aticles found!");
				break;
			}
			for(FeedItem fi: lst){
				try {
					fi.saveContent(outDir);
					fi.saveRecord(pw);
				}catch (Exception e){ Log.e("", "dulplicate item:"+fi.getId()); }
			}
			count += lst.size();
			String c = ju.getC();
			log("download "+c + " count:"+lst.size() + " " + lst.get(lst.size()-1).getS_published());
			if(c == null)
				break;
			feed = "user%2F-%2Fstate%2Fcom.google%2Freading-list?r=o&n=50&ot="+maxTime+"&c="+c;
		}
		pw.close();
		return count;
	}



	public InoApi(String rootdir, FeedItemDao dao, boolean useProxy) {
		this.dao = dao;
		if(rootdir != null) {
			this.rootDir = rootdir;
			tmpDir = rootDir + File.separator + "tmp" + File.separator;
			File f = new File(tmpDir);
			if (!f.exists()) f.mkdirs();
			auth_file = tmpDir + File.separator + "auth_file";
			f = new File(auth_file);
			if (f.exists()) {
				try {
					auth = Utils.fileToString(auth_file).trim();
				} catch (Exception e) {
					e.printStackTrace();
				}
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

	public InoApi(String rootdir, FeedItemDao dao, String proxyaddr){
		this(rootdir, dao, false);
		if(proxyaddr != null){
			int p=proxyaddr.lastIndexOf(':');
			HttpHost proxy = new HttpHost(proxyaddr.substring(0, p), Integer.parseInt(proxyaddr.substring(p+1)));
			DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
			httpclient = HttpClients.custom().setRoutePlanner(routePlanner)	.build();
		}
	}

	/**
	 * 统计库中的记录信息和 文件的情况
	 * @throws Exception
	 */
	public String state() throws Exception{
		StringBuilder sb = new StringBuilder();
		if(dao != null){
			sb.append("=========state in feeditems\n");
			for(State st: dao.state()) {
				sb.append("   "+st.getFeeday() + "  " + st.getCt()).append('\n');
			}
		}
		sb.append("==== files in "+tmpDir).append('\n');
		File ft = new File(tmpDir);
		for(File f: ft.listFiles()) {
			if(f.isDirectory()) continue;
			sb.append("  " + f.getName() + " len: "+f.length() + " lastmodify: "+Utils.timeToStr(f.lastModified())).append('\n');
		}
		return sb.toString();
	}

    /**
     * 归档, 把上个月及以前的数据保存到sdcard上, 并删除当前数据中的这部分
     * @param path
     * @throws Exception
     */
	public String archive(String path) throws  Exception {
        //Done 归档的实现
		return removeOldContent();
	}

	public String backup(String path) throws Exception {
		log("---path:" + path);
		String fpath = path + File.separator + "backup";
		File ft = new File(fpath);
		if(!ft.exists())
		    ft.mkdirs();
		ft = new File(tmpDir);
		StringBuilder sb = new StringBuilder();
		sb.append("=== backup from "+tmpDir +" to "+fpath).append('\n');
		for(File f: ft.listFiles()) {
            if(f.isDirectory())
                continue;
            Utils.copy(f.getAbsolutePath().toString(), fpath + File.separator + f.getName());
            sb.append("backup file ").append(f.getName()).append('\n');
        }

        sb.append("==== backup database sqlite to file\n");
        List<FeedItem> lst = dao.getAll();
        File f1 = new File(fpath + File.separator + "feeditems."+System.currentTimeMillis());
        OutputStream out = new FileOutputStream(f1);
		for(FeedItem fi: lst) {
		    fi.saveRecord(out);
        }
        out.close();
        sb.append("   record count:"+lst.size() +"  file length:"+f1.length());
        return sb.toString();
	}

    private long g_maxTime;
	private int down_pagesize = 50;
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
			else
				maxTime = System.currentTimeMillis()/1000 - oneday;
			g_maxTime = maxTime;
			Log.d("", "load from "+Utils.timeToStr(maxTime));
            feed = "user%2F-%2Fstate%2Fcom.google%2Freading-list?r=o&n="+down_pagesize+"&ot="+maxTime;
        }else{
            feed = "user%2F-%2Fstate%2Fcom.google%2Freading-list?r=o&n="+down_pagesize+"&ot="+g_maxTime+"&c="+c;
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
        int lstsize = lst.size();
        String prompt = String.valueOf(lstsize);
        if(lstsize > 0) {
        	final String pt = "minTime: " + lst.get(0).getS_published() + " maxTime: " + lst.get(lstsize - 1).getS_published();
			Log.d("", pt);
			prompt = prompt + " " + pt;
		}
        return new String[]{c, prompt};
    }

    private int pagesize = 20;
	private long mintime = 0L;
	private long maxtime = 0L;
	private long oneday = 24 * 3600;
    public List<FeedItem> loadnew() throws Exception {
	    if(dao == null) {
	    	Log.d("---", "loadnew failed, dao is null");
			return null;
		}
        if(mintime <= 0L)
            mintime = dao.findMaxTime() - oneday;
		List<FeedItem> lst = null;
		for(int i=0; i<6; i++) {
			lst = dao.getPage(mintime, pagesize);
			if(lst != null && lst.size() > 0)
				break;
		}
		if(lst == null || lst.size() == 0)
			return null;
        for(FeedItem fi: lst) {
            fi.loadContent(tmpDir);
        }
        mintime = lst.get(lst.size()-1).getPublished();
        maxtime = lst.get(0).getPublished();
        Log.d("", "loadnew mintime: "+mintime + " maxtime: "+maxtime);
	    return lst;
    }

    public List<FeedItem> loadold() throws Exception {
        if(dao == null)
            return null;

        maxtime = mintime;
        mintime = mintime - oneday;
		Log.d("", "loadold begin: "+mintime + " "+maxtime);
        List<FeedItem> lst = dao.getPage(mintime, maxtime, pagesize);
        if(lst == null || lst.size() == 0) {
        	Log.w("", "load old failed lst is null:"+(lst== null));
			return null;
		}
        for(FeedItem fi: lst) {
            fi.loadContent(tmpDir);
        }
        mintime = lst.get(lst.size()-1).getPublished();
        maxtime = lst.get(0).getPublished();
		Log.d("", "loadold mintime: "+mintime + " maxtime: "+maxtime + " count: "+lst.size());

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

	/**
	 *  删除上个月及以前的内容, 包括文件和sqlite中的
	 */
	public String removeOldContent() {
    	long tm = TextUtil.getLastDayOfLastMonth();
    	List<MN> m = dao.getmn();
    	MN mn = m.get(0);
    	//Log.d("", "all rowcount:"+dao.getCount() + " tm:"+tm + " minp:"+mn.getMnp() + " mxp:"+mn.getMxp());
    	int deleteRows = dao.deleteByPubTime(tm);
		Log.d("", "all rowcount:"+dao.getCount() + " delcount:"+deleteRows + " tm:"+tm + " minp:"+mn.getMnp() + " mxp:"+mn.getMxp());

    	String mon = null;
    	int deleteFiles = 0;
    	while(true) {
    		mon = TextUtil.getLastMonthStr(mon);
    		String fpath = realPath(mon);
    		File f = new File(fpath);
			Log.d("", "delete file: "+f.getAbsolutePath() + " exists:"+f.exists());
    		if(!f.exists())
    			break;

    		f.delete();
    		deleteFiles += 1;
		}
		return "removeOldContent summury:\n deleteRows: " +deleteRows + " deleteFiles: "+deleteFiles;
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

	public void get(String url, String fname) throws Exception {
		HttpGet get = new HttpGet(url);
		addDefHeaders(get);
		CloseableHttpResponse resp = httpclient.execute(get);
		int length = -1;
		if(resp.getFirstHeader("content-length") != null) {
			length = Integer.parseInt(resp.getFirstHeader("content-length").getValue())/1024;
		}
		log("execute get "+url +" return "+resp.getStatusLine() + " length:"+length + " kb");
		saveResp(resp, fname);
	}

	public String get(String url) throws Exception {
		HttpGet get = new HttpGet(url);
		addDefHeaders(get);
		CloseableHttpResponse resp = httpclient.execute(get);
		final int rcode = resp.getStatusLine().getStatusCode();
		log("--get "+url + " return "+rcode + " rootDir="+rootDir);
		if(200 == rcode) {
            return EntityUtils.toString(resp.getEntity());
        }else if(401 == rcode){
		    if(rootDir != null) {
				login();
				return get(url);
			}
        }else {
			log("get failed:" + resp.getStatusLine());
		}
		return null;
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
