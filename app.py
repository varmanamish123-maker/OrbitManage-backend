import os
from typing import Any, Dict, List, Optional

import pandas as pd
import requests
import yfinance as yf
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field


DEFAULT_HOLDINGS_URL = os.getenv("HOLDINGS_URL", "https://orbitmanage-backend-production.up.railway.app")
DEFAULT_MAX_TICKERS = 5
DEFAULT_START = "2022-01-01"
DEFAULT_END = "2024-01-01"

app = FastAPI(title="Portfolio Diversification API", version="1.0.0")


class DiversifyRequest(BaseModel):
	x: int = Field(..., ge=1, description="Number of tickers to select")
	holdings_url: Optional[str] = Field(
		default=None, description="Override Spring Boot /holdings endpoint"
	)
	max_tickers: int = Field(
		default=DEFAULT_MAX_TICKERS,
		ge=1,
		le=10,
		description="Cap tickers passed to yfinance",
	)
	start: str = Field(default=DEFAULT_START, description="Price history start date")
	end: str = Field(default=DEFAULT_END, description="Price history end date")


class DiversifyResponse(BaseModel):
	selected: List[str]
	used_tickers: List[str]
	diversity_score: float


def _extract_tickers(payload: Any) -> List[str]:
	if isinstance(payload, list):
		if all(isinstance(item, str) for item in payload):
			return [item.strip().upper() for item in payload if item.strip()]
		if all(isinstance(item, dict) for item in payload):
			tickers = []
			for item in payload:
				symbol = item.get("ticker") or item.get("symbol") or item.get("code")
				if symbol:
					tickers.append(str(symbol).strip().upper())
			return [t for t in tickers if t]
	if isinstance(payload, dict):
		embedded = payload.get("holdings") or payload.get("items") or payload.get("data")
		if embedded is not None:
			return _extract_tickers(embedded)
	return []


def _fetch_holdings(holdings_url: str) -> List[str]:
	try:
		response = requests.get(holdings_url, timeout=10)
	except requests.RequestException as exc:
		raise HTTPException(status_code=502, detail=f"Holdings request failed: {exc}")

	if response.status_code != 200:
		raise HTTPException(
			status_code=502,
			detail=f"Holdings endpoint error: {response.status_code}",
		)

	tickers = _extract_tickers(response.json())
	if not tickers:
		raise HTTPException(status_code=422, detail="No tickers found in holdings")
	return tickers


def _fetch_returns(tickers: List[str], start: str, end: str) -> pd.DataFrame:
	if not tickers:
		raise HTTPException(status_code=422, detail="No tickers to download")
	data = yf.download(
		tickers,
		start=start,
		end=end,
		auto_adjust=True,
		progress=False,
	)["Close"]

	if isinstance(data, pd.Series):
		data = data.to_frame()

	returns = data.pct_change().dropna(how="all")
	if returns.empty:
		raise HTTPException(status_code=422, detail="Insufficient price data")
	return returns


def _diversity_matrix(returns: pd.DataFrame) -> pd.DataFrame:
	corr = returns.corr().fillna(0.0)
	return 1.0 - corr


def _score_set(distance: pd.DataFrame, tickers: List[str]) -> float:
	if len(tickers) <= 1:
		return 0.0
	sub = distance.loc[tickers, tickers]
	values = sub.values
	upper = values[~pd.DataFrame(values).astype(bool).values]
	pairs = len(tickers) * (len(tickers) - 1) / 2
	if pairs == 0:
		return 0.0
	return float(sub.values.sum() / (2 * pairs))


def _greedy_diverse(distance: pd.DataFrame, x: int) -> List[str]:
	tickers = list(distance.columns)
	if x >= len(tickers):
		return tickers

	avg_scores = distance.mean(axis=1)
	selected = [avg_scores.idxmax()]

	while len(selected) < x:
		remaining = [t for t in tickers if t not in selected]
		best = None
		best_score = -1.0
		for candidate in remaining:
			score = distance.loc[candidate, selected].mean()
			if score > best_score:
				best_score = score
				best = candidate
		if best is None:
			break
		selected.append(best)

	return selected


def _top_diverse(distance: pd.DataFrame, x: int) -> List[str]:
	scores = distance.mean(axis=1).sort_values(ascending=False)
	return list(scores.head(x).index)


@app.post("/diverse", response_model=DiversifyResponse)
def pick_diverse(request: DiversifyRequest) -> DiversifyResponse:
	holdings_url = request.holdings_url or DEFAULT_HOLDINGS_URL
	tickers = _fetch_holdings(holdings_url)
	limited = tickers[: request.max_tickers]
	returns = _fetch_returns(limited, request.start, request.end)
	distance = _diversity_matrix(returns)

	x = min(request.x, len(distance.columns))
	selected = _greedy_diverse(distance, x)
	score = _score_set(distance, selected)

	return DiversifyResponse(
		selected=selected,
		used_tickers=limited,
		diversity_score=score,
	)


@app.post("/diverse/top", response_model=DiversifyResponse)
def pick_top_diverse(request: DiversifyRequest) -> DiversifyResponse:
	holdings_url = request.holdings_url or DEFAULT_HOLDINGS_URL
	tickers = _fetch_holdings(holdings_url)
	limited = tickers[: request.max_tickers]
	returns = _fetch_returns(limited, request.start, request.end)
	distance = _diversity_matrix(returns)

	x = min(request.x, len(distance.columns))
	selected = _top_diverse(distance, x)
	score = _score_set(distance, selected)

	return DiversifyResponse(
		selected=selected,
		used_tickers=limited,
		diversity_score=score,
	)
