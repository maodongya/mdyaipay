/**
 * 用户域对外 RPC 契约：Facade 接口与 DTO，供 gateway / payment 等模块依赖。
 * <p>实现与 MySQL/Redis/MQ 均在 {@code mdyaipay-user}，本模块保持零 Spring 依赖。</p>
 */
package com.mdyaipay.user.api;
