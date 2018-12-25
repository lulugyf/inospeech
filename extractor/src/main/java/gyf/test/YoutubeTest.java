package gyf.test;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.channel.ChannelInfoItemExtractor;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.services.youtube.YoutubeService;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.extractor.utils.Localization;

import java.util.LinkedList;
import java.util.List;

public class YoutubeTest {

    static Localization localization = new Localization("CN", "zh");

    // 关键字 搜索测试
    private static void test_search(YoutubeService ys, String str) throws Exception {
        SearchQueryHandlerFactory searchHF = ys.getSearchQHFactory();
        String searchStr = searchHF.getUrl(str, new LinkedList<String>(), null);
        System.out.println(searchStr);
//        searchStr = searchStr + "&page=0";

        SearchExtractor searchExtractor = ys.getSearchExtractor(searchHF.fromQuery(searchStr));

        System.out.println("--- search result:");
        ListExtractor.InfoItemsPage<InfoItem> items = searchExtractor.getPage(searchStr+ "&page=0");
        for(InfoItem ii: items.getItems()){
            if(ii instanceof StreamInfoItem) {
                StreamInfoItem i1 = (StreamInfoItem)ii;
                System.out.println(ii.getName() + "---" + ii.getInfoType().name() + " date:" + i1.getUploadDate() + " dur:" + i1.getDuration());
            }else
                System.out.println(ii.getName() + "---" + ii.getInfoType().name());
            System.out.println( "   : " +ii.getUrl());

        }

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
//        test_channel(ys, "UCtAIPjABiQD3qjlEl1T5VpA"); //wenzhao

        //美国之音中文网---CHANNEL
        //   : https://www.youtube.com/user/VOAchina
        test_download(ys, "SCPjaLh7yLc", "m4a");

    }
}
