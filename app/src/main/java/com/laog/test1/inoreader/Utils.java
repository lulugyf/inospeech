package com.laog.test1.inoreader;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.text.SimpleDateFormat;

public class Utils {
	public static String fileToString(String fname) throws Exception {
		return fileToString(fname, "UTF-8");
	}
	public static String fileToString(String fname, String charset) throws Exception {
		InputStream instream = new FileInputStream(fname);
		if(charset == null)
			charset = "UTF-8";
		final Reader reader = new InputStreamReader(instream, charset);
        final CharArrayWriter buffer = new CharArrayWriter(1024);
        final char[] tmp = new char[1024];
        int l;
        while((l = reader.read(tmp)) != -1) {
            buffer.write(tmp, 0, l);
        }
        return buffer.toString();
	}

	public static void stringToFile(String fname, String content) throws Exception {
		stringToFile(fname, content, "UTF-8");
	}
	public static void stringToFile(String fname, String content, String charset) throws Exception {
		final OutputStream outstream = new FileOutputStream(fname);
		try {
			if(charset == null)
				charset = "UTF-8";
			final Writer writer = new OutputStreamWriter(outstream, charset);
			writer.append(content);
			writer.close();
		}finally {
			outstream.close();
		}
	}

	public static long saveContent(String fname, String content) throws Exception {
		long fpos = 0L;
		File f = new File(fname);
		if(!f.exists())
			fpos = 0L;
		else
			fpos = f.length();

		final byte[] bytes = content.getBytes("UTF-8");
		final OutputStream outstream = new FileOutputStream(f, true);
		try {
			outstream.write(String.format("% 8d", bytes.length).getBytes());
			outstream.write(bytes);
		}finally {
			outstream.close();
		}
		return fpos;
	}

	public static String loadContent(String fname, long fpos) throws Exception {
        File f = new File(fname);
        if(!f.exists())
            return null;
        final InputStream instream = new FileInputStream(f);
        try{
            if(fpos > 0L)
                instream.skip(fpos);
            byte[] bytes = new byte[8];
            int r = instream.read(bytes);
            if(r != bytes.length) {
                log("file size not enough");
                return null;
            }
            int len = Integer.parseInt(new String(bytes).trim());
            bytes = new byte[len];
            r = instream.read(bytes);
            if(r != bytes.length) {
                log("file size not enough[content]");
                return null;
            }
            return new String(bytes, "UTF-8");
        }finally {
            instream.close();
        }
    }
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

	private final static String[] tailTags = new String[] {"镜像链接：",  "© "}; //"相关阅读：",
	public final static void _text(Node node, StringBuilder sb) {
		if("#text".equals(node.getNodeName())){
			String val = node.getNodeValue().trim();
			if(val.length() > 0)
				sb.append(val).append('\n');
		}
		Node child = node.getFirstChild();
		while (child != null) {
			_text(child, sb);
			child = child.getNextSibling();
		}
	}
	public final static String extractTextFromHtml(String html) throws Exception {
		InputSource src = new InputSource(new StringReader(html));
		DOMParser parser = new DOMParser();

		parser.parse(src);

		StringBuilder sb = new StringBuilder();
		Document root = parser.getDocument();
		_text(root, sb);
		String cc = sb.toString();
		for(String tag: tailTags){
			int p = cc.indexOf(tag);
			if(p > 0) cc = cc.substring(0, p);
		}
		return cc;
	}

	private static final SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd HH:mm:SS");
	public final static String timeToStr(long tm) {
		return sdf.format(new java.util.Date(tm));
	}

	public final static boolean copy(String src, String dst) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(src);
			out = new FileOutputStream(dst);
			byte[] buf = new byte[4096];
			while (true) {
				int i = in.read(buf);
				if (i <= 0)
					break;
				out.write(buf, 0, i);
			}
		}finally {
			try{ if(in != null) in.close(); }catch (Exception e){}
			try{ if(out != null) out.close(); }catch (Exception e){}
		}
		return true;
	}
}
