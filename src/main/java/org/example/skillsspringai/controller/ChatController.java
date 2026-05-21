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

import java.io.IOException;
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

        SseEmitter emitter = new SseEmitter(180000L);
        Agent agent = agentRegistry.getAgentByName(agentName).orElse(null);

        if (agent == null) {
            try {
                emitter.send("Agent not found");
                emitter.complete();
            } catch (IOException e) { }
            return emitter;
        }

        Map<String, Object> context = new HashMap<>();
        context.put("sessionId", sessionId);
        context.put("tools", financialTools);

        StringBuilder fullResponse = new StringBuilder();
        long startTime = System.currentTimeMillis();

        agent.processStream(message, context).subscribe(
                chunk -> {
                    fullResponse.append(chunk);
                    try {
                        emitter.send(SseEmitter.event().name("content").data(Map.of("chunk", chunk)));
                    } catch (IOException e) {
                        log.error("发送消息失败", e);
                    }
                },
                error -> {
                    log.error("流式输出异常", error);
                    saveAuditLog(sessionId, agentName, message, fullResponse.toString(), startTime);
                    emitter.completeWithError(error);
                },
                () -> {
                    try {
                        emitter.send(SseEmitter.event().name("done"));
                    } catch (IOException e) { }
                    emitter.complete();
                    saveAuditLog(sessionId, agentName, message, fullResponse.toString(), startTime);
                }
        );

        return emitter;
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
