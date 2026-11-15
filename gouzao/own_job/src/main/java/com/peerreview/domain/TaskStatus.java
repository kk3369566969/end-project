package com.peerreview.domain;

/** 互评任务的两种状态，构成「待互评 → 已完成」的流转。 */
public enum TaskStatus {

    PENDING("待互评"),
    COMPLETED("已完成");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
