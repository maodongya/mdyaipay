/**
 * Dubbo 压测驱动：{@link DubboLoadTestDriver}，协议 {@code dubbo}，基于 {@code GenericService} 泛化调用。
 * <p>
 * 无需业务 API jar；{@code ReferenceConfig} 按 target 缓存复用连接。参数见 {@code docs/loadtest-design.md}。
 */
package com.mdyaipay.tools.loadtest.dubbo;
