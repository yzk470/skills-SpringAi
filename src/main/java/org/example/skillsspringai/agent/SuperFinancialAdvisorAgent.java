package org.example.skillsspringai.agent;

import lombok.extern.slf4j.Slf4j;

import org.example.skillsspringai.framework.SkillPackage;
import org.example.skillsspringai.framework.impl.BaseAgent;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SuperFinancialAdvisorAgent extends BaseAgent {

    public SuperFinancialAdvisorAgent(
            ChatClient.Builder chatClientBuilder,
            ResourceLoader resourceLoader) {
        super(
                "全能金融顾问大师",
                "我提供投资分析、风控计算、组合优化、技术指标分析",
                chatClientBuilder.build(),
                chatClientBuilder
        );

        try {
            Resource resource = resourceLoader.getResource("classpath:skills/super-financial-advisor");
            setSkillsDirectory(resource.getFile());
        } catch (Exception e) {
            log.error("技能目录加载失败", e);
        }
    }

    @Override
    public void addSkillPackage(SkillPackage skillPackage) {
        this.skillPackages.add(skillPackage);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
