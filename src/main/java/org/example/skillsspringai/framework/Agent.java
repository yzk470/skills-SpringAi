package org.example.skillsspringai.framework;

import reactor.core.publisher.Flux;
import java.util.List;
import java.util.Map;

public interface Agent {
    /** 获取Agent唯一ID */
    String getId();

    /** 获取Agent名称 */
    String getName();

    /** 获取Agent绑定的所有技能包 */
    List<SkillPackage> getSkillPackages();

    /** 为Agent添加一个技能包 */
    void addSkillPackage(SkillPackage skillPackage);

    /**
     * 流式处理用户消息（返回 Flux 支持打字机效果/实时输出）
     * @param message 用户输入消息
     * @param context 会话上下文（可存会话ID、用户信息等）
     * @return 响应字符串流
     */
    Flux<String> processStream(String message, Map<String, Object> context);

    /** 检查Agent是否可用 */
    boolean isAvailable();
}