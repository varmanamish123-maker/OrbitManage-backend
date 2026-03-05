package com.orbit.portfolio.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class YahooMarketService {

    private static final RestTemplate restTemplate = new RestTemplate();

    // ===========================
    // YAHOO ENDPOINTS
    // ===========================
    private static final String TOP_GAINERS_URL =
            "https://query1.finance.yahoo.com/v1/finance/screener/predefined/saved?count=10&scrIds=day_gainers";

    private static final String TOP_LOSERS_URL =
            "https://query1.finance.yahoo.com/v1/finance/screener/predefined/saved?count=10&scrIds=day_losers";

    private static final String MOST_ACTIVE_URL =
            "https://query1.finance.yahoo.com/v1/finance/screener/predefined/saved?count=10&scrIds=most_actives";

    private static final String GENERAL_NEWS_URL =
            "https://query1.finance.yahoo.com/v1/finance/news?category=generalnews";

    private static final String SEARCH_NEWS_URL =
            "https://query1.finance.yahoo.com/v1/finance/search?q=%s&newsCount=5";

    // ===========================
    // PUBLIC METHODS
    // ===========================

    public List<Map<String, Object>> getTopGainers() {
        return fetchQuotesListAsMap(TOP_GAINERS_URL);
    }

    public List<Map<String, Object>> getTopLosers() {
        return fetchQuotesListAsMap(TOP_LOSERS_URL);
    }

    public List<Map<String, Object>> getMostActive() {
        return fetchQuotesListAsMap(MOST_ACTIVE_URL);
    }

    public List<Map<String, Object>> getGeneralNews() {
        try {
            String json = fetchYahooJson(GENERAL_NEWS_URL);
            JSONObject root = new JSONObject(json);

            JSONArray arr = root
                    .getJSONObject("items")
                    .getJSONArray("result");

            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getJSONObject(i).toMap()); // ✅ convert to Map
            }
            return list;

        } catch (Exception e) {
            System.err.println("⚠ Failed to fetch general news: " + e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getNewsByTicker(String ticker) {
        try {
            String url = String.format(SEARCH_NEWS_URL, ticker);
            String json = fetchYahooJson(url);

            JSONObject root = new JSONObject(json);
            JSONArray arr = root.getJSONArray("news");

            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getJSONObject(i).toMap()); // ✅ convert to Map
            }
            return list;

        } catch (Exception e) {
            System.err.println("⚠ Failed to fetch news for " + ticker + ": " + e.getMessage());
            return List.of();
        }
    }

    // ===========================
    // INTERNAL HELPERS
    // ===========================

    private List<Map<String, Object>> fetchQuotesListAsMap(String url) {
        try {
            String json = fetchYahooJson(url);
            JSONObject root = new JSONObject(json);

            JSONArray quotes = root
                    .getJSONObject("finance")
                    .getJSONArray("result")
                    .getJSONObject(0)
                    .getJSONArray("quotes");

            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < quotes.length(); i++) {
                list.add(quotes.getJSONObject(i).toMap()); // ✅ convert to Map
            }

            return list;

        } catch (Exception e) {
            System.err.println("⚠ Failed to fetch quotes list: " + e.getMessage());
            return List.of();
        }
    }

    private static String fetchYahooJson(String url) {

        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Mozilla/5.0");
        headers.add("Accept", "application/json");
        headers.add("Referer", "https://finance.yahoo.com");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        return response.getBody();
    }

    // ===========================
    // MAIN FOR TESTING
    // ===========================
    public static void main(String[] args) {

        YahooMarketService service = new YahooMarketService();

        System.out.println("=== TOP LOSERS ===");
        service.getTopLosers().forEach(q ->
                System.out.println(q.get("symbol") + "  "
                        + q.get("shortName") + "  "
                        + q.get("regularMarketPrice") + " ("
                        + q.get("regularMarketChangePercent") + "%)")
        );

        System.out.println("\n=== GENERAL NEWS ===");
        service.getGeneralNews().stream().limit(3).forEach(n ->
                System.out.println("- " + n.get("title") + " | " + n.get("publisher"))
        );

        System.out.println("\n=== NEWS FOR AAPL ===");
        service.getNewsByTicker("AAPL").stream().limit(3).forEach(n ->
                System.out.println("- " + n.get("title") + " | " + n.get("publisher"))
        );
    }
}
