package org.example.skillsspringai.framework;

import java.util.Map;

public interface SkillPackage {
    /**
     * 获取技能包名称
     * @return 技能名称，如 "狼人杀Agent"、"天气查询"
     */
    String getName();

    /**
     * 获取技能包描述
     * @return 功能说明，如 "用于查询指定城市的实时天气"
     */
    String getDescription();

    /**
     * 获取技能核心内容/提示词
     * @return 可执行的提示词模板或逻辑描述
     */
    String getContent();

    /**
     * 检查技能是否可用
     * @return true 表示可正常调用，false 表示已禁用/下线
     */
    boolean isAvailable();

    /**
     * 获取技能的扩展配置属性
     * @return 键值对形式的配置，如超时时间、权限等级、依赖服务等
     */
    Map<String, Object> getProperties();
}
