package com.github.hcsp;

public class Main {
    public static void main(String[] args) {
        CrawlerDao dao = new MyBatisDao();
        for (int i = 0; i < 8; ++i) {
            new Crawler(dao).start();
        }
    }
}
