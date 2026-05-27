package org.example.skillsspringai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.skillsspringai.entity.AuditLog;
import org.example.skillsspringai.framework.Agent;
import org.example.skillsspringai.tool.AgentRegistry;
import org.example.skillsspringai.tool.AuditLogService;
import org.example.skillsspringai.tool.FinancialTools;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final AgentRegistry agentRegistry;
    private final FinancialTools financialTools;
    private final AuditLogService auditLogService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(
            @RequestParam String agentName,
            @RequestParam String message,
            @RequestParam(defaultValue = "default-session") String sessionId) {

        Agent agent = agentRegistry.getAgentByName(agentName).orElse(null);

        if (agent == null) {
            return Flux.just("[ERROR] Agent not found: " + agentName);
        }

        Map<String, Object> context = new HashMap<>();
        context.put("sessionId", sessionId);
        context.put("tools", financialTools);

        StringBuffer fullResponse = new StringBuffer();
        long startTime = System.currentTimeMillis();

        return agent.processStream(message, context)
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    log.info("SSE stream completed: session={}, size={} chars", sessionId, fullResponse.length());
                    saveAuditLog(sessionId, agentName, message, fullResponse.toString(), startTime);
                })
                .onErrorResume(error -> {
                    log.error("流式输出异常: session={}", sessionId, error);
                    saveAuditLog(sessionId, agentName, message, fullResponse.toString(), startTime);
                    return Flux.just("[ERROR] " + buildUserFriendlyError(error.getMessage()));
                });
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> chat(
            @RequestParam String agentName,
            @RequestParam String message,
            @RequestParam(defaultValue = "default-session") String sessionId) {

        Agent agent = agentRegistry.getAgentByName(agentName).orElse(null);
        if (agent == null) {
            return Mono.just(Map.of("error", "Agent not found: " + agentName));
        }

        Map<String, Object> context = new HashMap<>();
        context.put("sessionId", sessionId);
        context.put("tools", financialTools);

        long startTime = System.currentTimeMillis();

        return agent.processStream(message, context)
                .collectList()
                .map(chunks -> {
                    String fullResponse = String.join("", chunks);
                    saveAuditLog(sessionId, agentName, message, fullResponse, startTime);
                    return Map.of(
                            "content", (Object) fullResponse,
                            "agentName", agentName,
                            "sessionId", sessionId,
                            "durationMs", System.currentTimeMillis() - startTime
                    );
                })
                .timeout(Duration.ofSeconds(180))
                .onErrorResume(e -> {
                    log.error("非流式输出异常", e);
                    String userMsg = buildUserFriendlyError(e.getMessage());
                    return Mono.just(Map.of("error", userMsg));
                });
    }

    private String buildUserFriendlyError(String rawMsg) {
        if (rawMsg == null) return "未知错误，请重试";
        if (rawMsg.contains("Connection reset") || rawMsg.contains("SocketException")) {
            return "DeepSeek API 连接被重置，网络波动或服务端限流导致，请稍后重试。";
        }
        if (rawMsg.contains("timeout") || rawMsg.contains("Timeout")) {
            return "请求超时，请简化问题后重试。";
        }
        if (rawMsg.contains("Conversion from JSON")) {
            return "AI模型生成的工具调用参数格式异常（通常是因为脚本内容太长导致JSON截断），请尝试简化请求或分步执行。";
        }
        return "服务异常: " + (rawMsg.length() > 100 ? rawMsg.substring(0, 100) + "..." : rawMsg);
    }

    private void saveAuditLog(String sessionId, String agentName,
                              String userMessage, String agentResponse, long startTime) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .sessionId(sessionId)
                    .skillName(agentName)
                    .userMessage(userMessage)
                    .agentResponse(agentResponse)
                    .timestamp(LocalDateTime.now())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
            auditLogService.logAndEvaluate(auditLog);
            log.info("审计日志已保存: session={}, duration={}ms", sessionId, auditLog.getDurationMs());
        } catch (Exception e) {
            log.error("审计日志保存失败", e);
        }
    }
}
