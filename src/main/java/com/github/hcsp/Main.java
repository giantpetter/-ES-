package com.github.hcsp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.stream.Collectors;


public class Main {
    private static final String ACCOUNT_NAME = "root";
    private static final String PASSWORDS = "root";
    private static final String DATA_BASE_URL = "jdbc:h2:file:/Users/mac/IdeaProjects/MyProject/xiedaimale-crawler/News";

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection(DATA_BASE_URL, ACCOUNT_NAME, PASSWORDS);
        String link;
        while ((link = getNextLinkThenDelete(connection)) != null) {
            if (isLinkProcessed(connection, link)) {
                continue;
            }
            if (isInterestingLink(link)) {
                Document doc = httpGetAndParseHtml(link);
                parseUrlsFromPageAndStoreIntoDatabase(connection, doc);
                StoreIntoDataBaseIfItIsNewsPage(connection, doc, link);
                updateDatabase(connection, link, "INSERT INTO LINKS_ALREADY_PROCESSED (link)values (?) ");
            }

        }


    }

    private static void parseUrlsFromPageAndStoreIntoDatabase(Connection connection, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            if (href.startsWith("//")) {
                href = "https:" + href;
            }
            if (!href.toLowerCase().startsWith("java")) {
                System.out.println(href);
                updateDatabase(connection, href, "INSERT INTO LINKS_TO_BE_PROCESSED (link)values (?)");
            }
        }
    }

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT LINK FROM LINKS_ALREADY_PROCESSED where link = ?")) {
            statement.setString(1, link);
            ResultSet resultSet = statement.executeQuery();
            //noinspection LoopStatementThatDoesntLoop
            while (resultSet.next()) {
                return true;
            }
        }
        return false;
    }

    private static void updateDatabase(Connection connection, String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    private static String getNextLink(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getString(1);
            }
        }
        return null;
    }

    private static String getNextLinkThenDelete(Connection connection) throws SQLException {
        String link = getNextLink(connection, "select link from LINKS_TO_BE_PROCESSED LIMIT 1");
        updateDatabase(connection, link, "DELETE FROM LINKS_TO_BE_PROCESSED where link = ?");
        return link;
    }

    private static void StoreIntoDataBaseIfItIsNewsPage(Connection connection, Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTag.child(0).text();
                String content = articleTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                try (PreparedStatement statement = connection.prepareStatement("insert into NEWS (title,content,url,created_at,modified_at)" +
                        "values (?,?,?,now(),now())")) {
                    statement.setString(1, title);
                    statement.setString(2, content);
                    statement.setString(3, link);
                    statement.executeUpdate();
                }
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
