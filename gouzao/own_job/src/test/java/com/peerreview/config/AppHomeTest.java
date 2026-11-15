package com.peerreview.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AppHome} 决定程序读哪一份配置，出错时会表现为「改了配置却没生效」，
 * 所以这里把它当作启动契约来测。
 */
class AppHomeTest {

    @Test
    @DisplayName("运行时根目录指回项目根（含 pom.xml），而不是 target/classes")
    void rootPointsToProjectDirectory() {
        Path root = AppHome.root();

        assertTrue(Files.isRegularFile(root.resolve("pom.xml")),
                "应定位到含 pom.xml 的项目根，实际为 " + root);
        assertTrue(Files.isDirectory(root.resolve("src")),
                "项目根下应有 src 目录，实际为 " + root);
    }

    @Test
    @DisplayName("相对路径按应用根解析为绝对路径，绝对路径原样保留")
    void resolveHandlesRelativeAndAbsolute() {
        Path relative = AppHome.resolve("config/config.properties");
        assertEquals(AppHome.root().resolve("config").resolve("config.properties"), relative);
        assertTrue(relative.isAbsolute());

        Path absolute = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath();
        assertEquals(absolute, AppHome.resolve(absolute.toString()));
    }

    @Test
    @DisplayName("系统属性 peerreview.home 优先于代码位置推导")
    void systemPropertyOverrides() {
        Path temp = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath();
        String original = System.getProperty(AppHome.PROPERTY);
        System.setProperty(AppHome.PROPERTY, temp.toString());
        try {
            AppHome.reset();
            assertEquals(temp, AppHome.root());
        } finally {
            if (original == null) {
                System.clearProperty(AppHome.PROPERTY);
            } else {
                System.setProperty(AppHome.PROPERTY, original);
            }
            AppHome.reset();
        }
    }

    @Test
    @DisplayName("空属性值不覆盖，仍按代码位置推导")
    void blankPropertyIgnored() {
        String original = System.getProperty(AppHome.PROPERTY);
        System.setProperty(AppHome.PROPERTY, "   ");
        try {
            AppHome.reset();
            Path root = AppHome.root();
            assertFalse(root.toString().isBlank());
            assertTrue(Files.isRegularFile(root.resolve("pom.xml")));
        } finally {
            if (original == null) {
                System.clearProperty(AppHome.PROPERTY);
            } else {
                System.setProperty(AppHome.PROPERTY, original);
            }
            AppHome.reset();
        }
    }
}
