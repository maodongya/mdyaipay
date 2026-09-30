package com.mdyaipay.finance.gateway.file;

import com.mdyaipay.finance.domain.reconcile.ChannelBillLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 读取渠道账单 CSV。表头必须为 {@code channelTradeNo,amount,channel,businessDate}。
 * <p>金额单位为分。不负责比对。</p>
 */
public final class CsvChannelBillReader {

    private static final String HEADER = "channelTradeNo,amount,channel,businessDate";

    private CsvChannelBillReader() {
    }

    /**
     * 读取文件中的账单行。空行跳过。
     * <p>前置：文件存在且首个非空行是约定表头。无副作用。</p>
     */
    public static List<ChannelBillLine> read(Path path) {
        List<String> raw = readLines(path);
        int headerIndex = firstNonBlank(raw);
        if (headerIndex < 0 || !HEADER.equals(raw.get(headerIndex).trim())) {
            throw new IllegalArgumentException("csv header must be " + HEADER);
        }
        List<ChannelBillLine> lines = new ArrayList<>();
        for (int i = headerIndex + 1; i < raw.size(); i++) {
            String row = raw.get(i).trim();
            if (!row.isEmpty()) {
                lines.add(parseRow(row));
            }
        }
        return List.copyOf(lines);
    }

    private static List<String> readLines(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException ex) {
            throw new IllegalArgumentException("failed to read csv: " + path, ex);
        }
    }

    private static int firstNonBlank(List<String> raw) {
        for (int i = 0; i < raw.size(); i++) {
            if (!raw.get(i).isBlank()) {
                return i;
            }
        }
        return -1;
    }

    private static ChannelBillLine parseRow(String row) {
        String[] cells = row.split(",", -1);
        if (cells.length != 4) {
            throw new IllegalArgumentException("csv row must have 4 columns");
        }
        long amount;
        try {
            amount = Long.parseLong(cells[1].trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("amount must be integer fen", ex);
        }
        return new ChannelBillLine(
                cells[0].trim(),
                amount,
                cells[2].trim(),
                LocalDate.parse(cells[3].trim()));
    }
}
