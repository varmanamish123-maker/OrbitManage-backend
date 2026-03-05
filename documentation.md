# Backend API Blueprint (Polished)

Goal: single, predictable contract that fully powers the current frontend without ad‑hoc mapping. All endpoints use the envelope:

```json
{ "success": true, "error": null, "data": { ... } }
```

## Conventions
- Base URL: `http://localhost:8080/api/v1`
- Time: ISO 8601 in UTC.
- Currency: `INR` for portfolio values unless specified; crypto buy/sell accepts a `currency` field.
- Enums:
  - `assetType`: `STOCK` | `CRYPTO` | `COMMODITY` (gold/others)
  - `side`: `BUY` | `SELL`
  - `range`: `1D` | `1W` | `1M` | `3M` | `6M` | `1Y` | `ALL`

## Portfolio Overview
**GET** `/portfolios/{portfolioId}/overview`

Lightweight category-level totals for the landing view and donut (use totalPnl for slice size).

```json
{
  "success": true,
  "data": {
    "stock": {
      "totalPnl": 16945.9991,
      "totalPnlPercent": 165.0135,
      "todayPnl": 38.3791,
      "todayPnlPercent": 0.3737
    },
    "commodity": {
      "totalPnl": 56519.7,
      "totalPnlPercent": 565.197,
      "todayPnl": -54.8,
      "todayPnlPercent": -0.548
    },
    "crypto": {
      "totalPnl": 777252.9063,
      "totalPnlPercent": 258223.5569,
      "todayPnl": 0,
      "todayPnlPercent": 0
    }
  }
}
```

> Frontend use: category cards + donut slices (sum of `totalPnl` by category).

## Portfolio Summary (global)
**GET** `/portfolios/{portfolioId}/summary`

```json
{
  "success": true,
  "data": {
    "totalUnrealisedPnl": 777252.91,
    "totalUnrealisedPnlPercent": 258223.56,
    "totalRealisedPnl": 0,
    "totalRealisedPnlPercent": 0,
    "totalTodayUnrealisedPnl": 0.0,
    "totalTodayUnrealisedPnlPercent": 0.0
  }
}
```

> Frontend use: top stats bar.

## Holdings
**GET** `/portfolios/{portfolioId}/holdings?assetType=STOCK|CRYPTO|COMMODITY` (assetType optional, defaults to all)

Response shape is uniform for all asset types:

```json
{
  "success": true,
  "data": [
    {
      "assetId": 1,
      "assetType": "STOCK",
      "ticker": "AAPL",
      "exchange": "NSE",
      "displayName": "Apple Inc.",
      "totalQty": 101,
      "totalAvgBuyPrice": 101.677822,
      "totalInvested": 101 * 101.677822,
      "currentValue": 101 * 101.677822 + 16945.999114962888,
      "totalPnl": 16945.999114962888,
      "totalPnlPercent": 165.0135,
      "todayPnl": 38.3791369628881,
      "todayPnlPercent": 0.3737,
      "holdings": [
        {
          "holdingId": 1,
          "qty": 100,
          "buyPrice": 100,
          "buyTimestamp": "2026-02-05T04:45:08.964891Z",
          "pnl": 16945.99914550781,
          "pnlPercent": 169.46,
          "todayPnl": 37.99914550781,
          "todayPnlPercent": 0.38
        },
        {
          "holdingId": 5,
          "qty": 1,
          "buyPrice": 269.46,
          "buyTimestamp": "2026-02-05T05:00:25.374002Z",
          "pnl": -0.0000085449219,
          "pnlPercent": 0,
          "todayPnl": 0.3799914550781,
          "todayPnlPercent": 0.141
        }
      ]
    },
    {
      "assetId": 4,
      "assetType": "CRYPTO",
      "displayName": "Crypto",
      "ticker": null,
      "exchange": null,
      "totalQty": 11,
      "totalAvgBuyPrice": 27.363636,
      "totalInvested": 301.0,
      "currentValue": 777553.906254,
      "totalPnl": 777252.906254,
      "totalPnlPercent": 258223.5569,
      "todayPnl": 0,
      "todayPnlPercent": 0,
      "holdings": [
        {
          "holdingId": 8,
          "qty": 1,
          "buyPrice": 291,
          "buyTimestamp": "2026-02-05T05:42:21.118087Z",
          "pnl": 70395.71875,
          "pnlPercent": 24190.9686,
          "todayPnl": 0,
          "todayPnlPercent": 0
        },
        {
          "holdingId": 9,
          "qty": 10,
          "buyPrice": 1,
          "buyTimestamp": "2026-02-05T05:42:37.503735Z",
          "pnl": 706857.1875,
          "pnlPercent": 7068571.875,
          "todayPnl": 0,
          "todayPnlPercent": 0
        }
      ]
    }
  ]
}
```

> Frontend use: holdings table (stocks/crypto/commodities), buy-lots expansion, totals, today P/L, allocation calculations.

## Holdings History (Realised)
**GET** `/portfolios/{portfolioId}/holdings/history?assetType=STOCK|CRYPTO|COMMODITY`

```json
{
  "success": true,
  "data": [
    {
      "holdingId": 1,
      "assetType": "STOCK",
      "ticker": "AAPL",
      "quantity": 100,
      "buyPrice": 100,
      "sellPrice": 125,
      "buyTimestamp": "2026-02-05T04:45:08.964Z",
      "sellTimestamp": "2026-02-06T04:45:08.964Z",
      "profitOrLoss": 2500,
      "profitOrLossPercent": 25.0
    }
  ]
}
```

> Frontend use: realised P/L table (per asset-type filter).

## Transactions (Buy/Sell)
Unified endpoint covers all asset types.

**POST** `/portfolios/{portfolioId}/transactions`

- Stock BUY example
```json
{
  "side": "BUY",
  "assetType": "STOCK",
  "ticker": "AAPL",
  "exchange": "NSE",
  "quantity": 10,
  "price": 102.5,
  "currency": "INR",
  "executedAt": "2026-02-05T05:10:00Z"
}
```

- Commodity (Gold) BUY example
```json
{
  "side": "BUY",
  "assetType": "COMMODITY",
  "name": "Gold",
  "unit": "gram",
  "quantity": 50,
  "price": 6000,
  "currency": "INR",
  "executedAt": "2026-02-05T05:28:24Z"
}
```

- Crypto BUY example
```json
{
  "side": "BUY",
  "assetType": "CRYPTO",
  "name": "Bitcoin",
  "symbol": "BTC",
  "blockchain": "BITCOIN",
  "quantity": 1.5,
  "price": 42000,
  "currency": "USD",
  "executedAt": "2026-02-05T05:42:37Z"
}
```

- SELL (all asset types)
```json
{
  "side": "SELL",
  "assetType": "CRYPTO",        // or STOCK / COMMODITY
  "holdingId": 9,
  "quantity": 1.0,
  "sellPrice": 43000,           // optional if using market price; required for crypto example
  "executedAt": "2026-02-05T06:00:00Z"
}
```

Response:
```json
{ "success": true, "data": { "holdingId": 9, "assetId": 4 } }
```

## Edit / Delete Holdings
- **PATCH** `/portfolios/{portfolioId}/holdings/{holdingId}`
  ```json
  { "quantity": 2.0, "buyPrice": 275.5 }
  ```
- **DELETE** `/portfolios/{portfolioId}/holdings/{holdingId}`

Both return the updated holding summary.

## Portfolio Chart
**GET** `/portfolios/{portfolioId}/chart?range=1M`
```json
{
  "success": true,
  "data": {
    "currency": "INR",
    "series": [
      { "timestamp": "2026-02-05T04:00:00Z", "value": 120000 },
      { "timestamp": "2026-02-05T05:00:00Z", "value": 180000 }
    ]
  }
}
```

> Frontend use: time-series graph.

## Market Data
- **GET** `/market/movers?type=top-gainers|top-losers|most-active`
- **GET** `/market/news/{ticker}`

Responses mirror the current Yahoo proxy; keep the shape intact for drop-in compatibility.

## Error Shape (consistent)
```json
{ "success": false, "error": { "code": "VALIDATION_ERROR", "message": "price must be > 0" } }
```

## Mapping to Frontend Components
- `TopStats`: `/summary`
- `CategoryTable` & donut: `/overview` (category totals) + `/holdings` (per-asset detail; donut slices by totalPnl)
- `HoldingsTableExpandable`: `holdings` endpoint
- Buy/Sell/Edit/Delete modals: `transactions`, `holdings PATCH/DELETE`
- `HoldingsHistoryTable`: `/holdings/history`
- `PortfolioGraph`: `chart`
- `MarketMovers` / `NewsPopup`: `market/movers`, `market/news/{ticker}`

## Migration Notes (from current state)
- Replace three buy endpoints (`/buy/stock|gold|crypto`) with `/transactions` using `assetType`.
- Replace three sell endpoints with `/transactions` using `side: "SELL"`; include `sellPrice` when needed.
- Replace edit endpoints with single `PATCH /holdings/{holdingId}`.
- Replace delete endpoints with single `DELETE /holdings/{holdingId}`.
- Keep `/holdings` as the single source of truth; frontend no longer does type-based massaging.
