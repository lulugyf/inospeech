package gyf.test;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.services.youtube.YoutubeService;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.extractor.utils.Localization;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class YoutubeTest {

    static Localization localization = new Localization("CN", "zh");

    private String rootDir;
    private YoutubeService ys;
    private YTDownloader ytDownloader;
    public YoutubeTest() {
        ytDownloader = YTDownloader.init(null);
        NewPipe.init(ytDownloader, localization);
        ys = new YoutubeService(0);
    }

    public void setRootDir(String s) { rootDir = s;}

    private String searchNextPage = null;
    private SearchExtractor searchExtractor;
    public List<InfoItem> search(String str) throws Exception {
        if(searchExtractor != null && str == null){
            // search next page
            ListExtractor.InfoItemsPage<InfoItem> items = searchExtractor.getPage(searchNextPage);
            searchNextPage = searchExtractor.getNextPageUrl();
            return items.getItems();
        }else {
            SearchQueryHandlerFactory searchHF = ys.getSearchQHFactory();
            String searchStr = searchHF.getUrl(str, new LinkedList<String>(), null);
            System.out.println(searchStr);

            searchExtractor = ys.getSearchExtractor(searchHF.fromQuery(searchStr));
            ListExtractor.InfoItemsPage<InfoItem> items = searchExtractor.getPage(searchStr + "&page=0");
            searchNextPage = searchExtractor.getNextPageUrl();
            return items.getItems();
        }
    }

    private String channelNextPage;
    private ChannelExtractor chExtractor;
    public List<InfoItem> channel(String url) throws Exception {
        if(channelNextPage != null && url == null){
            // fetch next page
            ListExtractor.InfoItemsPage<StreamInfoItem> items = chExtractor.getPage(channelNextPage);
            channelNextPage = chExtractor.getNextPageUrl();
            List<InfoItem> l = new ArrayList<>(items.getItems().size());
            for(StreamInfoItem si: items.getItems())
                l.add(si);
            return l;
        }else{
            chExtractor =
                    ys.getChannelExtractor(ys.getChannelLHFactory().fromUrl(url), localization);
            chExtractor.fetchPage();
            ListExtractor.InfoItemsPage<StreamInfoItem> items = chExtractor.getInitialPage();
            channelNextPage = chExtractor.getNextPageUrl();
            List<InfoItem> l = new ArrayList<>(items.getItems().size());
            for(StreamInfoItem si: items.getItems())
                l.add(si);
            return l;
        }
    }

    public List<InfoItem> readChannelItems(String url){
        String cid = url.substring(url.lastIndexOf('/')+1);
        return readItems("/channel_"+cid);
    }
    public void writeChannelItems(List<InfoItem> list, String url){
        String cid = url.substring(url.lastIndexOf('/')+1);
        writeItems(list, "/channel_"+cid);
    }

    public String readSearchStr() {
        return Utils.readStr(rootDir + "/searchStr");
    }
    public void writeSearchStr(String str) {
        Utils.writeStr(str, rootDir + "/searchStr");
    }

    private List<InfoItem> readItems(String file){
        File f = new File(rootDir + file);
        ArrayList<InfoItem> list = new ArrayList<>();
        if(!f.exists())
            return list;
        InputStream fin = null;
        try {
            fin = new FileInputStream(f);
            while(true){
                InfoItem ii = Utils.readItem(fin);
                if(ii == null)
                    break;
                list.add(ii);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try{
                if(fin != null) fin.close();
            }catch (IOException e){}
        }
        return list;
    }
    public void writeItems(List<InfoItem> list, String file) {
        OutputStream fout = null;
        try {
           fout = new FileOutputStream(rootDir + file);
            for(InfoItem ii: list)
                Utils.writeItem(ii, fout);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if (fout != null) fout.close();
            }catch (IOException e){}
        }
    }

    public List<InfoItem> readSearchItems(){
        return readItems("/searchResults");
    }
    public void writeSearchItems(List<InfoItem> list){
        writeItems(list, "/searchResults");
    }

    public List<InfoItem> readChannelList(){
        return readItems("/channels");
    }
    public void writeChannelList(List<InfoItem> list){
        writeItems(list, "/channels");
    }
    public boolean addChannel(ChannelInfoItem ci) {
        List<InfoItem> list = readChannelList();
        for(InfoItem i: list){
            if(i.getUrl().equals(ci.getUrl())){
                return false;
            }
        }
        list.add(ci);
        writeChannelList(list);
        return true;
    }


    // 关键字 搜索测试
    public static void test_search(YoutubeService ys, String str) throws Exception {
        SearchQueryHandlerFactory searchHF = ys.getSearchQHFactory();
        String searchStr = searchHF.getUrl(str, new LinkedList<String>(), null);
        System.out.println(searchStr);
//        searchStr = searchStr + "&page=0";

        SearchExtractor searchExtractor = ys.getSearchExtractor(searchHF.fromQuery(searchStr));
//        searchExtractor.fetchPage();

        System.out.println("--- search result:");
        OutputStream fout = new FileOutputStream("d:/tmp/search_"+str);
        ListExtractor.InfoItemsPage<InfoItem> items = searchExtractor.getPage(searchStr+ "&page=0");
        for(InfoItem ii: items.getItems()){
            System.out.println(Utils.toJson(ii));
            Utils.writeItem(ii, fout);
//            if(ii instanceof StreamInfoItem) {
//                StreamInfoItem i1 = (StreamInfoItem)ii;
//                System.out.println(ii.getName() + "---" + ii.getInfoType().name() + " date:" + i1.getUploadDate() + " dur:" + i1.getDuration());
//            }else if(ii instanceof ChannelInfoItem) {
//                System.out.println(ii.getName() + "---" + ii.getInfoType().name());
//            }
//            System.out.println( "   : " +ii.getUrl());

        }
        fout.close();

//        System.out.println("--- get nextpage:");
//        items = searchExtractor.getPage(searchExtractor.getNextPageUrl());
//        for(InfoItem ii: items.getItems()){
//            System.out.println(ii.getName());
//            System.out.println( "   : " +ii.getUrl());
//        }
    }

    private static void test_channel(YoutubeService ys, String cid) throws Exception {
        // -Wen Zhao Official文昭談古論今---CHANNEL
        //   : https://www.youtube.com/channel/UCtAIPjABiQD3qjlEl1T5VpA
//        String channelUrl = "https://www.youtube.com/channel/"+cid;
        ChannelExtractor chExtractor =
                ys.getChannelExtractor(ys.getChannelLHFactory().fromUrl("https://www.youtube.com/channel/"+cid), localization);
//        ChannelExtractor chExtractor = ys.getChannelExtractor(cid, null, "", localization);
        chExtractor.fetchPage();

        ListExtractor.InfoItemsPage<StreamInfoItem> items = chExtractor.getInitialPage();
        for(StreamInfoItem ii: items.getItems()){
            System.out.println(ii.getName() + "---" + ii.getInfoType().name() + " date:"+ ii.getUploadDate() + " dur:"+ii.getDuration());
            System.out.println( "   : " +ii.getUrl());
        }

        String nextPage = chExtractor.getNextPageUrl();
        System.out.println("---nextPageUrl: "+ nextPage);
        items = chExtractor.getPage(nextPage);
        for(StreamInfoItem ii: items.getItems()){
            System.out.println(ii.getName() + "---" + ii.getInfoType().name() + " date:"+ ii.getUploadDate() + " dur:"+ii.getDuration());
            System.out.println( "   : " +ii.getUrl());
        }
    }

    public static class FormatItem{
        public String url;
        public boolean isAudio;
        public String fmt;
        public String resolution;
        public FormatItem(String url, boolean isAudio){this.url=url; this.isAudio=isAudio;}
        public String toString() {
            return (isAudio?"A ":"V " )+fmt + "("+resolution+")";
        }
    }
    public static class Stream {
        public String url;
        public String name;
        public String desc;
        public String duration; //时长, 单位秒
        public List<FormatItem> items;
        public List<StreamInfoItem> relate; // 相关视频
        public Stream(String name, String url) {this.name=name; this.url=url; items= new LinkedList<>();}
        public String toString() {
            StringBuilder sb = new StringBuilder("\n").append(name);
            sb.append("\n-----\n")
                    .append(desc)
                    .append("\nformats:\n----");
            for(FormatItem fi: items)
                sb.append("\n  ").append(fi.toString());
            return sb.toString();
        }
    }

    //获取视频的详细信息
    public Stream getDetail(String url, String name, String duration) throws Exception {
        Stream s = new Stream(name, url);
        s.duration = duration;

        StreamExtractor streamExtractor = ys.getStreamExtractor(ys.getStreamLHFactory().fromUrl(url), localization);
        streamExtractor.fetchPage();
        s.name = streamExtractor.getName();
        s.desc = streamExtractor.getDescription();
        List<VideoStream> videoList = streamExtractor.getVideoStreams();
        for(VideoStream vs: videoList){
            FormatItem fi = new FormatItem(vs.getUrl(), false);
            fi.fmt = vs.getFormat().getName();
            fi.resolution = vs.getResolution();
            s.items.add(fi);
        }
        List<AudioStream> audioList = streamExtractor.getAudioStreams();
        for(AudioStream as : audioList){
            FormatItem fi = new FormatItem(as.getUrl(), true);
            fi.fmt = as.getFormat().getName();
            fi.resolution = as.getAverageBitrate() +"";
            s.items.add(fi);
        }

        s.relate = streamExtractor.getRelatedStreams().getStreamInfoItemList();
        for(StreamInfoItem sii: s.relate ) {
            System.out.println(sii.getName() + " " + sii.getDuration());
        }
        return s;
    }

    // 获取媒体文件的真实下载地址
    private static String test_getstreams(YoutubeService ys, String id,
                                          String fmt, String resolution) throws Exception {
        /**
         * 《奇葩说第5季》第24期 20181208 巅峰之夜BBKing荣耀诞生 神反转结局震惊全场
         *    : https://www.youtube.com/watch?v=CkTXJzjM6NU
         */
        String url = null;
        String itemUrl = "https://www.youtube.com/watch?v="+id;
        StreamExtractor streamExtractor = ys.getStreamExtractor(ys.getStreamLHFactory().fromUrl(itemUrl), localization);
        streamExtractor.fetchPage();

        String desc = streamExtractor.getDescription();
        System.out.println(streamExtractor.getName()+"\nDescription: "+desc);

        // 各个格式的url
        List<VideoStream> videoList = streamExtractor.getVideoStreams();
        for(VideoStream vs: videoList){
//            System.out.println("video: " + vs.getFormat().getName() + " " + vs.getResolution());
//            System.out.println(vs.getUrl());
            if(fmt.equals(vs.getFormat().getName()) && vs.getResolution().equals(resolution))
                url = vs.getUrl();
        }
        List<AudioStream> audioList = streamExtractor.getAudioStreams();
        for(AudioStream as : audioList){
//            System.out.println("audio: " + as.getFormat().getName() + " " + as.getAverageBitrate());
//            System.out.println(as.getUrl());
            if(fmt.equals(as.getFormat().name))
                url = as.getUrl();
        }
        System.out.println("stream url of "+fmt + " is "+url);
        return url;
    }

    private static void test_download(YoutubeService ys, String vid, String fmt) throws Exception {
        String streamurl = test_getstreams(ys, vid, fmt, "360p" );

        DownloadMission dm = new DownloadMission(vid + "." + fmt, streamurl, "d:/tmp/yt");
        dm.setProxy("127.0.0.1", 8083);
        dm.setTimeout(60000);
        dm.addListener(new DownloadMission.MissionListener(){
            public void onProgressUpdate(DownloadMission downloadMission, long done, long total) {
                System.out.printf("finish: %.2f %%\n", (done*100.0)/total);
            }
            public void onFinish(DownloadMission downloadMission) {
                System.out.println("Finished!");
            }
            public void onError(DownloadMission downloadMission, int errCode) {
                System.out.println("Failed:"+errCode);
            }
        });
        dm.start();

        while(! dm.isFinished() ){
            Thread.sleep(2000L);
        }
    }

    private static void test_readItems() throws Exception {
        InputStream fin = new FileInputStream("d:/tmp/search_wenzhao");
        while(true){
            InfoItem ii = Utils.readItem(fin);
            if(ii == null)
                break;
//            System.out.println(ii.getInfoType() + " " + ii.getName());
            System.out.println(Utils.toJson(ii));
        }
        fin.close();
    }

        // guanyf test
    public static void main(String[] args) throws Exception {
//        System.setProperty("socksProxyHost", "127.0.0.1"); // for the host name of the SOCKS proxy server
//        System.setProperty("socksProxyPort", "1083"); // "9150"); // for the port number, the default value being 1080

        YTDownloader downloader = YTDownloader.init(null, "127.0.0.1", 8083);

        NewPipe.init(downloader, localization);

        YoutubeService ys = new YoutubeService(0);

//        test_search(ys, "voachinese"); //"奇葩说");
//        test_download(ys, "J69tqfUKRzY", "m4a");
//        test_search(ys, "wenzhao");
        test_readItems();
//        test_channel(ys, "UCtAIPjABiQD3qjlEl1T5VpA"); //wenzhao

        //美国之音中文网---CHANNEL
        //   : https://www.youtube.com/user/VOAchina
//        test_download(ys, "SCPjaLh7yLc", "m4a");

    }
}
