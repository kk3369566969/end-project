package com.peerreview.common;

/**
 * 数据校验失败异常（用户输入非法、状态不允许等）。
 *
 * <p>UI 层捕获后直接展示 message 即可，message 必须是面向用户的中文描述。</p>
 */
public class ValidationException extends AppException {

    public ValidationException(String message) {
        super(message);
    }
}
