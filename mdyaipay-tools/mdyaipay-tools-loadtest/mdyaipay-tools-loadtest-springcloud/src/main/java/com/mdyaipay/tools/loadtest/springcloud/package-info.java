/**
 * Spring Cloud 压测驱动：{@link SpringCloudLoadTestDriver}，协议 {@code springcloud}。
 * <p>
 * 支持 {@code direct-url}、{@code loadbalancer}（静态 {@code instances} 轮询）、{@code feign}（Feign {@link feign.Client}）。
 * 打 Gateway 入口时通常用 HTTP 驱动即可；本模块侧重服务间 LB/Feign 路径。
 */
package com.mdyaipay.tools.loadtest.springcloud;
