package com.github.hcsp;

import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLinkThenDelete() throws SQLException;
    
    boolean isLinkProcessed(String link) throws SQLException;

    void insertNewsIntoDataBase(String url, String title, String content) throws SQLException;

    void insertProcessedLink(String link) throws SQLException;

    void insertLinkToBeProcessed(String href) throws SQLException;
}
