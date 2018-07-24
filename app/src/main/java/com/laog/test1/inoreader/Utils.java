package com.laog.test1.inoreader;

import java.io.CharArrayWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public class Utils {
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
	
	public static void stringToFile(String fname, String content, String charset) throws Exception {
		if(!fname.startsWith("tmp/"))
			fname = "tmp/"+fname;
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
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
}
