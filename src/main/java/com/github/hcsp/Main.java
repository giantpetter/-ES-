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
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    private static final String ACCOUNT_NAME = "root";
    private static final String PASSWORDS = "root";
    private static final String DATA_BASE_URL = "jdbc:h2:file:/Users/mac/IdeaProjects/MyProject/xiedaimale-crawler/News.mv.db";

    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection(DATA_BASE_URL, ACCOUNT_NAME, PASSWORDS);
        if(connection.isClosed()){
            System.out.println("Closed!");
        }
        while (true) {
            //待处理的链接池
            List<String> linkPool = LoadUrlsFromDataBase(connection, "select link from LINKS_TO_BE_PROCESSED");

            //以及处理的链接池
            Set<String> processedLinks = new HashSet<>(LoadUrlsFromDataBase(connection, "select link from LINKS_ALREADY_PROCESSED"));

            if (linkPool.isEmpty()) {
                break;
            }
            //从待处理池子中捞一个来处理；
            //处理完后从池子中删除，（包括数据库）
            String link = linkPool.remove(linkPool.size() - 1);
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM LINKS_TO_BE_PROCESSED where link = ?")) {
                statement.setString(1, link);
                statement.executeUpdate();
            }

            //如果该链接以及处理过则跳过循环
            boolean isProcessed = false;
            try (PreparedStatement statement = connection.prepareStatement("SELECT LINK FROM LINKS_ALREADY_PROCESSED where link = ?")) {
                statement.setString(1, link);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    isProcessed = true;
                }
            }
            if (isProcessed) {
                continue;
            }

            if (isInterestingLink(link)) {
                Document doc = httpGetAndParseHtml(link);
                System.out.println(link);
                for (Element aTag : doc.select("a")) {
                    String href = aTag.attr("href");
                    linkPool.add(href);
                    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO LINKS_TO_BE_PROCESSED (link)values (?)")) {
                        statement.setString(1, link);
                        statement.executeUpdate();
                    }
                }
                StoreIntoDataBaseIfItIsNewsPage(doc);
                insertLinkIntoDataBase(connection, link);
                processedLinks.add(link);
            }

        }


    }

    private static void insertLinkIntoDataBase(Connection connection, String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO LINKS_ALREADY_PROCESSED (link)values (?)")) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    private static List<String> LoadUrlsFromDataBase(Connection connection, String sql) throws SQLException {
        List<String> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet result = statement.executeQuery();
            while (result.next()) {
                results.add(result.getString(1));
            }
        }
        return results;
    }

    private static void StoreIntoDataBaseIfItIsNewsPage(Document doc) {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTag.child(0).text();
                System.out.println(title);
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        if (link.startsWith("//")) {
            link = "https:" + link;
        }
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", " Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.72 Safari/537.36");

        try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
            System.out.println(response.getStatusLine());
            HttpEntity entity = response.getEntity();
            String html = EntityUtils.toString(entity);
            return Jsoup.parse(html);
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
