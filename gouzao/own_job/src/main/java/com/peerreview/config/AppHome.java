package com.peerreview.config;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;

/**
 * 应用根目录定位：决定 {@code config/}、{@code data/}、{@code logs/} 落在哪里。
 *
 * <p>为什么需要它：双击 jar 时，JVM 的工作目录是 <b>jar 所在目录</b>而不是项目根。
 * 打包产物位于 {@code target/}，若直接用相对路径，程序会在 {@code target/} 下重新生成一份
 * 默认配置（密码回到默认值）并报「连不上数据库」，而用户在项目根改的配置根本不生效。
 * 这类"配置改了没反应"的故障排查成本很高，所以这里不依赖工作目录。</p>
 *
 * <p>定位顺序：</p>
 * <ol>
 *   <li>系统属性 {@code -Dpeerreview.home=...}（启动脚本、快捷方式可显式指定）；</li>
 *   <li>以代码位置为基准向上寻找含 {@code pom.xml} 的目录——开发期是 {@code target/classes}，
 *       打包后是 {@code target/}，二者都能回到项目根；</li>
 *   <li>找不到则用代码所在目录本身（jar 被单独拷到别处时，就地生成配置，行为如同便携版）；</li>
 *   <li>都取不到时退回当前工作目录。</li>
 * </ol>
 */
public final class AppHome {

    /** 显式指定应用根目录的系统属性名。 */
    public static final String PROPERTY = "peerreview.home";

    /** 从代码位置向上查找 pom.xml 的最大层数：target/classes → target → 项目根。 */
    private static final int MAX_WALK_UP = 3;

    private static volatile Path cached;

    private AppHome() {
    }

    /** 应用根目录（绝对路径）。首次调用时解析并缓存。 */
    public static Path root() {
        Path local = cached;
        if (local == null) {
            synchronized (AppHome.class) {
                local = cached;
                if (local == null) {
                    local = resolveRoot();
                    cached = local;
                }
            }
        }
        return local;
    }

    /** 把配置中的相对路径解析为基于应用根目录的绝对路径；本来就是绝对路径则原样返回。 */
    public static Path resolve(String path) {
        Path value = Path.of(path);
        return value.isAbsolute() ? value.normalize() : root().resolve(value).normalize();
    }

    /** 仅供测试使用：清空缓存以便重新解析。 */
    static synchronized void reset() {
        cached = null;
    }

    private static Path resolveRoot() {
        String override = System.getProperty(PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override).toAbsolutePath().normalize();
        }
        Path base = codeBase();
        if (base == null) {
            return Path.of("").toAbsolutePath().normalize();
        }
        Path cursor = base;
        for (int depth = 0; depth < MAX_WALK_UP && cursor != null; depth++) {
            if (Files.isRegularFile(cursor.resolve("pom.xml"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        return base;
    }

    /** 代码所在目录：jar 的父目录，或 {@code target/classes} 这类目录本身。 */
    private static Path codeBase() {
        try {
            CodeSource source = AppHome.class.getProtectionDomain().getCodeSource();
            if (source == null) {
                return null;
            }
            URL location = source.getLocation();
            if (location == null) {
                return null;
            }
            Path path = Path.of(location.toURI());
            return Files.isDirectory(path) ? path : path.getParent();
        } catch (Exception ex) {
            return null;
        }
    }
}
