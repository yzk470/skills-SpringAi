package org.example.skillsspringai.tool;

import lombok.extern.slf4j.Slf4j;

import org.example.skillsspringai.entity.ScriptResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.io.File;

@Slf4j
@Component
public class PythonScriptExecutor {

    private final String pythonCommand;

    private static final String[] KNOWN_PYTHON_PATHS = {
        "D:\\python\\python3.10.8\\python.exe",
        "D:\\python\\python3.14.2\\python.exe"
    };

    public PythonScriptExecutor() {
        this.pythonCommand = detectPython();
    }

    private String detectPython() {
        for (String path : KNOWN_PYTHON_PATHS) {
            if (new File(path).exists()) {
                log.info("检测到Python: {}", path);
                return path;
            }
        }
        if (testCommand("python3")) return "python3";
        if (testCommand("python")) return "python";
        log.warn("未检测到Python环境");
        return null;
    }

    private boolean testCommand(String command) {
        try {
            Process process = new ProcessBuilder(command, "--version")
                    .redirectErrorStream(true)
                    .start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public ScriptResult execute(String skillPath, String scriptName, String... args) {
        Path script = Paths.get("skills", skillPath, "scripts", scriptName);
        List<String> command = new ArrayList<>();

        command.add(pythonCommand);
        command.add("-u");
        command.add(script.toString());
        Collections.addAll(command, args);

        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            return exitCode == 0 ? ScriptResult.success(output) : ScriptResult.error(output);
        } catch (Exception e) {
            log.error("执行Python失败", e);
            return ScriptResult.error(e.getMessage());
        }
    }

    // DCF 估值
    public ScriptResult executeDcfCalculator(double[] cashFlows, double discountRate, double terminalGrowth, double sharesOutstanding) {
        String[] args = new String[cashFlows.length + 3];
        for (int i = 0; i < cashFlows.length; i++) args[i] = String.valueOf(cashFlows[i]);
        args[cashFlows.length] = String.valueOf(discountRate);
        args[cashFlows.length+1] = String.valueOf(terminalGrowth);
        args[cashFlows.length+2] = String.valueOf(sharesOutstanding);

        return execute("super-financial-advisor", "dcf_calculator.py", args);
    }

    // 技术指标 MA RSI MACD
    public ScriptResult executeTechnicalIndicators(double[] prices) {
        String[] args = new String[prices.length];
        for (int i = 0; i < prices.length; i++) args[i] = String.valueOf(prices[i]);
        return execute("super-financial-advisor", "technical_indicators.py", args);
    }

    // VaR 风险价值
    public ScriptResult executeVarCalculator(double portfolioValue, double meanReturn, double stdDev, double confidenceLevel) {
        return execute("super-financial-advisor", "var_calculator.py",
                String.valueOf(portfolioValue),
                String.valueOf(meanReturn),
                String.valueOf(stdDev),
                String.valueOf(confidenceLevel));
    }

    // 投资组合优化
    public ScriptResult executePortfolioOptimizer(String[] assets, double[] expectedReturns, double riskFreeRate) {
        String[] args = new String[assets.length + expectedReturns.length + 1];
        int idx = 0;
        for (String a : assets) args[idx++] = a;
        for (double r : expectedReturns) args[idx++] = String.valueOf(r);
        args[idx++] = String.valueOf(riskFreeRate);
        return execute("super-financial-advisor", "portfolio_optimizer.py", args);
    }
}
