package com.mdyaipay.tools.trace.slice;

import com.mdyaipay.tools.trace.TraceContext;
import com.mdyaipay.tools.trace.TraceSnapshot;
import com.mdyaipay.tools.trace.autoconfigure.TraceProperties;
import com.mdyaipay.tools.trace.mdc.TraceMdcSupport;
import com.mdyaipay.tools.trace.slice.sample.CustomSliceBean;
import com.mdyaipay.tools.trace.slice.sample.SliceCollaboratorService;
import com.mdyaipay.tools.trace.slice.sample.SliceSampleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TraceSliceAspect} 行为测试：通过 {@link AspectJProxyFactory} 验证切点与子 span 嵌套。
 */
class TraceSliceAspectTest {

    private final TraceMdcSupport mdcSupport = new TraceMdcSupport(new TraceProperties());
    private final TraceSliceAspect aspect = new TraceSliceAspect(mdcSupport, new TraceProperties());

    private SliceCollaboratorService collaborator;
    private SliceSampleService sampleService;
    private CustomSliceBean customSliceBean;

    @BeforeEach
    void setUp() {
        collaborator = proxy(new SliceCollaboratorService());
        sampleService = proxy(new SliceSampleService(collaborator));
        customSliceBean = proxy(new CustomSliceBean());
        collaborator.reset();
        TraceContext.clear();
    }

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    /**
     * 为目标对象织入 {@link TraceSliceAspect}，与 Spring AOP 运行时语义一致。
     */
    private <T> T proxy(T target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    /**
     * 无 Trace 上下文时，切片不改变 spanId（均为 null）。
     */
    @Test
    void noOpWithoutTraceContext() {
        String[] spans = sampleService.runFlow();
        assertNull(spans[0]);
        assertNull(spans[1]);
    }

    /**
     * 有根 Trace 时，入口 Service 与 Collaborator 各获得不同子 span，且结束后恢复根 span。
     */
    @Test
    void nestedSpringBeansGetDistinctChildSpans() throws Exception {
        TraceSnapshot root = TraceSnapshot.startNew();
        TraceContext.run(root, () -> {
            String[] spans = sampleService.runFlow();
            assertNotNull(spans[0]);
            assertNotNull(spans[1]);
            assertNotEquals(root.spanId(), spans[0]);
            assertNotEquals(spans[0], spans[1]);
        });
        assertTrue(TraceContext.current().isEmpty());
    }

    /**
     * {@link TraceSlice} 标记的 Bean 在活跃 Trace 下也会切换 span。
     */
    @Test
    void explicitTraceSliceOnComponent() throws Exception {
        TraceSnapshot root = TraceSnapshot.startNew();
        TraceContext.run(root, () -> {
            String spanInBean = customSliceBean.currentSpanId();
            assertNotNull(spanInBean);
            assertNotEquals(root.spanId(), spanInBean);
        });
    }
}
