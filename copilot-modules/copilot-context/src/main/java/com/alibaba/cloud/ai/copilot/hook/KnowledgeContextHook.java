package com.alibaba.cloud.ai.copilot.hook;

import com.alibaba.cloud.ai.copilot.knowledge.service.KnowledgeService;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识上下文 Hook
 * 在模型调用前,自动从知识库检索相关内容并注入到上下文中
 *
 * @author RobustH
 */
@Slf4j
@Component
@HookPositions({HookPosition.BEFORE_MODEL})
@RequiredArgsConstructor
public class KnowledgeContextHook extends MessagesModelHook {

    private final KnowledgeService knowledgeService;

    private static final int MAX_RESULTS = 3;  // 最多注入 3 条知识
    private static final int MIN_QUERY_LENGTH = 5;  // 最小查询长度

    @Override
    public String getName() {
        return "knowledge_context_hook";
    }

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        try {
            // 判断是否是首次用户请求
            boolean isFirstUserRequest = previousMessages.stream()
                    .noneMatch(msg -> msg instanceof AssistantMessage || msg instanceof ToolResponseMessage);

            if (!isFirstUserRequest) {
                // ReactAgent 内部消息流,不干预
                log.debug("ReactAgent 内部消息流,跳过知识上下文注入");
                return new AgentCommand(previousMessages);
            }

            // 获取 userId
            String userId = getUserId(config);
            if (userId == null) {
                log.debug("未找到 userId,跳过知识上下文注入");
                return new AgentCommand(previousMessages);
            }

            // 提取用户查询
            String userQuery = extractUserQuery(previousMessages);
            if (userQuery == null || userQuery.length() < MIN_QUERY_LENGTH) {
                log.debug("用户查询为空或太短,跳过知识上下文注入");
                return new AgentCommand(previousMessages);
            }

            // 搜索相关知识
            List<Document> knowledgeDocs = knowledgeService.search(userId, userQuery, MAX_RESULTS);
            if (knowledgeDocs.isEmpty()) {
                log.debug("未找到相关知识,跳过上下文注入: query={}", userQuery);
                return new AgentCommand(previousMessages);
            }

            // 格式化知识上下文
            String knowledgeContext = knowledgeService.formatAsContext(knowledgeDocs);
            if (knowledgeContext == null || knowledgeContext.trim().isEmpty()) {
                return new AgentCommand(previousMessages);
            }

            // 构建上下文消息
            SystemMessage contextMessage = new SystemMessage(
                    "## 用户项目上下文\n\n" +
                    "以下是从用户知识库中检索到的相关内容,可以帮助你更好地理解用户的项目:\n\n" +
                    knowledgeContext + "\n\n" +
                    "请基于这些上下文信息回答用户的问题。"
            );

            // 在消息列表开头注入上下文 (在 SystemMessage 之后,UserMessage 之前)
            List<Message> updatedMessages = injectContext(previousMessages, contextMessage);

            log.info("已注入知识上下文: userId={}, 知识块数={}, 查询={}", 
                    userId, knowledgeDocs.size(), userQuery);

            return new AgentCommand(updatedMessages, UpdatePolicy.REPLACE);

        } catch (Exception e) {
            log.error("知识上下文注入失败", e);
            return new AgentCommand(previousMessages);
        }
    }

    /**
     * 从配置中获取用户 ID
     */
    private String getUserId(RunnableConfig config) {
        if (config == null) {
            return null;
        }

        try {
            Object userIdObj = config.metadata("userId");
            if (userIdObj instanceof java.util.Optional<?> optional) {
                userIdObj = optional.orElse(null);
            }
            return userIdObj != null ? userIdObj.toString() : null;
        } catch (Exception e) {
            log.warn("获取 userId 失败", e);
            return null;
        }
    }

    /**
     * 提取用户查询
     */
    private String extractUserQuery(List<Message> messages) {
        // 从后往前找第一个 UserMessage
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg instanceof UserMessage userMsg) {
                return userMsg.getText();
            }
        }
        return null;
    }

    /**
     * 注入上下文消息
     * 策略: 在第一个 SystemMessage 之后插入
     */
    private List<Message> injectContext(List<Message> messages, SystemMessage contextMessage) {
        List<Message> result = new ArrayList<>();
        
        boolean contextInjected = false;
        for (Message msg : messages) {
            result.add(msg);
            
            // 在第一个 SystemMessage 之后插入上下文
            if (!contextInjected && msg instanceof SystemMessage) {
                result.add(contextMessage);
                contextInjected = true;
            }
        }

        // 如果没有 SystemMessage,在开头插入
        if (!contextInjected) {
            result.add(0, contextMessage);
        }

        return result;
    }
}
