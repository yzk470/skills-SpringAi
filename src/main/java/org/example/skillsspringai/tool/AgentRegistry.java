package org.example.skillsspringai.tool;


import org.example.skillsspringai.framework.Agent;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentRegistry {

    private final Map<String, Agent> agentMap = new ConcurrentHashMap<>();

    // 构造时自动注入所有 Agent
    public AgentRegistry(List<Agent> agents) {
        for (Agent agent : agents) {
            agentMap.put(agent.getName(), agent);
        }
    }

    public Optional<Agent> getAgentByName(String name) {
        return Optional.ofNullable(agentMap.get(name));
    }

    public Map<String, Agent> getAllAgents() {
        return agentMap;
    }
}
