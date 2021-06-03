package com.github.hcsp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcCrawlerDao implements CrawlerDao {
    private static final String ACCOUNT_NAME = "root";
    private static final String PASSWORDS = "root";
    private static final String DATA_BASE_URL = "jdbc:h2:file:/Users/mac/IdeaProjects/MyProject/xiedaimale-crawler/News";
    private final Connection connection;
    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public JdbcCrawlerDao() {
        try {
            this.connection = DriverManager.getConnection(DATA_BASE_URL, ACCOUNT_NAME, PASSWORDS);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public String getNextLinkThenDelete() throws SQLException {
        String link = getNextLink("select link from LINKS_TO_BE_PROCESSED LIMIT 1");
        updateDatabase(link, "DELETE FROM LINKS_TO_BE_PROCESSED where link = ?");
        return link;
    }

    public boolean isLinkProcessed(String link) throws SQLException {
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

    public void updateDatabase(String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    private String getNextLink(String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getString(1);
            }
        }
        return null;
    }

    public void insertNewsIntoDataBase(String url, String title, String content) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("insert into NEWS (title,content,url,created_at,modified_at)" +
                "values (?,?,?,now(),now())")) {
            statement.setString(1, title);
            statement.setString(2, content);
            statement.setString(3, url);
            statement.executeUpdate();
        }
    }

    @Override
    public void insertProcessedLink(String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO LINKS_ALREADY_PROCESSED (link)values (?) ")) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    @Override
    public void insertLinkToBeProcessed(String href) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO LINKS_TO_BE_PROCESSED (link)values (?)")) {
            statement.setString(1, href);
            statement.executeUpdate();
        }
    }
}
