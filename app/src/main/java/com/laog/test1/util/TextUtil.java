package com.laog.test1.util;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {
    static final class Pair<T, K> {
        T _1;
        K _2;
        public Pair(T _1, K _2) {this._1 = _1; this._2 = _2; }
        public T _1() { return _1;}
        public K _2() { return _2; }
    }
    public final static List<Pair<String, String>> parsePair(String content , String pattern) {
        //String pattern = " href=\\\"(\\?M=book&P=[\\d\\w]+)\\\" *>([^<]+)</a>";
        Pattern r = Pattern.compile(pattern);
        Matcher matcher = r.matcher(content);
        LinkedList<Pair<String, String>> ret = new LinkedList<>();
        while(matcher.find()){
            String group = matcher.group();
//            System.out.println(matcher.group(1) + "  = " + matcher.group(2));
            ret.add(new Pair<String, String>(matcher.group(1) , matcher.group(2)));
        }
        return ret;
    }

    public static void main(String[] args) throws Exception {
        String line = "<font color=\"CC0000\">茂呂美耶</font>\n" +
                "<a class=s href=\"?M=book&P=15E2\">【物語日本】</a><a class=s href=\"?M=book&P=15E9\">【江戶日本】</a><br>\n" +
                "<a class=s href=\"?M=book&P=15I2\">【平安日本】</a><a class=s href=\"?M=book&P=15K7\">【傳說日本】</a><a class=s href=\"?M=book&P=15M5\">【戰國日本】</a><br>\n" +
                "<a class=s href=\"?M=book&P=15R4\">【戰國日本Ⅱ】</a><a class=s href=\"?M=book&P=15Y1\">【明治日本】</a><br>\n" +
                "Ｍiya字解日本<a class=s href=\"?M=book&P=15R5\">【食衣住遊】</a><a class=s href=\"?M=book&P=15N8\">【鄉土料理】</a><br>\n" +
                "<a class=s href=\"?M=book&P=15N7\">【十二歲時】</a><br>\n" +
                "<hr class=s>\n" +
                "<font color=\"CC0000\">Ruth Benedict</font>\n" +
                "<a class=s href=\"?M=book&P=14H2\">【菊花與劍】</a><br>\n" +
                "<font color=\"CC0000\">山本常朝</font><a class=s href=\"?M=book&P=14BB3\">【葉隱聞書】</a>\n" +
                "<br>\n" +
                "<font color=\"CC0000\">吉川英治</font><a class=s href=\"?M=book&P=13J1\">【源賴朝】</a>\n" +
                "<br>\n" +
                "<font color=\"CC0000\">新田次郎</font><a class=s href=\"?M=book&P=13P1\">【武田信玄】</a>\n" +
                "<br>\n" +
                "<font color=\"CC0000\">長部日出雄</font><a class=s href=\"?M=book&P=1547\">【津輕風雲錄】</a>\n" +
                "<br>\n";
        String pattern = " href=\\\"(\\?M=book&P=[\\d\\w]+)\\\" *>([^<]+)</a>";
        Pattern r = Pattern.compile(pattern);
        Matcher matcher = r.matcher(line);
        while(matcher.find()){
            String group = matcher.group();
            System.out.println(matcher.group(1) + "  = " + matcher.group(2));
        }


    }
}
