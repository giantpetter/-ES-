package com.github.hcsp;

import java.sql.SQLException;

public interface CrawlerJdbc {
    String getNextLinkThenDelete() throws SQLException;

    String getNextLink(String sql) throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

    void updateDatabase(String link, String sql) throws SQLException;

    void insertNewsIntoDataBase(String url, String title, String content) throws SQLException;
}
