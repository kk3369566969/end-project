package com.peerreview.ui;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;

/**
 * 全局视觉规范：配色、字体、圆角与控件默认样式。
 *
 * <p>所有界面只从这里取色与字号，避免出现「同一功能三种蓝色」的失控感；
 * 需要整体换肤时也只改这一处。</p>
 */
public final class Theme {

    // —— 基础色板 ——
    public static final Color BG = new Color(0xF3F5FA);
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDER = new Color(0xE4E9F2);
    public static final Color TEXT = new Color(0x1F2A37);
    public static final Color MUTED = new Color(0x7B8797);
    public static final Color FAINT = new Color(0x9AA5B4);

    public static final Color PRIMARY = new Color(0x3D63F5);
    public static final Color PRIMARY_HOVER = new Color(0x3255DC);
    public static final Color PRIMARY_PRESSED = new Color(0x2A4AC4);
    public static final Color PRIMARY_SOFT = new Color(0xECF1FF);

    public static final Color SUCCESS = new Color(0x18A46F);
    public static final Color SUCCESS_SOFT = new Color(0xE7F7F0);
    public static final Color WARNING = new Color(0xC9821A);
    public static final Color WARNING_SOFT = new Color(0xFDF3E3);
    public static final Color DANGER = new Color(0xD94B57);
    public static final Color DANGER_SOFT = new Color(0xFDEDEE);
    public static final Color NEUTRAL_SOFT = new Color(0xF0F2F7);
    /** 侧栏未选中项图标色：比正文浅、比占位符深，避免「全部高亮」。 */
    public static final Color NAV_ICON = new Color(0x8C97AA);

    /** 依次尝试系统中文字体，保证 Windows / macOS 下都有合适的中文字形。 */
    private static final List<String> PREFERRED_FONTS = Arrays.asList(
            "Microsoft YaHei UI", "微软雅黑", "PingFang SC", "Segoe UI", "SansSerif");
    private static final String FONT_FAMILY = pickFontFamily();

    private Theme() {
    }

    /** 安装主题：FlatLaf 负责控件外形，这里覆盖字号、圆角与语义色。 */
    public static void install() {
        FlatLightLaf.setup();
        UIManager.put("defaultFont", plain(13));
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ProgressBar.arc", 10);

        UIManager.put("Panel.background", BG);
        UIManager.put("Viewport.background", SURFACE);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.focusColor", PRIMARY);

        UIManager.put("Component.borderColor", BORDER);
        UIManager.put("Component.disabledBorderColor", BORDER);
        UIManager.put("Component.focusedBorderColor", PRIMARY);
        UIManager.put("TextField.background", SURFACE);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.placeholderForeground", FAINT);
        UIManager.put("TextField.margin", new java.awt.Insets(4, 10, 4, 10));
        UIManager.put("TextArea.margin", new java.awt.Insets(8, 10, 8, 10));
        UIManager.put("ComboBox.background", SURFACE);

        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.selectionBackground", PRIMARY_SOFT);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.rowHeight", 40);
        UIManager.put("TableHeader.height", 38);
        UIManager.put("TableHeader.background", SURFACE);
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("TableHeader.separatorColor", BORDER);
        UIManager.put("TableHeader.bottomSeparatorColor", BORDER);

        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.track", BG);
        UIManager.put("ScrollBar.thumb", new Color(0xCBD3E0));

        UIManager.put("ToolTip.background", new Color(0x25303F));
        UIManager.put("ToolTip.foreground", Color.WHITE);

        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT);
    }

    public static Font plain(int size) {
        return new Font(FONT_FAMILY, Font.PLAIN, size);
    }

    public static Font bold(int size) {
        return new Font(FONT_FAMILY, Font.BOLD, size);
    }

    private static String pickFontFamily() {
        for (String candidate : PREFERRED_FONTS) {
            if (isAvailable(candidate)) {
                return candidate;
            }
        }
        return Font.SANS_SERIF;
    }

    private static boolean isAvailable(String family) {
        return Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
                .anyMatch(name -> name.equalsIgnoreCase(family));
    }
}
