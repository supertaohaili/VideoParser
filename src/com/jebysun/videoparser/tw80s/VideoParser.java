package com.jebysun.videoparser.tw80s;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.jebysun.videoparser.tw80s.model.DownloadInfo;
import com.jebysun.videoparser.tw80s.model.Video;
import com.jebysun.videoparser.tw80s.param.VideoType;
import com.jebysun.videoparser.tw80s.utils.Tw80sUtil;
import com.jebysun.videoparser.utils.JavaUtil;

/**
 * 80s视频资源解析
 * @author Jeby Sun
 * @Date 2015-12-24
 */
public class VideoParser {
	
	private VideoParser() {}
	
	/**
	 * 电影查询列表
	 * @param categroy - 类别，默认为全部
	 * @param area - 地区，默认为全部
	 * @param language - 语言，默认为全部
	 * @param year - 年份，默认为全部
	 * @param sort - 排序方式，默认按照最新视频排序
	 * @param pageIndex - 当前页索引
	 * @return
	 * @throws IOException
	 */
	public static List<Video> listMovie(String category, String area, String language, String year, String sort, int pageIndex) throws IOException {
		return listMovie(category, area, language, year, sort, "p" + pageIndex);
	}
	
	/**
	 * 电视剧查询列表
	 * @param category - 类别，默认为全部
	 * @param area - 地区，默认为全部
	 * @param year - 年份，默认为全部
	 * @param sort - 排序方式，默认按照最新视频排序
	 * @param pageIndex - 当前页索引
	 * @return
	 * @throws IOException
	 */
	public static List<Video> listTV(String category, String area, String year, String sort, int pageIndex) throws IOException {
		//地区默认全部需要设置地区参数为0
		area = area==null ? "0" : area;
		return listTV(category, area, year, sort, "p" + pageIndex);
	}
	
	/**
	 * 动漫查询列表
	 * @param type - 类型，默认全部
	 * @param sort - 排序方式，默认按照最新视频排序
	 * @param pageIndex - 当前页索引
	 * @return
	 * @throws IOException
	 */
	public static List<Video> listManga(String type, String sort, int pageIndex) throws IOException {
		return listManga(type, sort, "p" + pageIndex);
	}
	
	/**
	 * 综艺查询列表
	 * @param sort - 排序方式，默认按照最新视频排序
	 * @param pageIndex - 当前页索引
	 * @return
	 * @throws IOException
	 * @author Jeby Sun
	 */
	public static List<Video> listVariety(String sort, int pageIndex) throws IOException {
		return listVariety(sort, "p" + pageIndex);
	}
	

	/**
	 * 获取电影详情
	 * @param url 电影详情地址
	 * @return
	 * @throws IOException
	 */
	public static Video getVideoDetail(String url) throws IOException {
		Video v = new Video();
		
		Document doc = Jsoup.connect(url)
				.timeout(Config.TIMEOUT * 1000)
				.get();
		
		//电影详情地址
		v.setDetailUrl(url);

		//视频类型(根据视频详情地址路径判断类型)
		String subUrl = url.substring(url.indexOf("/", 7));
		v.setVideoType(Tw80sUtil.getVideoTypeByURL(subUrl));
		
		//电影海报
		Element imgElement = doc.select("div#minfo>div.img>img").get(0);
		String name = imgElement.attr("title");
		String imgPosterUrl = imgElement.attr("src");
		v.setName(name);
		
		if (imgPosterUrl !=null && !imgPosterUrl.startsWith("http")) {
			imgPosterUrl = "http:" + imgPosterUrl;
			if (imgPosterUrl.lastIndexOf(".")==imgPosterUrl.length()-4) {
				v.setPosterUrl(imgPosterUrl);
			}
		}
		
		//视频截图
		Elements screenShotH2Elmts = doc.select("h2#screenshots");
		if (!screenShotH2Elmts.isEmpty()) {
			Elements screenShotImgElmts = screenShotH2Elmts.get(0).parent().select(">img");
			if (!screenShotImgElmts.isEmpty()) {
				String screenShotUrl = screenShotImgElmts.get(0).attr("_src");
				if (screenShotUrl !=null && !screenShotUrl.startsWith("http")) {
					screenShotUrl = "http:" + screenShotUrl;
				}
				v.setScreenShotUrl(screenShotUrl);
			}
		}
		
		//备注
		if (v.getVideoType().equals(VideoType.ZY)) {
			String noteText = doc.select("div.info>span").get(0).ownText();
			v.setNote(noteText);
		} else {
			Element noteNode = doc.select("div.info>span").get(0);
			if (noteNode.children().size() == 0 && !JavaUtil.isEmptyString(noteNode.text())) {
				v.setNote(noteNode.text());
			}
			
		}
		
		
		//又名, 演员
		Elements eles = doc.select("div.info>span");
		for (Element e : eles) {
			String[] sArr = e.text().split("：", 2);
			if (sArr.length==2) {
				v = fillBaseInfo(v, sArr);
			}
		}
		
		//基本信息
		Element baseInfoNode = doc.select("div.info div.clearfix").get(0);
		Elements es = baseInfoNode.select(">span");
		for (Element e : es) {
			String[] sArr = e.text().split("：", 2);
			if (sArr.length==2) {
				v = fillBaseInfo(v, sArr);
			}
		}
		
		//影片评分
		Elements scoreNodes = doc.select("div.info span.score");
		if (scoreNodes.size() != 0) {
			Element scoreWrapNode = scoreNodes.get(0).parent();
			String scoreInfo = scoreWrapNode.text();
			if (scoreInfo.startsWith("豆瓣评分")) {
				v.setScore(scoreInfo.split("：")[1].trim());
			}
		}

		//豆瓣影视ID
		Elements commentNodes = doc.select("div.info span.textbg1");
		if (commentNodes.size() != 0) {
			Element commentWrapNode = commentNodes.get(0).parent();
			commentNodes = commentWrapNode.select("a");
			if (commentNodes.size() == 2) {
				String doubanId = Tw80sUtil.getDoubanIdFromCommentUrl(commentNodes.get(1).attr("href"));
				v.setDoubanMovieId(doubanId);
			}
		}
			
		//影片简介
		Elements storyNodes = doc.select("#movie_content_all");
		Element storyNode = storyNodes.size()==1 ? storyNodes.get(0) : null;
		if (storyNode == null) {
			storyNode = doc.select("div#movie_content").get(0);
		}
		//移除“剧情介绍”标题
		storyNode.select("span").get(0).remove();
		String story = storyNode.text().replaceAll("　", "").trim();
		v.setStory(story);
		
		//下载地址
		Elements tNodes = doc.select("ul.dllist1>li:not(.nohover)>div");
		//收起的下载地址，比如电视剧，动漫，综艺的剧集下载地址可能很多
		if (tNodes.size() != 0) {
//			String downloadPageUrl = getDownloadPageUrl(url, tNodes.get(0));
//			Map<String, String> downloadMap = getAllDownloadUrl(downloadPageUrl);
//			v.setDownloadMap(downloadMap);
		} else {
			v.setDownloadInfoList(parseDownloadUrl(doc));
		}

		return v;
	}
	
	/**
	 * 视频搜索
	 * @param keyword - 搜索关键字
	 * @return - 搜索结果
	 * @throws IOException
	 */
	public static List<Video> searchVideo(String keyword) throws IOException {
		String searchUrl = Config.DOMAIN + Config.VIDEO_SEARCH_PATH;
		
		Document doc = Jsoup.connect(searchUrl)
				.timeout(Config.TIMEOUT * 1000)
				.data("keyword", keyword)
				.post();
		
		return parserSearchVideoList(doc);
	}

	
	
	
	
	
	
	
	
	/**
	 * 电影分类列表
	 * @param categroy - 类别，默认为全部
	 * @param area - 地区，默认为全部
	 * @param language - 语言，默认为全部
	 * @param year - 年份，默认为全部
	 * @param sort - 排序方式，默认按照最新视频排序
	 * @param pageIndex - 当前页索引，默认为第一页(p1)
	 * @return
	 * @throws IOException
	 */
	private static List<Video> listMovie(String category, String area, String language, String year, String sort, String pageIndex) throws IOException {
		Map<String, String> param = new HashMap<String, String>();
		param.put("category", category);
		param.put("area", area);
		param.put("language", language);
		param.put("year", year);
		param.put("sort", sort);
		param.put("pageIndex", pageIndex);
		String queryUrl = buildQueryUrl(Config.MOVIE_QUERY_PATH, param);
		String url = Config.DOMAIN + queryUrl;
		//获取文档
		Document doc = getDocument(url, Config.TIMEOUT, 0);
		//解析视频列表
		return parserSimpleVideoList(doc);
	}
	
	/**
	 * 电视剧查询列表
	 * @param categroy - 类别，默认为全部
	 * @param area - 地区，默认为全部
	 * @param year - 年份，默认为全部
	 * @param sort - 排序方式，默认按照最新视频排序
	 * @param pageIndex - 当前页索引，默认为第一页(p1)
	 * @return
	 * @throws IOException
	 */
	private static List<Video> listTV(String category, String area, String year, String sort, String pageIndex) throws IOException {
		Map<String, String> param = new HashMap<String, String>();
		param.put("category", category);
		param.put("area", area);
		param.put("year", year);
		param.put("sort", sort);
		param.put("pageIndex", pageIndex);
		String queryUrl = buildQueryUrl(Config.TV_QUERY_PATH, param);
		String url = Config.DOMAIN + queryUrl;
		System.out.println(url);
		Document doc = getDocument(url, Config.TIMEOUT, 0);
		return parserSimpleVideoList(doc);
	}
	
	/**
	 * 查询动漫列表
	 * @param type - 动漫类型（连载，剧场），默认为全部
	 * @param sort - 排序方式，默认按照最新视频排序
	 * @param pageIndex - 当前页索引，默认为第一页(p1)
	 * @return
	 * @throws IOException
	 * @author Jeby Sun
	 */
	private static List<Video> listManga(String type, String sort, String pageIndex) throws IOException {
		Map<String, String> param = new HashMap<String, String>();
		param.put("type", type);
		param.put("sort", sort);
		param.put("pageIndex", pageIndex);
		String queryUrl = buildQueryUrl(Config.MANGA_QUERY_PATH, param);
		String url = Config.DOMAIN + queryUrl;
		Document doc = getDocument(url, Config.TIMEOUT, 0);
		return parserSimpleVideoList(doc);
	}
	
	/**
	 * 查询综艺列表
	 * @param sort
	 * @param pageIndex
	 * @return
	 * @throws IOException
	 * @author Jeby Sun
	 */
	private static List<Video> listVariety(String sort, String pageIndex) throws IOException {
		Map<String, String> param = new HashMap<String, String>();
		param.put("sort", sort);
		param.put("pageIndex", pageIndex);
		String queryUrl = buildQueryUrl(Config.VARIETY_QUERY_PATH, param);
		String url = Config.DOMAIN + queryUrl;
		
		Document doc = getDocument(url, Config.TIMEOUT, 0);
		
		List<Video> videos = new ArrayList<Video>();
		Video v = null;
		Elements elements = doc.select("ul.zy>li");
		for (Element e : elements) {
			v = new Video();
			String name = e.select(">div>a").get(0).attr("title");
			String note = e.select("div.zyr a span").get(0).text();
			String path = e.select(">div>a").get(0).attr("href");
			String imgUrl = e.select(">div>a>img").get(0).attr("_src");
			if (imgUrl.lastIndexOf(".")==imgUrl.length()-4) {
				v.setPosterUrl(imgUrl);
			}
			v.setName(name);
			v.setNote(note.substring(1, note.length()-1));
			v.setDetailUrl(Config.DOMAIN+path);
			v.setVideoType(Tw80sUtil.getVideoTypeByURL(path));
			videos.add(v);
		}
		return videos;
	}
	
	/**
	 * 构建查询路径
	 * @param queryUrl
	 * @param param
	 * @return
	 * @author Jeby Sun
	 */
	private static String buildQueryUrl(String queryUrl, Map<String, String> param) {
		queryUrl = paramReplace(queryUrl, "\\$category", param.get("category"));
		queryUrl = paramReplace(queryUrl, "\\$area", param.get("area"));
		queryUrl = paramReplace(queryUrl, "\\$language", param.get("language"));
		queryUrl = paramReplace(queryUrl, "\\$year", param.get("year"));
		queryUrl = paramReplace(queryUrl, "\\$type", param.get("type"));
		queryUrl = paramReplace(queryUrl, "\\$sort", param.get("sort"));
		queryUrl = paramReplace(queryUrl, "\\$page", param.get("pageIndex"));
		return queryUrl;
	}
	
	/**
	 * 字符串非空替换
	 * @param url
	 * @param pname
	 * @param pvalue
	 * @return
	 * @author Jeby Sun
	 */
	private static String paramReplace(String url, String pname, String pvalue) {
		pvalue = (pvalue==null) ? "" : pvalue;
		return url.replaceFirst(pname, pvalue);
	}
	
	
	/**
	 * 填充电影基本信息
	 * @param v
	 * @param sArr
	 * @return
	 */
	private static Video fillBaseInfo(Video v, String[] sArr) {
		String key = sArr[0].trim();
		String value = sArr[1].trim();
		switch(key) {
		case "又名":
			v.setAlias(value);
			break;
		case "演员":
			v.setActor(value);
			break;
		case "类型":
			v.setCategory(value);
			break;
		case "地区":
			v.setArea(value);
			break;
		case "语言":
			v.setLanguage(value);
			break;
		case "导演":
			v.setDirector(value);
			break;
		case "上映日期":
			v.setReleaseDate(value);
			break;
		case "片长":
			v.setDuration(value);
			break;
		}
		return v;
	}
	
	/**
	 * 解析下载地址
	 * @param doc
	 * @return
	 */
	private static List<DownloadInfo> parseDownloadUrl(Document doc) {
		List<DownloadInfo> downloadInfoList = new ArrayList<DownloadInfo>();
		Elements urlNodes = doc.select("ul.dllist1>li:not(.nohover)");
		DownloadInfo downloadInfo = null;
		//下载地址序列反转，网页上的下载地址最近更新的排在前面，这里我们从最后面开始解析
		for (int i=urlNodes.size()-1; i>=0; i--) {
			downloadInfo = new DownloadInfo();
			
			Element fileSizeNode = urlNodes.get(i).select("span.nm>span").get(0);
			Element downloadLinkNode = fileSizeNode.select("a").get(0);
			
			String downloadFileSize = fileSizeNode.ownText();
			//去除不可见字符（此空白字符串从源拷贝而来）
			downloadFileSize = downloadFileSize.replaceAll("      ", "");
			String downloadTitle = downloadLinkNode.ownText();
			String downloadUrl = downloadLinkNode.attr("href");
			
//			System.out.println("标题："+ downloadTitle);
//			System.out.println("文件大小："+ downloadUrl);
//			System.out.println("下载地址："+ downloadFileSize);
			
			downloadInfo.setTitle(downloadTitle);
			downloadInfo.setFileSize(downloadFileSize);
			downloadInfo.setDownloadUrl(downloadUrl);
			downloadInfoList.add(downloadInfo);
		}
		return downloadInfoList;
	}
	
	/**
	 * 获取完整下载地址
	 * @param downloadPageUrl
	 * @return
	 * @throws IOException
	 */
//	private static Map<String, String> getAllDownloadUrl(String downloadPageUrl) throws IOException {
//		
//		Document doc = Jsoup.connect(downloadPageUrl)
//				.timeout(Config.TIMEOUT * 1000)
//				.maxBodySize(1024*1024*2) //2M 设置网页内容最大容量
//				.get();
//		
//		return parseDownloadUrl(doc);
//	}
	
	/**
	 * 通过Jsoup获取html文本文档
	 * @param url - 文档地址
	 * @param timeout - 超时时间，单位秒
	 * @param maxBodySize - 最大文档容量，单位Byte
	 * @return - HTML文档
	 * @throws IOException
	 * @author Jeby Sun
	 */
	private static Document getDocument(String url, int timeout, int maxBodySize) throws IOException {
		Connection conn = Jsoup.connect(url);
		if (timeout>0) {
			conn = conn.timeout(timeout * 1000);
		}
		if (maxBodySize>0) {
			conn = conn.maxBodySize(maxBodySize);
		}
		return conn.get();
	}
	
	/**
	 * 解析根据关键字搜索到的视频信息列表。
	 * @param doc - 要解析的HTML文档
	 * @return - 解析结果，List<Video>集合
	 * @author Jeby Sun
	 */
	private static List<Video> parserSearchVideoList(Document doc) {
		List<Video> videos = new ArrayList<Video>();
		//判断是不是没有搜索结果
		Elements isEmptyResultElemts = doc.select("#block3 div.nomoviesinfo");
		if (isEmptyResultElemts.size() != 0) {
			return videos;
		}
		
		Elements elements = doc.select("#block3 ul.search_list>li");
		Video v = null;
		for (Element e : elements) {
			v = new Video();
			Element nameEle = e.select("a").get(0);
			v.setName(nameEle.text());
			v.setDetailUrl(Config.DOMAIN + nameEle.attr("href"));
			v.setVideoType(Tw80sUtil.getVideoTypeByTitle(nameEle.text()));
			if (v.getVideoType().equals(VideoType.OTHER)) {
				continue;
			}
			v.setAlias(e.ownText());
			Elements ems = e.select("em");
			if (ems.size() == 2) {
				v.setScore(ems.get(0).text().replace("豆瓣", "").replace("分", ""));
				v.setNote(ems.get(1).text());
			} else if (ems.size() == 1) {
				String s = ems.get(0).text();
				if (s.indexOf("豆瓣") == 0) {
					v.setScore(s.replace("豆瓣", "").replace("分", ""));
				} else {
					v.setNote(s);
				}
			}
			videos.add(v);
		}
		return videos;
	}
	
	/**
	 * 解析简单的视频列表，即视频列表需要的信息。
	 * @param doc - 要解析的HTML文档
	 * @return - 解析结果，List<Video>集合
	 * @author Jeby Sun
	 */
	private static List<Video> parserSimpleVideoList(Document doc) {
		List<Video> videos = new ArrayList<Video>();
		//判断是不是没有搜索结果
		Elements isEmptyResultElemts = doc.select("#block3 div.nomoviesinfo");
		if (isEmptyResultElemts.size() != 0) {
			return videos;
		}
		
		Video v = null;
		Elements elements = doc.select("ul.me1>li");
		for (Element e : elements) {
			v = new Video();
			
			Elements scoreNodes = e.select("a>span.poster_score");
			Elements noteNodes = e.select("span.tip");
			Elements imageNodes = e.select("a>img");
			if (scoreNodes.size() != 0) {
				v.setScore(scoreNodes.get(0).text());
			}
			if (noteNodes.size() != 0) {
				v.setNote(noteNodes.get(0).text());
			}
			if (imageNodes.size() != 0) {
				//e.g: //t.dyxz.la/upload/img/201612/poster_20161213_2146695_b.jpg!list
				String imgPath = imageNodes.get(0).attr("_src");
				imgPath = imgPath.lastIndexOf("!")==(imgPath.length()-5) ? imgPath.substring(0, imgPath.lastIndexOf("!")) : null;
				if (imgPath !=null && !imgPath.startsWith("http")) {
					imgPath = "http:" + imgPath;
				}
				v.setPosterUrl(imgPath);
			}
			
			String name = e.select("a>img").get(0).attr("alt");
			String path = e.select("a").get(0).attr("href");
			v.setName(name);
			v.setDetailUrl(Config.DOMAIN + path);
			v.setVideoType(Tw80sUtil.getVideoTypeByURL(path));
			if (v.getVideoType().equals(VideoType.OTHER)) {
				continue;
			}
			videos.add(v);
		}
		return videos;
	}
	
	/**
	 * 获取下载地址页URL
	 * @param url
	 * @param e
	 * @return
	 */
	private static String getDownloadPageUrl(String url, Element e) {
		//解析获取全部下载地址的URL
		String methodStr = e.select("span").get(0).attr("onclick");
		String levelStr =  methodStr.split("'")[1];
		return url + "/" + levelStr+"-2";
	}
	
	
	
	
	
}









