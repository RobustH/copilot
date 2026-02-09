package com.alibaba.cloud.ai.copilot.knowledge.splitter.impl;

import com.alibaba.cloud.ai.copilot.knowledge.splitter.DocumentSplitter;
import com.alibaba.cloud.ai.copilot.knowledge.splitter.SplitterStrategy;

import com.alibaba.cloud.ai.copilot.knowledge.model.KnowledgeCategory;
import com.alibaba.cloud.ai.copilot.knowledge.model.KnowledgeChunk;
import com.alibaba.cloud.ai.transformer.splitter.SentenceSplitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 基于句子的文档切割器
 * 使用 Spring AI Alibaba 的 SentenceSplitter
 * 
 * SentenceSplitter 特点:
 * - 基于 OpenNLP 的 SentenceDetectorME 实现
 * - 精准识别句子边界，特别适合中文和多语言文本
 * - 按句子聚合，保留语义完整性
 * - 适用于 RAG 场景，提升检索准确性
 * 
 * 应用场景:
 * - 长文本文档（技术文档、论文、报告等）
 * - 需要保持语义完整性的文本
 * - 中文和多语言混合文本
 * - RAG 检索增强生成场景
 *
 * @author RobustH
 */
@Slf4j
@Component
public class SentenceDocumentSplitter implements DocumentSplitter {

    private final SentenceSplitter sentenceSplitter;

    @Override
    public List<KnowledgeChunk> split(String content, String filePath) {
        try {
            // 创建 Spring AI Document
            Document document = new Document(content, Map.of("source", filePath));

            // 使用 SentenceSplitter 切割
            // SentenceSplitter 会：
            // 1. 使用 OpenNLP 模型识别句子边界
            // 2. 按最大 token 数聚合句子
            // 3. 保持语义完整性
            List<Document> splitDocs = sentenceSplitter.apply(List.of(document));

            log.debug("文件 {} 使用 SentenceSplitter 切割为 {} 个 chunks", filePath, splitDocs.size());

            // 转换为 KnowledgeChunk
            return splitDocs.stream()
                    .filter(doc -> !doc.getText().trim().isEmpty())
                    .map(doc -> createKnowledgeChunk(doc, filePath))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("SentenceSplitter 切割失败: {}", filePath, e);
            // 降级：返回整个文档作为单个 chunk
            return List.of(createKnowledgeChunk(content, filePath));
        }
    }

    @Override
    public SplitterStrategy getStrategy() {
        return SplitterStrategy.SENTENCE;
    }

    /**
     * 默认构造函数
     * 使用默认的最大 token 数 (通常为 800)
     */
    public SentenceDocumentSplitter() {
        this.sentenceSplitter = new SentenceSplitter();
    }

    /**
     * 自定义最大 token 数
     * 
     * @param maxTokens 每个 chunk 的最大 token 数
     */
    public SentenceDocumentSplitter(int maxTokens) {
        this.sentenceSplitter = new SentenceSplitter(maxTokens);
    }

    private KnowledgeChunk createKnowledgeChunk(Document doc, String filePath) {
        return KnowledgeChunk.builder()
                .id(UUID.randomUUID().toString())
                .content(doc.getText())
                .filePath(filePath)
                .fileType(KnowledgeCategory.FileType.DOCUMENT)
                .language(detectLanguage(doc.getText()))
                .createdAt(System.currentTimeMillis())
                .build();
    }

    private KnowledgeChunk createKnowledgeChunk(String content, String filePath) {
        return KnowledgeChunk.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .filePath(filePath)
                .fileType(KnowledgeCategory.FileType.DOCUMENT)
                .language(detectLanguage(content))
                .createdAt(System.currentTimeMillis())
                .build();
    }

    /**
     * 简单的语言检测
     * 根据内容中的字符判断是否包含中文
     */
    private String detectLanguage(String content) {
        if (content == null || content.isEmpty()) {
            return "unknown";
        }
        
        // 检测是否包含中文字符
        boolean hasChinese = content.chars()
                .anyMatch(c -> Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
        
        return hasChinese ? "zh" : "en";
    }


}
