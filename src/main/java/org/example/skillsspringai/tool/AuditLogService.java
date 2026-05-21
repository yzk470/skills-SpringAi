package org.example.skillsspringai.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.skillsspringai.entity.AuditLog;
import org.example.skillsspringai.entity.AuditLogMapper;
import org.example.skillsspringai.entity.Skill;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final ChatClient.Builder chatClientBuilder;
    private final SkillLoaderService skillLoaderService;

    public void logAndEvaluate(AuditLog auditLog) {
        // 调用 critic-agent 自动评估对话质量
        Skill criticSkill = skillLoaderService.findByName("critic-agent").orElse(null);

        if (criticSkill != null) {
            String evaluation = chatClientBuilder.build()
                    .prompt()
                    .system(criticSkill.getFullContent())
                    .user(String.format("用户：%s\nAI：%s",
                            auditLog.getUserMessage(),
                            auditLog.getAgentResponse()))
                    .call()
                    .content();

            auditLog.setEvaluation(evaluation);
        }

        // 保存审计日志
        auditLogMapper.insert(auditLog);
    }
}
