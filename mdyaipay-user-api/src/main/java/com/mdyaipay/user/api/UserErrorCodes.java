package com.mdyaipay.user.api;

/**
 * 用户域业务错误码（与 {@link com.mdyaipay.tools.exception.ErrorCode} 平台码并存）。
 * <p>商户开放 API 验签失败优先返回 {@link #SIGN_INVALID} / {@link #SIGN_EXPIRED}。</p>
 */
public final class UserErrorCodes {

    /** 签名不匹配、参数被篡改、sign_method 不支持等。 */
    public static final int SIGN_INVALID = 10101;
    /** 请求 timestamp 超出 {@code user.merchant.sign.max-skew-seconds}。 */
    public static final int SIGN_EXPIRED = 10102;
    /** appKey 不存在或密钥已停用。 */
    public static final int CREDENTIAL_DISABLED = 10103;
    /** 商户状态不允许调用开放 API（如 DISABLED）。 */
    public static final int MERCHANT_STATE_INVALID = 10104;
    /** 商户或店铺不存在。 */
    public static final int MERCHANT_NOT_FOUND = 10105;
    /** 操作人角色不足（如非 OWNER/OP 提交审核）。 */
    public static final int MEMBER_FORBIDDEN = 10106;

    private UserErrorCodes() {
    }
}
