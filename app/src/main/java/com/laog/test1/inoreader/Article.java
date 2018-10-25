package com.laog.test1.inoreader;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Article implements Comparable<Article>{

	public String title;
	public String content;
	public String source;
	public String href;
	public String id;
	public String author;
	public String keys;
	public long published;
	private long fid;

	public Article(String content, String id) throws Exception {
		source = null;
		this.id = id;
		this.fid = Long.parseLong(id);
		InputSource src = new InputSource(new StringReader(content));
    	DOMParser parser = new DOMParser();
    	parser.parse(src);
    	
    	Document root = parser.getDocument();
    	NodeList nl = root.getElementsByTagName("A");
    	if(nl.getLength() > 0) {
    		Node n = nl.item(0);
    		title = n.getTextContent();
    		href = n.getAttributes().getNamedItem("href").getNodeValue();
    	}else {
    		title = null;
    		href = null;
    	}

        StringBuilder sb_author = new StringBuilder();
		StringBuilder sb_cont = new StringBuilder();
        extractText(root, sb_author, sb_cont);

		this.author = sb_author.toString();
		String cc = sb_cont.toString();
		int p = cc.indexOf("关键词:");
		if(p > 0) {
			keys = cc.substring(p);
			keys = keys.replace('\n', ' ');
			cc = cc.substring(0, p);
		}
		p = cc.indexOf("来源:");
		if(p >= 0){
			int p1 = cc.indexOf('\n', p+5);
			if(p1 > 0) {
				source = cc.substring(0, p1 + 1).replace('\n', ' ');
				cc = cc.substring(p1+1);
			}
		}
		for(String tag: tailTags){
			p = cc.indexOf(tag);
			if(p > 0) cc = cc.substring(0, p);
		}

        this.content = cc;
	}

	private final static String[] tailTags = new String[] {"镜像链接：", "相关阅读：", "© "};

	private boolean inAuthor = false;
	private boolean inContent = false;
	private void extractText(Node node, StringBuilder sb_author, StringBuilder sb_cont) {
		final String nname = node.getNodeName();
		final String nclass = nodeClass(node);
		if("A".equals(nname) && "bluelink".equals(nclass)){
			href = node.getAttributes().getNamedItem("href").getNodeValue();
			title = node.getTextContent();
		}else if("DIV".equals(nname) && "article_author".equals(nclass)){
			inAuthor = true; inContent = false;
		}else if("DIV".equals(nname) && "article_content".equals(nclass)){
			inAuthor = false; inContent = true;
		}else if("#text".equals(nname) ) {
			String val = node.getNodeValue().trim();
			if(val.length() > 0) {
				if (inAuthor) {
					sb_author.append(val).append(' ');
				}
				if (inContent) {
					sb_cont.append(val).append('\n');
				}
			}
		}

		Node child = node.getFirstChild();
        while (child != null) {
        	extractText(child, sb_author, sb_cont);
            child = child.getNextSibling();
        }
	}
	private String nodeClass(Node n) {
		NamedNodeMap nn = n.getAttributes();
		if(nn == null)
			return null;
		Node n1 = nn.getNamedItem("class");
		if(n1 == null)
			return null;
		return n1.getNodeValue();
	}
	
    public static void main(String[] argv) throws Exception {
    	String content = Utils.fileToString("/tmp/tmp/articles/16639945633.html", null);
    	Article a = new Article(content, "1");

    	Utils.log(a.title);
    	Utils.log(a.href);
    	Utils.log(a.author);
		Utils.log(a.source);
		Utils.log(a.keys);
    	Utils.log(a.content);
    	
    	print_html(content);
    }
    public static void print_html(String content) throws Exception {
//    	String content = "<div dir=\"ltr\"><a style=\"text-decoration:none;font-weight:bold;font-size:1.1em;\" class=\"bluelink\" target=\"_blank\" rel=\"noopener\" href=\"https://botanwang.com/articles/201807/%E4%BB%A5%E8%89%B2%E5%88%97%E9%80%9A%E8%BF%87%E6%B0%91%E6%97%8F%E5%9B%BD%E5%AE%B6%E6%B3%95%E5%94%AF%E7%8A%B9%E5%A4%AA%E4%BA%BA%E5%8F%AF%E4%BA%AB%E8%87%AA%E6%B2%BB%E6%9D%83.html\">以色列通过民族国家法唯犹太人可享自治权</a> <div class=\"article_author\">发表于 12:43 由  <span style=\"font-style:italic\">daying</span> 通过 <a class=\"bluelink boldlink\" style=\"font-style:normal !important;text-decoration:none !important;\" href=\"?list_articles=1&filter_type=subscription&filter_id=22541990\">博谈网</a> </div></div><div id=\"article_contents_inner_16573942101\" class=\"article_content\"><div><div>来源:?</div><div><div>美国之音</div></div></div><div><div><div><p>以色列议会星期四通过一项有高度争议的法律。根据新通过的法律，只有以色列的犹太人才享有自治权，并鼓励建立犹太人定居点。</p> ";
//    	String content = Utils.fileToString("v.js", null);

    	Utils.log("-------print_html parser:");
		InputSource src = new InputSource(new StringReader(content));
    	DOMParser parser = new DOMParser();
    	
    	parser.parse(src);
    	
    	Document root = parser.getDocument();
//    	NodeList nl = root.getElementsByTagName("A");
//    	for(int i=0; i<nl.getLength(); i++) {
//    		Node n = nl.item(i);
//    		Utils.log(n.getNodeName() + " " + n.getTextContent() + " " + n.getAttributes().getNamedItem("href").getNodeValue());
//    	}
        print(parser.getDocument(), "  ");

    }
    private static String attrs(Node n) {
		NamedNodeMap nn = n.getAttributes();
		if(nn == null)
			return "[no attrs]";
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<nn.getLength(); i++){
			Node attr = nn.item(i);
			sb.append(attr.getNodeName()).append('=');
			sb.append(attr.getNodeValue()).append(';');
		}
		return sb.toString();
	}
    private static void print(Node node, String indent) {
        System.out.println(indent+node.getNodeName() + " " + node.getNodeValue() + " " +attrs(node));
        Node child = node.getFirstChild();
        while (child != null) {
            print(child, indent+" ");
            child = child.getNextSibling();
        }
    }

    private static void parseImage(Node node, List<String> imgs) {
		if("IMG".equals(node.getNodeName())){
			NamedNodeMap nn = node.getAttributes();
			if(nn != null){
				Node att = nn.getNamedItem("src");
				if(att != null)
					imgs.add(att.getNodeValue());
			}
			return;
		}
		Node child = node.getFirstChild();
		while (child != null) {
			parseImage(child, imgs);
			child = child.getNextSibling();
		}
	}
    public static List<String> parseImageLinks(String content) throws Exception {
		InputSource src = new InputSource(new StringReader(content));
		DOMParser parser = new DOMParser();
		parser.parse(src);
		List<String> ret = new LinkedList<>();
		parseImage(parser.getDocument(), ret);
		return ret;
	}

	@Override
	public int compareTo(Article o) {
		if(o.fid > fid)
			return 1;
		else if(o.fid < fid)
			return -1;
		return 0;
	}
}
