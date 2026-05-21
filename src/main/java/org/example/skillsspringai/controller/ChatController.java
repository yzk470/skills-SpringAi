package org.example.skillsspringai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.skillsspringai.framework.Agent;
import org.example.skillsspringai.tool.AgentRegistry;
import org.example.skillsspringai.tool.AuditLogService;
import org.example.skillsspringai.tool.FinancialTools;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
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

        agent.processStream(message, context).subscribe(
                chunk -> {
                    try {
                        emitter.send(SseEmitter.event().name("content").data(Map.of("chunk", chunk)));
                    } catch (IOException e) {
                        log.error("发送消息失败", e);
                    }
                },
                error -> {
                    log.error("流式输出异常", error);
                    emitter.completeWithError(error);
                },
                () -> {
                    try {
                        emitter.send(SseEmitter.event().name("done"));
                    } catch (IOException e) { }
                    emitter.complete();
                }
        );

        return emitter;
    }
}
