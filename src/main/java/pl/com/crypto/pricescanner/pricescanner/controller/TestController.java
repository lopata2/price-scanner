package pl.com.crypto.pricescanner.pricescanner.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.IsLowestRule;
import org.ta4j.core.trading.rules.StopGainRule;
import pl.com.crypto.pricescanner.pricescanner.adapter.CandleDuration;
import pl.com.crypto.pricescanner.pricescanner.indicators.BollingerBandsPricePositionIndicator;
import pl.com.crypto.pricescanner.pricescanner.model.Market;
import pl.com.crypto.pricescanner.pricescanner.model.TradingStrategy;
import pl.com.crypto.pricescanner.pricescanner.service.CurrencyPairService;
import pl.com.crypto.pricescanner.pricescanner.service.MarketService;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/test-api")
public class TestController {

    private final MarketService marketService;
    private final CurrencyPairService currencyPairService;

    @GetMapping(value = "/start")
    public HttpStatus startPriceScanning() {
        log.info("STARTED");
        currencyPairService.streamTicker(CurrencyPair.BTC_USDT);
        Market m = marketService.createMarket(CurrencyPair.BTC_USDT, CandleDuration.m1);


        ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(m.getTimeSeries());
        SMAIndicator smaIndicator = new SMAIndicator(closePriceIndicator, closePriceIndicator.getTimeSeries().getBarCount());
        StandardDeviationIndicator standardDeviationIndicator = new StandardDeviationIndicator(
                closePriceIndicator, closePriceIndicator.getTimeSeries().getBarCount());
        BollingerBandsMiddleIndicator bollingerBandsMiddleIndicator = new BollingerBandsMiddleIndicator(smaIndicator);
        BollingerBandsLowerIndicator bollingerBandsLowerIndicator = new BollingerBandsLowerIndicator(bollingerBandsMiddleIndicator, standardDeviationIndicator);
        BollingerBandsUpperIndicator bollingerBandsUpperIndicator = new BollingerBandsUpperIndicator(bollingerBandsMiddleIndicator, standardDeviationIndicator);
        BollingerBandsPricePositionIndicator bollingerBandsPricePositionIndicator = new BollingerBandsPricePositionIndicator(
                bollingerBandsLowerIndicator, bollingerBandsMiddleIndicator, bollingerBandsUpperIndicator);
//

        m.addIndicator(bollingerBandsLowerIndicator);
        m.addIndicator(bollingerBandsMiddleIndicator);
        m.addIndicator(bollingerBandsUpperIndicator);
        m.addIndicator(bollingerBandsPricePositionIndicator);


        RSIIndicator rsiIndicator = new RSIIndicator(closePriceIndicator, closePriceIndicator.getTimeSeries().getBarCount());
        TradingStrategy tradingStrategy = new TradingStrategy();

        tradingStrategy.addIndicator(bollingerBandsLowerIndicator);
        tradingStrategy.addIndicator(bollingerBandsMiddleIndicator);
        tradingStrategy.addIndicator(bollingerBandsUpperIndicator);
        tradingStrategy.addIndicator(bollingerBandsPricePositionIndicator);

        Rule enterRule = new CrossedDownIndicatorRule(rsiIndicator, 20);

        Rule exitRule = new CrossedUpIndicatorRule(rsiIndicator, 40);

        Strategy strategy =  new BaseStrategy(enterRule, exitRule);


        Rule exitRule2 = new CrossedDownIndicatorRule(rsiIndicator, 60);

        Rule enterRule2 = new CrossedUpIndicatorRule(rsiIndicator, 80);

        Strategy strategy2 =  new BaseStrategy(enterRule2, exitRule2);

        Strategy strategy3 = strategy.or(strategy2);

        TimeSeriesManager seriesManager = new TimeSeriesManager(m.getTimeSeries());
        TradingRecord tradingRecord = seriesManager.run(strategy3);

        AnalysisCriterion criterion = new TotalProfitCriterion();
        Num profit = criterion.calculate(m.getTimeSeries(), tradingRecord);

        return HttpStatus.OK;
    }


    @GetMapping(value = "/stop")
    public HttpStatus stop() {
        marketService.disconnectAllMarkets();
        return HttpStatus.OK;
    }
}

