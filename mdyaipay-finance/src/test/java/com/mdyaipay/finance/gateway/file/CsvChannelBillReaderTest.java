package com.mdyaipay.finance.gateway.file;

import com.mdyaipay.finance.domain.reconcile.ChannelBillLine;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CSV 渠道账单读取：表头固定为渠道交易号、金额（分）、渠道、业务日。
 */
class CsvChannelBillReaderTest {

    /** 按表头读出账单行。 */
    @Test
    void shouldReadBillLines() throws Exception {
        Path file = Files.createTempFile("channel-bill", ".csv");
        Files.writeString(file, """
                channelTradeNo,amount,channel,businessDate
                CH-1,100,MOCK,2026-09-30
                """);

        List<ChannelBillLine> lines = CsvChannelBillReader.read(file);

        assertEquals(1, lines.size());
        assertEquals("CH-1", lines.get(0).channelTradeNo());
        assertEquals(100L, lines.get(0).amount());
        assertEquals("MOCK", lines.get(0).channel());
        assertEquals(LocalDate.of(2026, 9, 30), lines.get(0).businessDate());
    }

    /** 缺少约定表头时拒绝。 */
    @Test
    void shouldRejectWhenHeaderMissing() throws Exception {
        Path file = Files.createTempFile("channel-bill", ".csv");
        Files.writeString(file, "CH-1,100,MOCK,2026-09-30\n");
        assertThrows(IllegalArgumentException.class, () -> CsvChannelBillReader.read(file));
    }
}
