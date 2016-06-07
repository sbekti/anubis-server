package io.bekti.anubis.server.util;

import io.bekti.anubis.server.model.dao.Token;
import io.bekti.anubis.server.model.dao.User;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HibernateUtils {

    private static final Logger log = LoggerFactory.getLogger(HibernateUtils.class);

    private static SessionFactory sessionFactory;

    static {
        Configuration configuration = new Configuration();

        configuration.setProperty(
                "hibernate.connection.driver_class",
                SharedConfiguration.getString("hibernate.connection.driver_class")
        );

        configuration.setProperty(
                "hibernate.connection.url",
                SharedConfiguration.getString("hibernate.connection.url")
        );

        configuration.setProperty(
                "hibernate.connection.username",
                SharedConfiguration.getString("hibernate.connection.username")
        );

        configuration.setProperty(
                "hibernate.connection.password",
                SharedConfiguration.getString("hibernate.connection.password")
        );

        configuration.setProperty(
                "hibernate.dialect",
                SharedConfiguration.getString("hibernate.dialect")
        );

        configuration.setProperty(
                "hibernate.show_sql",
                SharedConfiguration.getString("hibernate.show_sql")
        );

        configuration.setProperty(
                "hibernate.hbm2ddl.auto",
                SharedConfiguration.getString("hibernate.hbm2ddl.auto")
        );

        configuration.addAnnotatedClass(User.class);
        configuration.addAnnotatedClass(Token.class);

        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties())
                .build();

        try {
            sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        } catch (Exception e) {
            StandardServiceRegistryBuilder.destroy(serviceRegistry);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

}
