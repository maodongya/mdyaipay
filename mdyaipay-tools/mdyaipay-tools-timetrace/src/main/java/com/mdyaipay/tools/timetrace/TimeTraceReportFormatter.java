package com.mdyaipay.tools.timetrace;

/**
 * 将 {@link TimeTraceReport} 格式化为可读文本（树形缩进 + 毫秒，保留三位小数）。
 * <p>
 * 供 {@link TimeTraceListener#LOG_TO_STDOUT} 及业务侧接入 SLF4J 时复用。
 */
public final class TimeTraceReportFormatter {

    private TimeTraceReportFormatter() {
    }

    /**
     * 生成多行报告：首行摘要 + 以根节点开始的调用树。
     */
    public static String format(TimeTraceReport report) {
        StringBuilder sb = new StringBuilder();

        /* 功能块：报告头 — 入口标签、总耗时与失败摘要 */
        sb.append("[TimeTrace] ").append(report.getLabel());
        sb.append(" total=").append(nanosToMillis(report.getTotalNanos())).append("ms");
        if (!report.isSuccess()) {
            sb.append(" (failed: ").append(report.getError().getClass().getSimpleName()).append(')');
        }
        sb.append(System.lineSeparator());

        /* 功能块：调用树 — 自根节点递归输出 total/self */
        appendNode(sb, report.getRoot(), "", true);
        return sb.toString();
    }

    /**
     * 递归输出单个节点及其子节点；{@code prefix}/{@code last} 控制树形连接线。
     */
    private static void appendNode(StringBuilder sb, TimeTraceNode node, String prefix, boolean last) {
        /* 功能块：当前节点一行 */
        sb.append(prefix);
        sb.append(last ? "└─ " : "├─ ");
        sb.append(node.getSignature());
        sb.append(" total=").append(nanosToMillis(node.getDurationNanos())).append("ms");
        sb.append(" self=").append(nanosToMillis(node.getSelfNanos())).append("ms");
        sb.append(System.lineSeparator());

        /* 功能块：子节点 — 缩进前缀随兄弟序号变化 */
        var children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            boolean childLast = i == children.size() - 1;
            String childPrefix = prefix + (last ? "   " : "│  ");
            appendNode(sb, children.get(i), childPrefix, childLast);
        }
    }

    /** 纳秒转毫秒字符串，供报告统一口径。 */
    static String nanosToMillis(long nanos) {
        return String.format("%.3f", nanos / 1_000_000.0);
    }
}
