from __future__ import annotations

from typing import Any, Dict, List, Optional, Tuple

import numpy as np
import pandas as pd
import yfinance as yf
from qiskit.primitives import StatevectorSampler
from qiskit_algorithms import QAOA
from qiskit_algorithms.optimizers import COBYLA
from qiskit_optimization import QuadraticProgram
from qiskit_optimization.algorithms import MinimumEigenOptimizer, OptimizationResult
from qiskit_optimization.algorithms import OptimizationResultStatus
from qiskit_optimization.converters import QuadraticProgramToQubo


def download_prices(
    tickers: List[str],
    start: Optional[str] = None,
    end: Optional[str] = None,
    auto_adjust: bool = True,
    period: Optional[str] = None,
    interval: Optional[str] = None,
) -> pd.DataFrame:
    if period:
        data = yf.download(
            tickers,
            period=period,
            interval=interval,
            auto_adjust=auto_adjust,
            progress=False,
        )["Close"]
    else:
        data = yf.download(
            tickers,
            start=start,
            end=end,
            auto_adjust=auto_adjust,
            progress=False,
        )["Close"]

    if isinstance(data, pd.Series):
        data = data.to_frame()

    return data


def compute_returns(
    data: pd.DataFrame,
) -> Tuple[pd.DataFrame, np.ndarray, np.ndarray]:
    returns = data.pct_change().dropna(how="all")
    if returns.empty:
        raise ValueError("Insufficient price data")
    mu = returns.mean().values
    sigma = returns.cov().values
    return returns, mu, sigma


def build_quadratic_program(
    tickers: List[str],
    mu: np.ndarray,
    sigma: np.ndarray,
    budget: int,
    risk_factor: float,
) -> QuadraticProgram:
    qp = QuadraticProgram("portfolio_optimization")

    for name in tickers:
        qp.binary_var(name=name)

    linear_coeffs = {name: -mu[i] for i, name in enumerate(tickers)}

    quadratic_coeffs: Dict[Tuple[str, str], float] = {}
    for i, name1 in enumerate(tickers):
        for j, name2 in enumerate(tickers):
            if i <= j:
                quadratic_coeffs[(name1, name2)] = float(risk_factor) * float(sigma[i, j])

    qp.minimize(linear=linear_coeffs, quadratic=quadratic_coeffs)

    constraint_coeffs = {name: 1 for name in tickers}
    qp.linear_constraint(
        linear=constraint_coeffs,
        sense="==",
        rhs=budget,
        name="budget",
    )

    return qp


def to_qubo(
    qp: QuadraticProgram,
    penalty: float,
) -> Tuple[QuadraticProgramToQubo, QuadraticProgram]:
    converter = QuadraticProgramToQubo(penalty=penalty)
    qubo = converter.convert(qp)
    return converter, qubo


def create_qaoa(reps: int = 3, maxiter: int = 100) -> QAOA:
    optimizer = COBYLA(maxiter=maxiter)
    return QAOA(sampler=StatevectorSampler(), optimizer=optimizer, reps=reps)


def run_qaoa(
    qubo: QuadraticProgram,
    qaoa: QAOA,
) -> Tuple[OptimizationResult, object, float, str, float]:
    operator, offset = qubo.to_ising()
    qaoa_result = qaoa.compute_minimum_eigenvalue(operator)

    bitstring = qaoa_result.best_measurement["bitstring"]
    objective = float(qaoa_result.best_measurement["value"])
    x_array = [int(bit) for bit in bitstring]

    result = OptimizationResult(
        x=x_array,
        fval=qaoa_result.best_measurement["value"] + offset,
        variables=qubo.variables,
        status=OptimizationResultStatus.SUCCESS,
    )

    return result, qaoa_result, offset, bitstring, objective


def extract_selected(tickers: List[str], result: OptimizationResult) -> List[str]:
    return [tickers[i] for i in range(len(tickers)) if result.x[i] == 1]


def portfolio_stats(
    tickers: List[str],
    mu: np.ndarray,
    sigma: np.ndarray,
    selected: List[str],
) -> Dict[str, float]:
    if not selected:
        return {"return": 0.0, "volatility": 0.0, "sharpe": 0.0}

    portfolio_return = sum(mu[tickers.index(s)] for s in selected) / len(selected)
    portfolio_indices = [tickers.index(s) for s in selected]
    portfolio_covariance = sigma[np.ix_(portfolio_indices, portfolio_indices)]
    portfolio_variance = np.mean(portfolio_covariance)
    portfolio_volatility = float(np.sqrt(portfolio_variance))
    sharpe = portfolio_return / portfolio_volatility if portfolio_volatility else 0.0

    return {
        "return": float(portfolio_return),
        "volatility": portfolio_volatility,
        "sharpe": float(sharpe),
    }


def naive_portfolio_stats(
    tickers: List[str],
    mu: np.ndarray,
    sigma: np.ndarray,
    count: int = 3,
) -> Dict[str, float]:
    naive_portfolio = tickers[:count]
    return portfolio_stats(tickers, mu, sigma, naive_portfolio)


def risk_sweep(
    tickers: List[str],
    mu: np.ndarray,
    sigma: np.ndarray,
    budget: int,
    risk_factors: List[float],
    qaoa: QAOA,
    penalty: float,
) -> List[Dict[str, object]]:
    converter = QuadraticProgramToQubo(penalty=penalty)
    min_eigen_optimizer = MinimumEigenOptimizer(qaoa)

    results: List[Dict[str, object]] = []
    for rf in risk_factors:
        qp_test = build_quadratic_program(tickers, mu, sigma, budget, rf)
        qubo_test = converter.convert(qp_test)
        result_test = min_eigen_optimizer.solve(qubo_test)

        selected = extract_selected(tickers, result_test)
        stats = portfolio_stats(tickers, mu, sigma, selected)

        results.append(
            {
                "risk_factor": rf,
                "stocks": selected,
                "return": stats["return"],
                "volatility": stats["volatility"],
                "sharpe": stats["sharpe"],
            }
        )

    return results


def run_pipeline(params: Dict[str, Any]) -> Dict[str, Any]:
    tickers = params.get("tickers") or []
    period = params.get("period")
    interval = params.get("interval")
    start = params.get("start")
    end = params.get("end")
    budget = int(params.get("budget", 1))
    risk_factor = float(params.get("riskFactor", 0.5))
    penalty = float(params.get("penalty", len(tickers)))
    show_matrices = bool(params.get("showMatrices", False))

    if not tickers:
        raise ValueError("Tickers list must be provided")

    data = download_prices(
        tickers=tickers,
        start=start,
        end=end,
        period=period,
        interval=interval,
    )
    returns, mu, sigma = compute_returns(data)

    qp = build_quadratic_program(tickers, mu, sigma, budget, risk_factor)
    _, qubo = to_qubo(qp, penalty)
    qaoa = create_qaoa()
    result, _, _, bitstring, objective = run_qaoa(qubo, qaoa)
    selected = extract_selected(tickers, result)

    stats_rows = []
    returns_mean = returns.mean()
    returns_vol = returns.std()
    for ticker in tickers:
        stats_rows.append(
            {
                "ticker": ticker,
                "meanReturn": float(returns_mean.get(ticker, 0.0)),
                "volatility": float(returns_vol.get(ticker, 0.0)),
            }
        )

    selected_set = set(selected)
    selected_stats = [row for row in stats_rows if row["ticker"] in selected_set]
    if selected_stats:
        avg_sel_return = sum(row["meanReturn"] for row in selected_stats) / len(selected_stats)
        avg_sel_vol = sum(row["volatility"] for row in selected_stats) / len(selected_stats)
        avg_all_return = sum(row["meanReturn"] for row in stats_rows) / len(stats_rows)
        avg_all_vol = sum(row["volatility"] for row in stats_rows) / len(stats_rows)

        return_bias = "higher" if avg_sel_return >= avg_all_return else "lower"
        risk_bias = "higher" if avg_sel_vol >= avg_all_vol else "lower"

        conclusion = (
            f"Selected {len(selected)} of {len(tickers)} stocks. "
            f"On average, the selected set has {return_bias} expected return "
            f"and {risk_bias} volatility compared to the full list."
        )
    else:
        conclusion = "No stocks were selected; unable to summarize risk and return trade-offs."

    response: Dict[str, Any] = {
        "tickers": tickers,
        "qubo": {
            "budget": budget,
            "riskFactor": risk_factor,
            "penalty": penalty,
        },
        "qaoa": {
            "selected": selected,
            "bitstring": bitstring,
            "objective": float(objective),
        },
        "stats": stats_rows,
        "conclusion": conclusion,
    }

    if show_matrices:
        response["mu"] = mu.tolist()
        response["sigma"] = sigma.tolist()

    return response


def main() -> int:
    import json
    import sys

    raw = sys.stdin.read().strip()
    if not raw:
        print("No input provided", file=sys.stderr)
        return 1

    try:
        params = json.loads(raw)
        result = run_pipeline(params)
        print(json.dumps(result))
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
