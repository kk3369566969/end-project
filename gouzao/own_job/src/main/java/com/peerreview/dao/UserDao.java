package com.peerreview.dao;

import com.peerreview.domain.Role;
import com.peerreview.domain.User;

import java.util.List;
import java.util.Optional;

/** 用户数据访问。 */
public interface UserDao {

    Optional<User> findByUsername(String username);

    Optional<User> findById(long id);

    List<User> findByRole(Role role);

    /** 用户名是否已被占用；注册时先查一次，给出「已被注册」而不是数据库约束错误。 */
    boolean existsByUsername(String username);

    /**
     * 新增用户并回填自增主键。
     *
     * <p>并发注册同一用户名时唯一约束仍会拦截，由服务层翻译成可读提示。</p>
     */
    User insert(User user);
}
