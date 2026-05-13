package com.phy.starpicture.manager;

import cn.hutool.core.util.StrUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bing 图片抓取器
 * 使用 Jsoup 解析 Bing 图片搜索结果页面，提取图片 URL 和名称。
 */
@Component
public class BingImageFetcher {

    /** Bing 图片搜索地址 */
    private static final String BING_SEARCH_URL = "https://cn.bing.com/images/search";

    /** 请求 User-Agent（模拟浏览器，避免被拦截） */
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/120.0.0.0 Safari/537.36";

    /**
     * 提取的图片信息
     */
    public static class BingImage {
        /** 图片标题/名称 */
        public String name;
        /** 图片源 URL */
        public String imageUrl;
    }

    /**
     * 从 Bing 图片搜索结果中提取图片列表
     *
     * @param keyword 搜索关键词
     * @param count   需要获取的数量（实际能拿到的不超过页面返回数量）
     * @return 提取到的图片 URL 列表
     */
    public List<BingImage> fetchImages(String keyword, int count) {
        List<BingImage> result = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        try {
            int offset = 0;
            // 最多尝试获取 2 页
            while (result.size() < count && offset < 60) {
                String url = BING_SEARCH_URL + "?q=" + URLEncoder.encode(keyword, "UTF-8")
                        + "&first=" + offset + "&tsc=ImageHoverTitle";
                Document doc = Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .timeout(15000)
                        .get();

                // Bing 图片结果存储在 <a> 标签的 m 属性中（JSON 格式，含 murl 字段）
                Elements links = doc.select("a[href][m]");
                for (Element link : links) {
                    if (result.size() >= count) break;

                    String mAttr = link.attr("m");
                    // 从 m 属性 JSON 中提取 murl（图片原始 URL）
                    String imageUrl = extractJsonValue(mAttr, "murl");
                    String title = extractJsonValue(mAttr, "t");

                    if (StrUtil.isBlank(imageUrl) || seenUrls.contains(imageUrl)) continue;

                    seenUrls.add(imageUrl);

                    BingImage img = new BingImage();
                    img.imageUrl = imageUrl;
                    img.name = StrUtil.isNotBlank(title) ? cleanTitle(title) : (keyword + " " + (result.size() + 1));
                    result.add(img);
                }

                // 如果本页没提取到任何图片则退出
                if (links.isEmpty()) break;
                offset += links.size();
            }
        } catch (Exception e) {
            throw new RuntimeException("Bing 图片抓取失败: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 从 JSON 字符串中简单提取指定 key 的字符串值
     * 不依赖 JSON 库，使用简单的字符串匹配（key":"...value..."）
     */
    private String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":\"";
            int start = json.indexOf(searchKey);
            if (start < 0) return null;
            start += searchKey.length();
            int end = json.indexOf("\"", start);
            if (end < 0) return null;
            return json.substring(start, end)
                    .replace("\\u002F", "/")
                    .replace("\\u0026", "&")
                    .replace("\\u002C", ",");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 清理标题中的 HTML 标签和特殊字符
     */
    private String cleanTitle(String title) {
        return title.replaceAll("<[^>]+>", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
