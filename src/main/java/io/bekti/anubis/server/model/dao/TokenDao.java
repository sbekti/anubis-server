package io.bekti.anubis.server.model.dao;

import io.bekti.anubis.server.util.HibernateUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TokenDao {

    private static final Logger log = LoggerFactory.getLogger(TokenDao.class);

    public static Integer add(Token token) {
        Session session = HibernateUtils.getSessionFactory().openSession();
        Transaction transaction = null;
        Integer id = null;

        try {
            transaction = session.beginTransaction();
            id = (Integer) session.save(token);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            TokenDao.log.error(e.getMessage(), e);
        } finally {
            session.close();
        }

        return id;
    }

    public static void delete(int tokenId) {
        Session session = HibernateUtils.getSessionFactory().openSession();
        Transaction transaction = null;

        try {
            transaction = session.beginTransaction();
            Token token = session.get(Token.class, new Integer(tokenId));
            session.delete(token);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            TokenDao.log.error(e.getMessage(), e);
        } finally {
            session.close();
        }
    }

    public static void update(Token token) {
        Session session = HibernateUtils.getSessionFactory().openSession();
        Transaction transaction = null;

        try {
            transaction = session.beginTransaction();
            session.update(token);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            TokenDao.log.error(e.getMessage(), e);
        } finally {
            session.close();
        }
    }

    public static List<Token> getAll() {
        List<Token> tokens = new ArrayList<>();
        Session session = HibernateUtils.getSessionFactory().openSession();
        Transaction transaction = null;

        try {
            transaction = session.beginTransaction();
            tokens = session.createQuery("from Token").list();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            TokenDao.log.error(e.getMessage(), e);
        } finally {
            session.close();
        }

        return tokens;
    }

    public static Token getById(int tokenId) {
        Token token = null;
        Session session = HibernateUtils.getSessionFactory().openSession();
        Transaction transaction = null;

        try {
            transaction = session.beginTransaction();
            token = session.get(Token.class, new Integer(tokenId));
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            TokenDao.log.error(e.getMessage(), e);
        } finally {
            session.close();
        }

        return token;
    }

    public static Token getByUUID(String uuid) {
        Token token = null;
        Session session = HibernateUtils.getSessionFactory().openSession();
        Transaction transaction = null;

        try {
            transaction = session.beginTransaction();
            Query query = session.createQuery("from Token where uuid=:uuid");
            query.setParameter("uuid", uuid);
            token = (Token) query.uniqueResult();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            TokenDao.log.error(e.getMessage(), e);
        } finally {
            session.close();
        }

        return token;
    }
}
