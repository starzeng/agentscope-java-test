/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.examples.agui.config;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agui.registry.AguiAgentRegistry;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.examples.agui.tools.ExampleTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that registers agents with the AG-UI registry.
 *
 * <p>This example demonstrates how to register multiple agents with different IDs.
 * Clients can select which agent to use via:
 * <ul>
 *   <li>URL path variable: {@code POST /agui/run/{agentId}}</li>
 *   <li>HTTP header: {@code X-Agent-Id: agentId}</li>
 *   <li>Request body: {@code forwardedProps.agentId}</li>
 * </ul>
 */
@Configuration
public class AgentConfiguration {

    @Autowired
    public void configureAgents(AguiAgentRegistry registry) {
        registry.registerFactory("default", this::createDefaultAgent);
        registry.registerFactory("chat", this::createChatAgent);
        registry.registerFactory("calculator", this::createCalculatorAgent);

        System.out.println("Registered agents with AG-UI registry: default, chat, calculator");
        System.out.println("Access agents via:");
        System.out.println("  - POST /agui/run (uses default-agent-id from config)");
        System.out.println("  - POST /agui/run/chat (uses 'chat' agent)");
        System.out.println("  - POST /agui/run with X-Agent-Id header");
    }

    /**
     * Create the default agent instance.
     *
     * <p>This agent is configured with:
     * <ul>
     *   <li>DashScope qwen-plus model with streaming enabled</li>
     *   <li>Example tools (get_weather, calculate)</li>
     *   <li>In-memory conversation memory</li>
     * </ul>
     */
    private Agent createDefaultAgent() {
        String apiKey = getRequiredApiKey();

        // Create toolkit with example tools
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new ExampleTools());

        // Create the agent
        String sysPrompt = """
                您是一个通过 AG-UI 协议提供的有用 AI 助手。
                您可以帮助用户处理各种任务，包括天气查询和计算。
                请在回复中保持简洁和有帮助。
                """;
        return ReActAgent.builder()
                .name("AG-UI Assistant")
                .sysPrompt(sysPrompt)
                .model(
                        DashScopeChatModel.builder()
                                .apiKey(apiKey)
                                .modelName("qwen-plus")
                                .stream(true)
                                .enableThinking(false)
                                .formatter(new DashScopeChatFormatter())
                                .build())
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .maxIters(10)
                .build();
    }

    /**
     * Create a simple chat agent without tools.
     *
     * <p>This agent is a pure conversational assistant.
     */
    private Agent createChatAgent() {
        String apiKey = getRequiredApiKey();

        String sysPrompt = """
                您是一个友好的对话助手。
                进行自然的对话并帮助用户回答一般性问题和讨论。
                """;
        return ReActAgent.builder()
                .name("Chat Assistant")
                .sysPrompt(sysPrompt)
                .model(
                        DashScopeChatModel.builder()
                                .apiKey(apiKey)
                                .modelName("qwen-plus")
                                .stream(true)
                                .formatter(new DashScopeChatFormatter())
                                .build()
                )
                .memory(new InMemoryMemory())
                .maxIters(1)
                .build();
    }

    /**
     * Create a calculator agent specialized for mathematical operations.
     */
    private Agent createCalculatorAgent() {
        String apiKey = getRequiredApiKey();

        String sysPrompt = """
                您是一个专门从事计算的数学助手。
                使用计算工具执行数学运算。
                始终展示您的计算过程并解释结果。
                """;

        // Create toolkit with only calculation tools
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new ExampleTools());

        return ReActAgent.builder()
                .name("Calculator Agent")
                .sysPrompt(sysPrompt)
                .model(
                        DashScopeChatModel.builder()
                                .apiKey(apiKey)
                                .modelName("qwen-plus")
                                .stream(true)
                                .formatter(new DashScopeChatFormatter())
                                .build()
                )
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .maxIters(5)
                .build();
    }

    private String getRequiredApiKey() {
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException(
                    "DASHSCOPE_API_KEY environment variable is required. Please set it before starting the application."
            );
        }
        return apiKey;
    }
}
