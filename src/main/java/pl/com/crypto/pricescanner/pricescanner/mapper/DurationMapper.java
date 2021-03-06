package pl.com.crypto.pricescanner.pricescanner.mapper;

import org.knowm.xchange.binance.dto.marketdata.KlineInterval;
import pl.com.crypto.pricescanner.pricescanner.adapter.CandleDuration;
import pl.com.crypto.pricescanner.pricescanner.error.UnsupportedCandleIntervalException;

import java.time.Duration;
import java.util.Arrays;

public class DurationMapper {

    public static Duration map(KlineInterval klineInterval) {
        return Duration.ofMillis(klineInterval.getMillis());
    }

    public static KlineInterval map(CandleDuration candleDuration) {
        return map(candleDuration.getDuration());
    }

    public static KlineInterval map(Duration duration) {
        return Arrays.stream(
                KlineInterval.values())
                .filter(interval -> interval.getMillis().equals(duration.toMillis()))
                .findFirst()
                .orElseThrow(() -> new UnsupportedCandleIntervalException(duration.toString()));
    }
}
