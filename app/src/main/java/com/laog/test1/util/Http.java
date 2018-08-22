package com.laog.test1.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.SSLContext;

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
import cz.msebera.android.httpclient.client.protocol.HttpClientContext;
import cz.msebera.android.httpclient.config.Registry;
import cz.msebera.android.httpclient.config.RegistryBuilder;
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory;
import cz.msebera.android.httpclient.conn.socket.PlainConnectionSocketFactory;
import cz.msebera.android.httpclient.conn.ssl.SSLConnectionSocketFactory;
import cz.msebera.android.httpclient.cookie.Cookie;
import cz.msebera.android.httpclient.cookie.CookieOrigin;
import cz.msebera.android.httpclient.impl.client.BasicCookieStore;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.impl.conn.DefaultProxyRoutePlanner;
import cz.msebera.android.httpclient.impl.conn.PoolingHttpClientConnectionManager;
import cz.msebera.android.httpclient.impl.cookie.DefaultCookieSpec;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.protocol.HttpContext;
import cz.msebera.android.httpclient.ssl.SSLContexts;
import cz.msebera.android.httpclient.util.EntityUtils;

public class Http {

    private CloseableHttpClient httpclient;
    private HttpClientContext context;
    private BasicCookieStore cookieStore;
    private String cookie_file = "cookies.obj";
    private DefaultCookieSpec cookieSpec = new DefaultCookieSpec();
    private String baseDir = "d:/tmp/haodoo/";
    private boolean cookieChg = false;

    private String start_url = "";

    public static void main(String[] args) throws Exception {
        Http ht = new Http();
        ht.test();
    }

    // http://www.haodoo.net/
    private List<String> parseBookUrl(String page) {
        int p = 0, p1 = 0;
        List<String> ret = new LinkedList<>();
        while(true) {
            p = page.indexOf("?M=book", p);
            if(p < 0) break;
            p1 = page.indexOf('\"', p);
            if(p1 < 0) break;
            ret.add("http://www.haodoo.net/" + page.substring(p, p1));
            p = p1;
        }
        return ret;
    }
    private String inStr(String s, String begin, String end){ return inStr(s, begin, end, 0); }
    private String inStr(String s, String begin, String end, int p0) {
        int p = s.indexOf(begin, p0);
        if(p <0){
            log("inStr failed 1 ");
            return null;
        }
        p += begin.length();
        int p1 = s.indexOf(end, p);
        if(p1 < 0){
            log("inStr failed 2 ");
            return null;
        }
        return s.substring(p, p1);
    }
    private void saveEpub(String bookurl) throws Exception {
        String page = get(bookurl);
        String bookid = inStr(page, "value=\"下載 epub 檔\" onClick = \"DownloadEpub('", "')\";");
        String epuburl = "http://www.haodoo.net/?M=d&P="+bookid+".epub";
        String title = inStr(page, "SetTitle(\"", "\");");
        log("      title:"+title + "    epub-url: "+epuburl);
        get(epuburl, title + ".epub");
    }
    private void test() throws Exception {
        String[] urls = new String[]{ "http://www.haodoo.net/?M=hd&P=100-3", "http://www.haodoo.net/?M=hd&P=100-4", "http://www.haodoo.net/?M=hd&P=100-5"};
        for(String u: urls) {
            log("---collect: "+u);
            String page = get(u);
            List<String> bookUrls = parseBookUrl(page);
            for(String bookurl: bookUrls) {
                log("   --bookurl: "+bookurl);
                saveEpub(bookurl);
            }
        }
    }












    public Http() throws Exception{
        buildClient(true); //httpclient = HttpClients.createDefault();
    }
    public void login() throws Exception {
        log("----login");
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("warp_action", "login"));
        formparams.add(new BasicNameValuePair("hash_action", ""));
        formparams.add(new BasicNameValuePair("sendback", ""));
        formparams.add(new BasicNameValuePair("username", "gyf_freedom@sohu.com"));
        formparams.add(new BasicNameValuePair("password", "helloworld"));
        formparams.add(new BasicNameValuePair("remember_me", "on"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
        URI uri = new URI(start_url);
        HttpPost post = new HttpPost(uri);
        post.setEntity(entity);
        addDefHeaders(post);
        //warp_action=login&hash_action=&sendback=&username=gyf_freedom%40sohu.com&password=helloworld&remember_me=on

        CloseableHttpResponse resp = httpclient.execute(post);
        int statusCode = resp.getStatusLine().getStatusCode();
        if(302 == statusCode) {
            parseCookie(resp, uri);
            String location = resp.getFirstHeader("location").getValue();
            log("login success!!, redirect to "+location);
            resp.close();
            get(location,  "login");
        }else {
            log("login failed");
        }
    }


    static class MyConnectionSocketFactory extends SSLConnectionSocketFactory {
        public MyConnectionSocketFactory(final SSLContext sslContext) {
            super(sslContext);
        }
        @Override
        public Socket createSocket(final HttpContext context) throws IOException {
            InetSocketAddress socksaddr = (InetSocketAddress) context.getAttribute("socks.address");
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksaddr);
            return new Socket(proxy);
        }

    }
    private void buildClient(boolean useProxy) {
        // use http proxy
        DefaultProxyRoutePlanner routePlanner = null;
        if(useProxy) {
            HttpHost proxy = new HttpHost("127.0.0.1", 8083);
            routePlanner = new DefaultProxyRoutePlanner(proxy);
        }

        cookieStore = new BasicCookieStore();

        httpclient = HttpClients.custom()
                .setRoutePlanner(routePlanner)
                .setDefaultCookieStore(cookieStore)
                .build();

        /*
        // use socks proxy
        Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", new MyConnectionSocketFactory(SSLContexts.createSystemDefault()))
                .build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(reg);
        httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
        InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", 9150);
        context = HttpClientContext.create();
        context.setAttribute("socks.address", socksaddr); */
    }

    private String get(String url) throws Exception {
        HttpGet get = new HttpGet(url);
        addDefHeaders(get);
        CloseableHttpResponse resp = httpclient.execute(get, context);
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

    private String realPath(String p) {
        return baseDir + p;
    }

    private void get(String url, String fname) throws Exception {
        fname = realPath(fname);
        File f = new File(fname);
        if(f.exists()) {
            log("  "+fname + " already exists");
            return;
        }
        HttpGet get = new HttpGet(url);
        addDefHeaders(get);
        CloseableHttpResponse resp = httpclient.execute(get, context);
        log("execute get return "+resp.getStatusLine());
        saveResp(resp, fname);
    }

    private void saveResp(CloseableHttpResponse resp, String fname) throws Exception {
        if(200 != resp.getStatusLine().getStatusCode()) {
            log("saveResp failed, request failed with:"+resp.getStatusLine());
            return;
        }


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


    private void parseCookie(HttpResponse response, URI uri) throws Exception {
        CookieOrigin co = new CookieOrigin(uri.getHost(), uri.getPort()>0?uri.getPort():443, "/", true);  //uri.getPath()
        for(Header h: response.getHeaders("Set-Cookie")) {
            log("---" + h.getValue());
            List<Cookie> l = cookieSpec.parse(h, co);
            if(l != null) {
                for(Cookie c: l) {
                    cookieStore.addCookie(c);
                    cookieChg = true;
                }
            }else {
                log("failed to parse cookies");
            }
        }
    }

    private static void log(Object msg) {
        System.out.println(msg);
    }

    private String auth = null;
    private void addDefHeaders(HttpRequestBase req) {
        if(auth != null)  // Authorization: GoogleLogin auth=
            req.addHeader("Authorization", "GoogleLogin auth="+auth);
//        req.addHeader("AppId", "1000000383");
//        req.addHeader("AppKey", "qjkqDw3PXoacHrpahEL2OSDKgeiExZfu");
        req.addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        req.addHeader("accept-encoding", "gzip, deflate, br");
        req.addHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
        req.addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");
        req.addHeader("Cookie", "__utmc=111611677; __utmz=111611677.1533970330.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); __utma=111611677.1877317255.1533970330.1534945341.1534949119.5");
        req.addHeader("Host", "www.haodoo.net");
        req.addHeader("Referer", "http://www.haodoo.net/?M=hd&P=100-1");

    }
}
