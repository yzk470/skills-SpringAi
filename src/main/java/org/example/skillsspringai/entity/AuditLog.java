package org.example.skillsspringai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("session_id")
    private String sessionId;
    @TableField("skill_name")
    private String skillName;
    @TableField("user_message")
    private String userMessage;
    @TableField("agent_response")
    private String agentResponse;
    private String evaluation;
    private LocalDateTime timestamp;
    @TableField("duration_ms")
    private long durationMs;
}
