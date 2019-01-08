package gyf.test;


import org.schabi.newpipe.extractor.Downloader;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.utils.Localization;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.RealResponseBody;
import okio.GzipSource;
import okio.Okio;

public class YTDownloader implements org.schabi.newpipe.extractor.Downloader {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:43.0) Gecko/20100101 Firefox/43.0";

    private static YTDownloader instance;
    private String mCookies;
    private final OkHttpClient client;

    private YTDownloader(OkHttpClient.Builder builder) {
        this.client = builder
                .addInterceptor(new UnzippingInterceptor())//开启Gzip压缩
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    private YTDownloader(OkHttpClient.Builder builder, String proxy_host, int proxy_port) {
        Proxy proxyTest = new Proxy(Proxy.Type.HTTP,new InetSocketAddress(proxy_host, proxy_port));
        this.client = builder
                .proxy(proxyTest)
                .addInterceptor(new UnzippingInterceptor())//开启Gzip压缩
                .readTimeout(30, TimeUnit.SECONDS)
                //.cache(new Cache(new File(context.getExternalCacheDir(), "okhttp"), 16 * 1024 * 1024))
                .build();
    }

    /**
     * It's recommended to call exactly once in the entire lifetime of the application.
     *
     * @param builder if null, default builder will be used
     */
    public static YTDownloader init(OkHttpClient.Builder builder) {
        return instance = new YTDownloader(builder != null ? builder : new OkHttpClient.Builder());
    }
    public static YTDownloader init(OkHttpClient.Builder builder, String proxy_host, int proxy_port) {
        return instance = new YTDownloader(builder != null ? builder : new OkHttpClient.Builder(), proxy_host, proxy_port);
    }

    public static Downloader getInstance() {
        return instance;
    }

    public String getCookies() {
        return mCookies;
    }

    public void setCookies(String cookies) {
        mCookies = cookies;
    }

    /**
     * Get the size of the content that the url is pointing by firing a HEAD request.
     *
     * @param url an url pointing to the content
     * @return the size of the content, in bytes
     */
    public long getContentLength(String url) throws IOException {
        Response response = null;
        try {
            final Request request = new Request.Builder()
                    .head().url(url)
                    .addHeader("User-Agent", USER_AGENT)
                    .build();
            response = client.newCall(request).execute();

            return Long.parseLong(response.header("Content-Length"));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid content length", e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * Download the text file at the supplied URL as in download(String),
     * but set the HTTP header field "Accept-Language" to the supplied string.
     *
     * @param siteUrl  the URL of the text file to return the contents of
     * @param localization the language and country (usually a 2-character code) to set
     * @return the contents of the specified text file
     */
    @Override
    public String download(String siteUrl, Localization localization) throws IOException, ReCaptchaException {
//        Map<String, String> requestProperties = new HashMap<>();
//        requestProperties.put("Accept-Language", "zh-CN,zh;q=0.9"); //localization.getLanguage());
//        requestProperties.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
//        return download(siteUrl, requestProperties);
        return download(siteUrl, (Map<String, String>)null);
    }

    /**
     * Download the text file at the supplied URL as in download(String),
     * but set the HTTP headers included in the customProperties map.
     *
     * @param siteUrl          the URL of the text file to return the contents of
     * @param customProperties set request header properties
     * @return the contents of the specified text file
     * @throws IOException
     */
    @Override
    public String download(String siteUrl, Map<String, String> customProperties) throws IOException, ReCaptchaException {
        return getBody(siteUrl, customProperties).string();
    }

    public InputStream stream(String siteUrl) throws IOException {
        try {

//            return getBody(siteUrl, Collections.emptyMap()).byteStream();
            Map<String, String> k = new LinkedHashMap<>();
            return getBody(siteUrl, k).byteStream();
        } catch (ReCaptchaException e) {
            throw new IOException(e.getMessage(), e.getCause());
        }
    }


    private ResponseBody getBody(String siteUrl, Map<String, String> customProperties) throws IOException, ReCaptchaException {
        final Request.Builder requestBuilder = new Request.Builder()
                .method("GET", null).url(siteUrl)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Accept-Language", "en-US,en;q=0.5")
                .addHeader("Accept-Encoding" , "gzip")
                ;

        if(customProperties != null)
            for (Map.Entry<String, String> header : customProperties.entrySet()) {
                requestBuilder.addHeader(header.getKey(), header.getValue());
            }

        if (!Utils.isEmpty(mCookies)) {
            requestBuilder.addHeader("Cookie", mCookies);
        }

        final Request request = requestBuilder.build();
        final Response response = client.newCall(request).execute();
        final ResponseBody body = response.body();

        if (response.code() == 429) {
            throw new ReCaptchaException("reCaptcha Challenge requested");
        }

        if (body == null) {
            response.close();
            return null;
        }

        return body;
    }

    /**
     * Download (via HTTP) the text file located at the supplied URL, and return its contents.
     * Primarily intended for downloading web pages.
     *
     * @param siteUrl the URL of the text file to download
     * @return the contents of the specified text file
     */
    @Override
    public String download(String siteUrl) throws IOException, ReCaptchaException {
        Map<String, String> k = new LinkedHashMap<>();
        String ret= download(siteUrl, k);
//        System.out.println(ret);
        return ret;
    }

//    private String proxy_host = "127.0.0.1";
//    private int proxy_port = 8083;
    /**
     * Download (via HTTP) the text file located at the supplied URL, and return its contents.
     * Primarily intended for downloading web pages.
     *
     * @param siteUrl the URL of the text file to download
     * @return the contents of the specified text file
     */
//    public String download(String siteUrl) throws IOException, ReCaptchaException {
//        URL url = new URL(siteUrl);
//        HttpsURLConnection con = null;
//        if(proxy_host != null){
//            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy_host, proxy_port));
//            con = (HttpsURLConnection) url.openConnection(proxy);
//        }else {
//            con = (HttpsURLConnection) url.openConnection();
//        }
//        //HttpsURLConnection con = NetCipher.getHttpsURLConnection(url);
//        return dl(con);
//    }

    private class UnzippingInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Response response = chain.proceed(chain.request());
            return unzip(response);
        }


            // copied from okhttp3.internal.http.HttpEngine (because is private)
            private Response unzip(final Response response) throws IOException
            {
                if (response.body() == null)
                {
                    return response;
                }

                //check if we have gzip response
                String contentEncoding = response.headers().get("Content-Encoding");

                //this is used to decompress gzipped responses
                if (contentEncoding != null && contentEncoding.equals("gzip"))
                {
                    Long contentLength = response.body().contentLength();
                    GzipSource responseBody = new GzipSource(response.body().source());
                    Headers strippedHeaders = response.headers().newBuilder().build();
                    return response.newBuilder().headers(strippedHeaders)
                            .body(new RealResponseBody(response.body().contentType().toString(), contentLength, Okio.buffer(responseBody)))
                            .build();
                }
                else
                {
                    return response;
                }
            }
        }

}
