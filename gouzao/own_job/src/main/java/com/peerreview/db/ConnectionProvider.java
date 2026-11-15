package com.peerreview.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 连接提供者抽象。
 *
 * <p>业务层与 DAO 只依赖该接口，不关心连接从哪来；
 * 后续若要替换为连接池（HikariCP）或测试用的内存库，只需换实现。</p>
 */
public interface ConnectionProvider extends AutoCloseable {

    Connection getConnection() throws SQLException;

    /** 用于日志与界面展示的连接描述。 */
    String describe();

    @Override
    void close();
}
