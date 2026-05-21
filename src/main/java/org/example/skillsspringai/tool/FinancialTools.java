package org.example.skillsspringai.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class FinancialTools {

    private final PythonScriptExecutor executor;

    @Tool(description = "DCF现金流折现估值，参数：cashFlows、discountRate、terminalGrowth、sharesOutstanding")
    public String calculateDcf(double[] cashFlows, double discountRate, double terminalGrowth, double sharesOutstanding) {
        return executor.executeDcfCalculator(cashFlows, discountRate, terminalGrowth, sharesOutstanding).getOutput();
    }

    @Tool(description = "计算技术指标 MA、RSI、MACD")
    public String calculateTechnicalIndicators(double[] prices) {
        return executor.executeTechnicalIndicators(prices).getOutput();
    }

    @Tool(description = "计算投资组合VaR风险价值")
    public String calculateVar(double portfolioValue, double meanReturn, double stdDev, double confidenceLevel) {
        return executor.executeVarCalculator(portfolioValue, meanReturn, stdDev, confidenceLevel).getOutput();
    }

    @Tool(description = "马科维茨投资组合优化")
    public String optimizePortfolio(String[] assets, double[] expectedReturns, double riskFreeRate) {
        return executor.executePortfolioOptimizer(assets, expectedReturns, riskFreeRate).getOutput();
    }
}