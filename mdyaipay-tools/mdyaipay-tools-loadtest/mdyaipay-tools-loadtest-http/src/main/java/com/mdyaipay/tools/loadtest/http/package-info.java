/**
 * HTTP/HTTPS 压测驱动：{@link com.mdyaipay.tools.loadtest.http.HttpLoadTestDriver}，
 * 协议标识 {@code http}，目标参数见 {@code docs/loadtest-design.md} 中 {@code target} 段。
 * <p>
 * {@link com.mdyaipay.tools.loadtest.http.HttpRequestExecutor} 可供 Spring Cloud 等模块复用。
 * <p>对外收单压测：{@code bodyMode: merchantEncryptedCollect}，见 {@link MerchantEncryptedCollectBodyBuilder}。</p>
 */
package com.mdyaipay.tools.loadtest.http;
