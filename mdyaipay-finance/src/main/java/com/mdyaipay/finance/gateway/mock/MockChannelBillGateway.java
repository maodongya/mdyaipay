package com.mdyaipay.finance.gateway.mock;

import com.mdyaipay.finance.domain.reconcile.ChannelBillLine;
import com.mdyaipay.finance.gateway.ChannelBillGateway;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 渠道账单拉取的模拟实现。默认没有账单行。
 * <p>不负责解析 CSV。</p>
 */
@Component
public class MockChannelBillGateway implements ChannelBillGateway {

    private final List<ChannelBillLine> scripted;

    /** 空账单，供 Spring 装配。 */
    public MockChannelBillGateway() {
        this(List.of());
    }

    /**
     * @param scripted 预设账单行；拉取时按渠道与业务日过滤
     */
    public MockChannelBillGateway(List<ChannelBillLine> scripted) {
        this.scripted = List.copyOf(scripted);
    }

    /** {@inheritDoc} */
    @Override
    public List<ChannelBillLine> pull(String channel, LocalDate businessDate) {
        return scripted.stream()
                .filter(line -> channel.equals(line.channel()) && businessDate.equals(line.businessDate()))
                .toList();
    }
}
