package com.orbit.portfolio.controller;

import com.orbit.portfolio.service.YahooMarketService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/yahoo")
public class YahooDataController {

    private final YahooMarketService yahooMarketService;

    public YahooDataController(YahooMarketService yahooMarketService) {
        this.yahooMarketService = yahooMarketService;
    }

    // ===========================
    // MOVERS
    // ===========================

    @GetMapping("/top-gainers")
    public List<Map<String, Object>> topGainers() {
        return yahooMarketService.getTopGainers();
    }

    @GetMapping("/top-losers")
    public List<Map<String, Object>> topLosers() {
        return yahooMarketService.getTopLosers();
    }

    @GetMapping("/most-active")
    public List<Map<String, Object>> mostActive() {
        return yahooMarketService.getMostActive();
    }

    @GetMapping("/news/{ticker}")
    public List<Map<String, Object>> newsByTicker(@PathVariable String ticker) {
        return yahooMarketService.getNewsByTicker(ticker);
    }

}
