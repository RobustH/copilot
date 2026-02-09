package com.alibaba.cloud.ai.copilot.knowledge.splitter.impl;

import com.alibaba.cloud.ai.copilot.knowledge.splitter.DocumentSplitter;
import com.alibaba.cloud.ai.copilot.knowledge.splitter.SplitterStrategy;

import com.alibaba.cloud.ai.copilot.knowledge.model.KnowledgeCategory;
import com.alibaba.cloud.ai.copilot.knowledge.model.KnowledgeChunk;
import com.alibaba.cloud.ai.transformer.splitter.RecursiveCharacterTextSplitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Markdown 文档切割器
 * 使用 Spring AI Alibaba 的 RecursiveCharacterTextSplitter
 * 
 * RecursiveCharacterTextSplitter 特点:
 * - 递归式文本分割，按分隔符优先级切割
 * - 默认分隔符: \n\n, \n, 。, ！, ？, ；, ，, 空格
 * - 针对中文语境优化
 *
 * @author RobustH
 */
@Slf4j
@Component
public class MarkdownSplitter implements DocumentSplitter {

    private final RecursiveCharacterTextSplitter textSplitter;

    @Override
    public List<KnowledgeChunk> split(String content, String filePath) {
        try {
            // 使用 RecursiveCharacterTextSplitter 切割文本
            List<String> chunks = textSplitter.splitText(content);

            log.debug("Markdown 文件 {} 切割为 {} 个 chunks", filePath, chunks.size());

            // 转换为 KnowledgeChunk
            return chunks.stream()
                    .filter(chunk -> !chunk.trim().isEmpty()) // 过滤空 chunk
                    .map(chunk -> createKnowledgeChunk(chunk, filePath))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Markdown 切割失败: {}", filePath, e);
            // 降级：返回整个文档作为单个 chunk
            return List.of(createKnowledgeChunk(content, filePath));
        }
    }

    @Override
    public SplitterStrategy getStrategy() {
        return SplitterStrategy.RECURSIVE_CHARACTER;
    }

    public MarkdownSplitter() {
        // 使用默认配置: chunkSize=1024, 默认中文分隔符
        this.textSplitter = new RecursiveCharacterTextSplitter();
    }

    /**
     * 自定义 chunk 大小
     */
    public MarkdownSplitter(int chunkSize) {
        this.textSplitter = new RecursiveCharacterTextSplitter(chunkSize);
    }

    /**
     * 完全自定义配置
     */
    public MarkdownSplitter(int chunkSize, String[] separators) {
        this.textSplitter = new RecursiveCharacterTextSplitter(chunkSize, separators);
    }

    private KnowledgeChunk createKnowledgeChunk(String content, String filePath) {
        return KnowledgeChunk.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .filePath(filePath)
                .fileType(KnowledgeCategory.FileType.DOCUMENT)
                .language("markdown")
                .createdAt(System.currentTimeMillis())
                .build();
    }

}
