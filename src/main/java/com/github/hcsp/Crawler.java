package com.github.hcsp;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.stream.Collectors;


public class Crawler extends Thread {
    CrawlerDao Dao;

    public Crawler(CrawlerDao dao) {
        Dao = dao;
    }

    @Override
    public void run() {
        try {
            String link;
            while ((link = Dao.getNextLinkThenDelete()) != null) {
                if (Dao.isLinkProcessed(link)) {
                    continue;
                }
                if (isInterestingLink(link)) {
                    Document doc = httpGetAndParseHtml(link);
                    parseUrlsFromPageAndStoreIntoDatabase(doc);
                    storeIntoDataBaseIfItIsNewsPage(doc, link);
                    Dao.insertProcessedLink(link);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void parseUrlsFromPageAndStoreIntoDatabase(Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            if (href.startsWith("//")) {
                href = "https:" + href;
            }
            if (!href.toLowerCase().startsWith("java") && !href.equals("")) {
                System.out.println(href);
                Dao.insertLinkToBeProcessed(href);
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", " Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.72 Safari/537.36");

        try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
            HttpEntity entity = response.getEntity();
            String html = EntityUtils.toString(entity);
            return Jsoup.parse(html);
        }
    }

    public void storeIntoDataBaseIfItIsNewsPage(Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTag.child(0).text();
                String content = articleTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                Dao.insertNewsIntoDataBase(link, title, content);
            }
        }
    }


    private static boolean isInterestingLink(String link) {
        return isIndexPage(link) && isNotLoginPage(link) && isNewsPage(link);
    }

    private static boolean isNewsPage(String link) {
        return (link.contains("news.sina.cn") || "https://sina.cn".equals(link));
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }

    private static boolean isIndexPage(String link) {
        return link.contains("sina.cn");
    }


}
