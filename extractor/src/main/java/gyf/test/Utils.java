package gyf.test;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class Utils {

    private static ObjectMapper mapper = null;
    static {
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        jsonFactory.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        mapper = new ObjectMapper(jsonFactory);
    }

    public static boolean isEmpty(String n){
        if(n == null || n.length() == 0)
            return true;
        return false;
    }

    public static String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object fromJson(String json, Class clz) {
        try {
            return mapper.readValue(json, clz);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void intToByte(int i, byte[] result, int offset) {
        int j = offset;
        result[j++] = (byte)((i >> 24) & 0xFF);
        result[j++] = (byte)((i >> 16) & 0xFF);
        result[j++] = (byte)((i >> 8) & 0xFF);
        result[j++] = (byte)(i & 0xFF);
    }

    public static int byteToInt(byte[] b, int offset) {
        int value= 0;
        for (int i = 0; i < 4; i++) {
            int shift= (4 - 1 - i) * 8;
            value +=(b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }

    public static int writeItem(InfoItem o, OutputStream out){
        try {
            byte[] b = mapper.writeValueAsBytes(o);
            byte[] bhead = new byte[8];
            intToByte(b.length +1, bhead, 4);
            switch (o.getInfoType()){
                case CHANNEL:
                    bhead[0] = (byte)2;
                    break;
                case STREAM:
                    bhead[0] = (byte)1;
                    break;
                case PLAYLIST:
                    return -1;
                default:
                    return -1;
            }
            out.write(bhead);
            out.write(b);
            out.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }
    public static InfoItem readItem(InputStream in) {
        try{
            byte[] bhead = new byte[8];
            int r = in.read(bhead);
            if(r != bhead.length)
                return null;
            int len = byteToInt(bhead, 4);
            int type = bhead[0];
            if(len > 8192)
                return null;
            byte[] b = new byte[len];
            r = in.read(b);
            if(r < b.length)
                return null;
            if(type == 1){
                return mapper.readValue(b, StreamInfoItem.class);
            }else if(type == 2) {
                return mapper.readValue(b, ChannelInfoItem.class);
            }else{
                byte[] b1 = new byte[10];
                in.read(b1);
                System.out.println(new String(b1));
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String readStr(String fpath) {
        try{
            File f = new File(fpath);
            if(!f.exists())
                return null;
            byte[] b = new byte[(int)f.length()];
            InputStream fin = new FileInputStream(f);
            fin.read(b);
            fin.close();
            return new String(b, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void writeStr(String str, String fpath) {
        try{
            byte[] b = str.getBytes("UTF-8");
            OutputStream fout = new FileOutputStream(fpath);
            fout.write(b);
            fout.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static  String dura(long dura){
        long h = dura / 60 / 60;
        long m = dura / 60 - h * 60;
        long s = dura % 60;
        if(h > 0)
            return String.format("%02d:%02d:%02d", h, m, s);
        else
            return String.format("%02d:%02d", m, s);
    }
}
