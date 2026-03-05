package com.orbit.portfolio.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.hibernate.cache.spi.entry.CacheEntry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.client.RestTemplate;

import com.orbit.portfolio.model.ClosePriceHistory;
import com.orbit.portfolio.repository.ClosePriceHistoryRepository;

import org.springframework.http.*;
import org.springframework.stereotype.Service;

@Service
public class PriceFetcher {

	private final ClosePriceHistoryRepository closePriceHistoryRepository;

    public PriceFetcher(ClosePriceHistoryRepository closePriceHistoryRepository) {
        this.closePriceHistoryRepository = closePriceHistoryRepository;
    }
	

    private static final RestTemplate restTemplate = new RestTemplate();
	
    private static final String DUMMY_API_URL =
            "https://c4rm9elh30.execute-api.us-east-1.amazonaws.com/default/cachedPriceData?ticker=";

    private static final String YAHOO_API_URL =
            "https://query1.finance.yahoo.com/v8/finance/chart/";

    private static final String STOCK_MODE = "YAHOO"; // Change to "YAHOO" to switch modes
    private static final String CRYPTO_MODE = "YAHOO"; // Change to "YAHOO" to switch modes

    private static final BigDecimal GOLD_PRICE = BigDecimal.valueOf(1800.00);
	private static final BigDecimal DUMMY_ETH_PRICE = BigDecimal.valueOf(2000.00);
	private static final BigDecimal DUMMY_BTC_PRICE = BigDecimal.valueOf(30000.00);

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

	///////////////////
	/// STOCK PRICE ///
	///////////////////

	public static BigDecimal getStockPrice(String assetName) {
		try {
			BigDecimal current = null;
			//if mode is dummy 
			if (STOCK_MODE.equals("DUMMY")) {

				String json = restTemplate.getForObject(DUMMY_API_URL + assetName, String.class);

				JSONObject priceData = new JSONObject(json).getJSONObject("price_data");
				JSONArray closeArr = priceData.getJSONArray("close");
				JSONArray tsArr = priceData.getJSONArray("timestamp");

				
				for (int i = closeArr.length() - 1; i >= 0; i--) {
					if (!"NaN".equals(closeArr.optString(i))) {
						current = closeArr.getBigDecimal(i);
						break;
					}
				}
				
			}

			else if(STOCK_MODE.equals("YAHOO")) {
				String url = YAHOO_API_URL + assetName + "?interval=1d&range=5d";
				String json = fetchYahooJson(url);

				JSONObject result = new JSONObject(json)
						.getJSONObject("chart")
						.getJSONArray("result")
						.getJSONObject(0);

				JSONArray timestamps = result.getJSONArray("timestamp");
				JSONArray closes = result.getJSONObject("indicators")
						.getJSONArray("quote")
						.getJSONObject(0)
						.getJSONArray("close");

						
				for (int i = closes.length() - 1; i >= 0; i--) {
					if (!closes.isNull(i)) {
						current = closes.getBigDecimal(i);
						break;
					}
				}
			}
			
			return current != null ? current : BigDecimal.ZERO;
		} catch (Exception e) {
			System.err.println("Error fetching stock price for " + assetName + ": " + e.getMessage());
			return BigDecimal.ZERO;
		}
		
	}

	//////////////////////////////
	/// STOCK PRICE LAST CLOSE ///
	//////////////////////////////

	public static BigDecimal getStockPriceLastClose(String assetName) {
		try {
			BigDecimal yClose = null;
			LocalDate yesterday = LocalDate.now().minusDays(1);
			//if mode is dummy 
			if (STOCK_MODE.equals("DUMMY")) {

				String json = restTemplate.getForObject(DUMMY_API_URL + assetName, String.class);

				JSONObject priceData = new JSONObject(json).getJSONObject("price_data");
				JSONArray closeArr = priceData.getJSONArray("close");
				JSONArray tsArr = priceData.getJSONArray("timestamp");
			

				for (int i = tsArr.length() - 1; i >= 0; i--) {
					LocalDate date = LocalDate.parse(tsArr.getString(i).substring(0, 10));
					if (date.equals(yesterday) && !"NaN".equals(closeArr.optString(i))) {
						yClose = closeArr.getBigDecimal(i);
						break;
					}
				}

				if (yClose == null && closeArr.length() >= 2) {
					yClose = closeArr.getBigDecimal(closeArr.length() - 2);
				}

				
			}

			else if(STOCK_MODE.equals("YAHOO")) {

				String url = YAHOO_API_URL + assetName + "?interval=1d&range=5d";
				String json = fetchYahooJson(url);

				JSONObject result = new JSONObject(json)
						.getJSONObject("chart")
						.getJSONArray("result")
						.getJSONObject(0);

				JSONArray timestamps = result.getJSONArray("timestamp");
				JSONArray closes = result.getJSONObject("indicators")
						.getJSONArray("quote")
						.getJSONObject(0)
						.getJSONArray("close");
						
				
				for (int i = timestamps.length() - 1; i >= 0; i--) {
					LocalDate date = Instant.ofEpochSecond(timestamps.getLong(i))
							.atZone(ZoneId.systemDefault())
							.toLocalDate();

					if (date.equals(yesterday) && !closes.isNull(i)) {
						yClose = closes.getBigDecimal(i);
						break;
					}
				}

				if (yClose == null && closes.length() >= 2) {
					yClose = closes.getBigDecimal(closes.length() - 2);
				}

			}
			
			return yClose != null ? yClose : BigDecimal.ZERO;
		} catch (Exception e) {
			System.err.println("Error fetching stock price for " + assetName + ": " + e.getMessage());
			return BigDecimal.ZERO;
		}
		
	}

	
	/////////////////////////////////////
	/// STOCK PRICE LAST CLOSE for WS ///
	////////////////////////////////////

	// public BigDecimal getStockPriceLastCloseForWS(String assetName) {
	// 	try {
	// 		// First check if we have a recent close price in the database CloseProceHitsory.closePriceTimestamp is yesaterfday return closePrice.
	// 		//elseif CloseProceHitsory.closePriceTimestamp is not yesterday, update date is not today fetch from getStockPriceLastClose() and update the database with new close price and timestamp and return the close price
	// 		//else if assetname only doesntexist in db fetchprice create new entry as a new row in closePriceHistory table and return the close price
	// 		Optional<ClosePriceHistory> optionalCph = closePriceHistoryRepository.findTopByAsset_AssetNameOrderByClosePriceTimestampDesc(assetName);
	// 		if (optionalCph.isPresent()) {
	// 			ClosePriceHistory cph = optionalCph.get();
	// 			if (cph.getClosePriceTimestamp() != null) {
	// 				LocalDate priceDate = cph.getClosePriceTimestamp().atZone(ZoneId.systemDefault()).toLocalDate();
	// 				LocalDate yesterday = LocalDate.now().minusDays(1);
	// 				if (priceDate.equals(yesterday)) {
	// 					return cph.getClosePrice();
	// 				} else if (!priceDate.equals(LocalDate.now())) {
	// 					BigDecimal newClosePrice = getStockPriceLastClose(assetName);
	// 					cph.setClosePrice(newClosePrice);
	// 					cph.setClosePriceTimestamp(Instant.now());
	// 					closePriceHistoryRepository.save(cph);
	// 					return newClosePrice;
	// 				}
	// 			} else {
	// 				BigDecimal newClosePrice = getStockPriceLastClose(assetName);
	// 				cph.setClosePrice(newClosePrice);
	// 				cph.setClosePriceTimestamp(Instant.now());
	// 				closePriceHistoryRepository.save(cph);
	// 				return newClosePrice;
	// 			}
	// 		} else {
	// 			BigDecimal newClosePrice = getStockPriceLastClose(assetName);
	// 			ClosePriceHistory newCph = new ClosePriceHistory();
	// 			newCph.setAsset(new Asset(assetName)); // Assuming you have a constructor or method to set the asset by name
	// 			newCph.setClosePrice(newClosePrice);
	// 			newCph.setClosePriceTimestamp(Instant.now());
	// 			closePriceHistoryRepository.save(newCph);
	// 			return newClosePrice;
	// 		}
	// 	}
	// catch (Exception e) {
	// 	System.err.println("Error fetching stock price for " + assetName + ": " + e.getMessage());
	// 	return BigDecimal.ZERO;
	// }
	// }

	////////////////////
	/// CRYPTO PRICE ///
	////////////////////

	public static BigDecimal getCryptoPrice(String assetName) {
		if (CRYPTO_MODE.equals("DUMMY")) {
			if (assetName.equalsIgnoreCase("ETH-USD")) {
				return DUMMY_ETH_PRICE;
			} else if (assetName.equalsIgnoreCase("BTC-USD")) {
				return DUMMY_BTC_PRICE;
			}
		} else if (CRYPTO_MODE.equals("YAHOO")) {
			return getStockPrice(assetName);
		}
		return BigDecimal.ZERO;
	}

	///////////////////////////////
	/// CRYPTO PRICE LAST CLOSE ///
	///////////////////////////////
	
	public static BigDecimal getCryptoPriceLastClose(String assetName) {
		if (CRYPTO_MODE.equals("DUMMY")) {
			if (assetName.equalsIgnoreCase("ETH-USD")) {
				return DUMMY_ETH_PRICE.multiply(BigDecimal.valueOf(0.95)); 
			} else if (assetName.equalsIgnoreCase("BTC-USD")) {
				return DUMMY_BTC_PRICE.multiply(BigDecimal.valueOf(0.95)); 
			}
		} else if (CRYPTO_MODE.equals("YAHOO")) {
			return getStockPriceLastClose(assetName);
		}
		return BigDecimal.ZERO;
	}

	////////////////////////
	/// COMMODITY PRICE ///
	////////////////////////

	public static BigDecimal getCommodityPrice(String assetName) {
		if (assetName.equalsIgnoreCase("GOLD")) {
			return GOLD_PRICE;
		}
		return BigDecimal.ZERO;
	}

	///////////////////////////////////
	/// COMMODITY PRICE LAST CLOSE  ///
	///////////////////////////////////

	public static BigDecimal getCommodityPriceLastClose(String assetName) {
		if (assetName.equalsIgnoreCase("GOLD")) {
			return GOLD_PRICE.multiply(BigDecimal.valueOf(0.95)); // Simulate a 5% drop yesterday
		}
		return BigDecimal.ZERO;
	}

	 public static void main(String[] args) {
        String ticker = "AAPL"; // try XAUUSD=X, AAPL, ETH-USD

        System.out.println("Current = " + getStockPrice(ticker));
		System.out.println("Last Close = " + getStockPriceLastClose(ticker));

		System.out.println("Current Crypto = " + getCryptoPrice("ETH-USD"));
		System.out.println("Last Close Crypto = " + getCryptoPriceLastClose("ETH-USD"));

		System.out.println("Current Commodity = " + getCommodityPrice("GOLD"));
		System.out.println("Last Close Commodity = " + getCommodityPriceLastClose("GOLD"));

		
    }
}
