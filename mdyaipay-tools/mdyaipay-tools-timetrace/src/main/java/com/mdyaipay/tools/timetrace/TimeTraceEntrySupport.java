package com.mdyaipay.tools.timetrace;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/** {@link TimeTraceAspect} 入口 advice 的核心逻辑。 */
public final class TimeTraceEntrySupport {

    private TimeTraceEntrySupport() {
    }

    public static Object aroundEntry(ProceedingJoinPoint joinPoint, TimeTraceListener listener) throws Throwable {
        if (TimeTraceContext.isTracing()) {
            return joinPoint.proceed();
        }

        TimeTrace annotation = resolveAnnotation(joinPoint);
        String label = buildLabel(joinPoint, annotation);
        boolean report = annotation == null || annotation.reportOnComplete();
        long thresholdMillis = annotation != null ? annotation.reportThresholdMillis() : 0L;
        TimeTraceContext.Session session = TimeTraceContext.begin(label, listener, report, thresholdMillis);

        String signature = joinPoint.getSignature().toShortString();
        TimeTraceNode root = TimeTraceContext.enterFrame(signature);
        long start = System.nanoTime();
        Throwable error = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            long elapsed = System.nanoTime() - start;
            TimeTraceContext.leaveFrame(root, elapsed);
            TimeTraceReport reportPayload = new TimeTraceReport(
                    session.label,
                    session.root,
                    elapsed,
                    error);
            if (shouldEmitReport(session, elapsed, error)) {
                session.listener.onComplete(reportPayload);
            }
            TimeTraceContext.end();
        }
    }

    private static boolean shouldEmitReport(TimeTraceContext.Session session, long elapsedNanos, Throwable error) {
        if (!session.reportOnComplete) {
            return false;
        }
        if (error != null) {
            return true;
        }
        if (session.reportThresholdMillis <= 0) {
            return true;
        }
        long thresholdNanos = session.reportThresholdMillis * 1_000_000L;
        return elapsedNanos >= thresholdNanos;
    }

    private static TimeTrace resolveAnnotation(ProceedingJoinPoint joinPoint) {
        if (joinPoint.getSignature() instanceof MethodSignature methodSignature) {
            Method method = methodSignature.getMethod();
            TimeTrace onMethod = method.getAnnotation(TimeTrace.class);
            if (onMethod != null) {
                return onMethod;
            }
            Class<?> targetClass = joinPoint.getTarget() != null
                    ? joinPoint.getTarget().getClass()
                    : method.getDeclaringClass();
            return targetClass.getAnnotation(TimeTrace.class);
        }
        Object target = joinPoint.getTarget();
        if (target != null) {
            return target.getClass().getAnnotation(TimeTrace.class);
        }
        return null;
    }

    private static String buildLabel(ProceedingJoinPoint joinPoint, TimeTrace annotation) {
        if (annotation != null && !annotation.value().isBlank()) {
            return annotation.value();
        }
        return joinPoint.getSignature().toShortString();
    }
}
