package com.arth.bot.core.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
@Configuration
public class MybatisPlusConfig {

    private final DataSource dataSource;

    public MybatisPlusConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public MybatisPlusInterceptor mpInterceptor() {
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return mybatisPlusInterceptor;
    }

    /**
     * 检查 SQL 数据库连接，并判断表是否存在
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkSqlConnectionAndTables() {
        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL();
            log.info("[sql] SQL database connection successful: {}", url);

            boolean pjskTableExists = checkTableExists(conn, "t_pjsk_binding");
            boolean userTableExists = checkTableExists(conn, "t_user");
            boolean groupTableExists = checkTableExists(conn, "t_group");
            boolean membershipTableExists = checkTableExists(conn, "t_membership");
            boolean subscriptionTableExists = checkTableExists(conn, "t_subscription");

            log.info("[sql] table check results - t_pjsk_binding: {}, t_user: {}, t_group: {}, t_membership: {}, t_subscription: {}",
                    pjskTableExists, userTableExists, groupTableExists, membershipTableExists, subscriptionTableExists);

            if (!userTableExists) {
                log.warn("[sql] user table (t_user) does not exist. Database may not be initialized yet.");
            }

        } catch (SQLException e) {
            log.error("[sql] SQL database connection failed!", e);
        }
    }

    private boolean checkTableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet tables = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }
}