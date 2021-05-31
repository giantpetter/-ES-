package com.github.hcsp;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MyBatisDao implements CrawlerDao {
    private final SqlSessionFactory sqlSessionFactory;

    public MyBatisDao() {
        String resource = "db/mybatis/config.xml";
        InputStream inputStream;
        try {
            inputStream = Resources.getResourceAsStream(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    }

    @Override
    public String getNextLinkThenDelete() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String link = session.selectOne("com.github.hcsp.MyMapper.selectNextAvailableLink");
            if (link != null) {
                session.delete("com.github.hcsp.MyMapper.deleteLink", link);
            }
            return link;
        }
    }


    @Override
    public boolean isLinkProcessed(String link) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            int count =  session.selectOne("com.github.hcsp.MyMapper.countLink", link);
            if (count != 0) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void insertNewsIntoDataBase(String url, String title, String content) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.hcsp.MyMapper.insertNews",new NEWS(title,url,content));
        }
    }

    @Override
    public void insertProcessedLink(String link) {
        Map<String,Object> map = new HashMap<>();
        map.put("tableName","LINKS_ALREADY_PROCESSED");
        map.put("link",link);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.hcsp.MyMapper.insertLinkIntoDatabase",map);
        }
    }

    @Override
    public void insertLinkToBeProcessed(String link) {
        Map<String,Object> map = new HashMap<>();
        map.put("tableName","LINKS_TO_BE_PROCESSED");
        map.put("link",link);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.hcsp.MyMapper.insertLinkIntoDatabase",map);
        }
    }
}
