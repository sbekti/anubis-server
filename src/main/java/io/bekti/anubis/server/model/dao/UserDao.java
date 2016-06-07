package io.bekti.anubis.server.model.dao;

import io.bekti.anubis.server.util.HibernateUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDao.class);

    public static Integer add(User user) {
        Session session = HibernateUtils.getSessionFactory().openSession();
        Transaction transaction = null;
        Integer id = null;

        try {
            transaction = session.beginTransaction();
            id = (Integer) session.save(user);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            UserDao.log.error(e.getMessage(), e);
        } finally {
            session.close();
        }

        return id;
    }

    public static void delete(int userId) {
        Session session = HibernateUtils.getSessionFactory().openSession();
        Transaction transaction = null;

        try {
            transaction = session.beginTransaction();
            User user = session.get(User.class, new Integer(userId));
            session.delete(user);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            UserDao.log.error(e.getMessage(), e);
        } finally {
            session.close();
        }
    }

    public static void update(User user) {
        Session session = HibernateUtils.getSessionFactory().openSession();
        Transaction transaction = null;

        try {
            transaction = session.beginTransaction();
            session.update(user);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            UserDao.log.error(e.getMessage(), e);
        } finally {
            session.close();
        }
    }

    public static List<User> getAll() {
        List<User> users = new ArrayList<>();
        Session session = HibernateUtils.getSessionFactory().openSession();
        Transaction transaction = null;

        try {
            transaction = session.beginTransaction();
            users = session.createQuery("from User").list();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            UserDao.log.error(e.getMessage(), e);
        } finally {
            session.close();
        }

        return users;
    }

    public static User getById(int userId) {
        User user = null;
        Session session = HibernateUtils.getSessionFactory().openSession();
        Transaction transaction = null;

        try {
            transaction = session.beginTransaction();
            user = session.get(User.class, new Integer(userId));
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            UserDao.log.error(e.getMessage(), e);
        } finally {
            session.close();
        }

        return user;
    }

    public static User getByName(String name) {
        User user = null;
        Session session = HibernateUtils.getSessionFactory().openSession();
        Transaction transaction = null;

        try {
            transaction = session.beginTransaction();
            Query query = session.createQuery("from User where name=:name");
            query.setParameter("name", name);
            user = (User) query.uniqueResult();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            UserDao.log.error(e.getMessage(), e);
        } finally {
            session.close();
        }

        return user;
    }
}
