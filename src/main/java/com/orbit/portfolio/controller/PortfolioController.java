package com.orbit.portfolio.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.orbit.portfolio.service.PortfolioService;


@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    
    private PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }   

    @GetMapping("/{portfolioId}/summary")
    public Object getSummary(@PathVariable Long portfolioId) {
        return portfolioService.getPortfolioStats(portfolioId);
    }
}