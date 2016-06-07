package io.bekti.anubis.server.util;

import io.bekti.anubis.server.model.dao.User;
import io.bekti.anubis.server.model.dao.UserDao;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseUtils {

    private static final Logger log = LoggerFactory.getLogger(DatabaseUtils.class);

    public static void initAdminUser() {
        String adminName = SharedConfiguration.getString("admin.default.name");

        User adminUser = UserDao.getByName(adminName);

        if (adminUser != null) return;

        String defaultPassword = SharedConfiguration.getString("admin.default.password");
        int rounds = SharedConfiguration.getInteger("bcrypt.rounds");
        String hashedPassword = BCrypt.hashpw(defaultPassword, BCrypt.gensalt(rounds));

        adminUser = new User();
        adminUser.setName(adminName);
        adminUser.setPassword(hashedPassword);

        UserDao.add(adminUser);
    }

    public static void startH2Console() {
        boolean enabled = SharedConfiguration.getBoolean("h2.console.enabled");

        if (!enabled) return;

        String port = SharedConfiguration.getString("h2.console.port");
        boolean enableSSL = SharedConfiguration.getBoolean("h2.console.enable.ssl");
        boolean allowOthers = SharedConfiguration.getBoolean("h2.console.allow.others");

        List<String> params = new ArrayList<>();
        params.add("-webPort");
        params.add(port);
        if (enableSSL) params.add("-webSSL");
        if (allowOthers) params.add("-webAllowOthers");

        try {
            Server.createWebServer(params.toArray(new String[params.size()])).start();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

}
