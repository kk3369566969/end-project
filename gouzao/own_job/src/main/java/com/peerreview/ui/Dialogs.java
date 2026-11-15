package com.peerreview.ui;

import com.peerreview.common.AppException;
import com.peerreview.common.AppLogger;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * 对话框封装：统一提示语气与按钮文案，避免各处出现中英混排的「OK / Cancel」。
 */
public final class Dialogs {

    private Dialogs() {
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, html(message), "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void success(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, html(message), "操作成功", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, html(message), "操作失败", JOptionPane.ERROR_MESSAGE);
    }

    /** 业务异常直接展示其面向用户的文案，其余异常给出兜底提示并保留原始信息。 */
    public static void error(Component parent, Throwable error) {
        AppLogger.get().error("界面操作失败：" + error.getMessage(), error);
        if (error instanceof AppException) {
            error(parent, error.getMessage());
        } else {
            error(parent, "发生未预期的错误：" + error.getMessage());
        }
    }

    public static boolean confirm(Component parent, String title, String message, String confirmText) {
        Object[] options = {confirmText, "取消"};
        int choice = JOptionPane.showOptionDialog(parent, html(message), title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        return choice == 0;
    }

    /** 多行文本转 HTML，保证长提示能自动换行而不是被拉成一条长线。 */
    private static String html(String message) {
        String escaped = message == null ? "" : message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
        return "<html><body style='width:360px;font-family:sans-serif'>" + escaped + "</body></html>";
    }
}
