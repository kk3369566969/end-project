package com.peerreview.ui;

import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * 主界面中的一个功能页。
 *
 * <p>主框架只认识该接口：负责切换卡片、刷新页头文字，并在进入页面时调用 {@link #onShow()}
 * 让页面按最新数据重建列表 —— 页面之间不需要互相通知。</p>
 */
public interface Page {

    /** 侧栏导航文字。 */
    String navLabel();

    Icon navIcon();

    /** 页头标题。 */
    String title();

    /** 页头副标题（一句话说明当前页能做什么）。 */
    String subtitle();

    /** 页面内容（只构建一次）。 */
    JComponent view();

    /** 每次切换到该页时调用，用于拉取最新数据。 */
    void onShow();
}
