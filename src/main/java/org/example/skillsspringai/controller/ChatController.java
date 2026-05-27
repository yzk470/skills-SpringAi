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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import reactor.core.publisher.Mono;

import java.io.IOException;
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
    public SseEmitter stream(
            @RequestParam String agentName,
            @RequestParam String message,
            @RequestParam(defaultValue = "default-session") String sessionId) {

        SseEmitter emitter = new SseEmitter(300000L);
        Agent agent = agentRegistry.getAgentByName(agentName).orElse(null);

        if (agent == null) {
            return errorEmitter(emitter, "Agent not found: " + agentName);
        }

        Map<String, Object> context = new HashMap<>();
        context.put("sessionId", sessionId);
        context.put("tools", financialTools);

        StringBuilder fullResponse = new StringBuilder();
        long startTime = System.currentTimeMillis();

        emitter.onCompletion(() -> {
            log.info("SSE stream completed: session={}, size={} chars", sessionId, fullResponse.length());
            saveAuditLog(sessionId, agentName, message, fullResponse.toString(), startTime);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE stream timeout: session={}, sent={} chars", sessionId, fullResponse.length());
            saveAuditLog(sessionId, agentName, message, fullResponse.toString(), startTime);
        });

        emitter.onError(ex -> {
            log.error("SSE stream transport error: session={}", sessionId, ex);
        });

        agent.processStream(message, context).subscribe(
                chunk -> {
                    fullResponse.append(chunk);
                    sendSseEvent(emitter, "content", Map.of("chunk", chunk));
                },
                error -> {
                    log.error("流式输出异常: session={}", sessionId, error);
                    // 先发送已收集的部分内容，再发error事件
                    if (fullResponse.length() > 0) {
                        sendSseEvent(emitter, "partial", Map.of("content", fullResponse.toString()));
                    }
                    String rawMsg = error.getMessage();
                    String userMsg = buildUserFriendlyError(rawMsg);
                    log.error("流式输出异常: session={}, detail={}", sessionId, rawMsg);
                    if (fullResponse.length() > 0) {
                        sendSseEvent(emitter, "partial", Map.of("content", fullResponse.toString()));
                    }
                    sendSseEvent(emitter, "error", Map.of("message", userMsg));
                    saveAuditLog(sessionId, agentName, message, fullResponse.toString(), startTime);
                    emitter.complete();
                },
                () -> {
                    sendSseEvent(emitter, "done", Map.of(
                            "totalChars", fullResponse.length(),
                            "durationMs", System.currentTimeMillis() - startTime
                    ));
                    emitter.complete();
                }
        );

        return emitter;
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            synchronized (emitter) {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            }
        } catch (IOException e) {
            log.error("SSE send failed: event={}", eventName, e);
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
            }
        }
    }

    private SseEmitter errorEmitter(SseEmitter emitter, String errorMsg) {
        try {
            emitter.send(SseEmitter.event().name("error").data(Map.of("message", errorMsg)));
            emitter.complete();
        } catch (IOException ignored) {
        }
        return emitter;
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
