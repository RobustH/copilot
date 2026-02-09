package com.alibaba.cloud.ai.copilot.knowledge.service;

import com.alibaba.cloud.ai.copilot.knowledge.model.KnowledgeCategory;
import com.alibaba.cloud.ai.copilot.knowledge.model.KnowledgeChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识库向量存储服务
 * 使用 Spring AI VectorStore 抽象
 *
 * @author RobustH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeVectorStoreService {

    private final VectorStore vectorStore;

    /**
     * 添加知识块到向量库
     */
    public void addKnowledge(String userId, KnowledgeChunk chunk) {
        Document document = convertToDocument(userId, chunk);
        vectorStore.add(List.of(document));
        log.info("已添加知识块: 用户ID={}, 文件路径={}", userId, chunk.getFilePath());
    }

    /**
     * 批量添加知识块
     */
    public void addKnowledgeBatch(String userId, List<KnowledgeChunk> chunks) {
        List<Document> documents = chunks.stream()
                .map(chunk -> convertToDocument(userId, chunk))
                .collect(Collectors.toList());
        
        vectorStore.add(documents);
        log.info("已批量添加 {} 个知识块, 用户: {}", chunks.size(), userId);
    }

    /**
     * 搜索相关知识
     */
    public List<Document> searchKnowledge(String userId, String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.5)  // 降低阈值以提高召回率
                .filterExpression(String.format("user_id == '%s'", userId))
                .build();

        return vectorStore.similaritySearch(request);
    }

    /**
     * 搜索指定文件类型的知识
     */
    public List<Document> searchKnowledgeByFileType(
            String userId, 
            String query, 
            KnowledgeCategory.FileType fileType,
            int topK) {
        
        String filter = String.format(
                "user_id == '%s' && file_type == '%s'", 
                userId, 
                fileType.name()
        );

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.5)  // 降低阈值以提高召回率
                .filterExpression(filter)
                .build();

        return vectorStore.similaritySearch(request);
    }

    /**
     * 删除用户的所有知识
     */
    public void deleteUserKnowledge(String userId) {
        // Spring AI VectorStore 暂不支持批量删除,需要通过 Milvus SDK 直接操作
        log.warn("Spring AI VectorStore 暂不完全支持删除操作");
    }

    /**
     * 将 KnowledgeChunk 转换为 Spring AI Document
     */
    private Document convertToDocument(String userId, KnowledgeChunk chunk) {
        return Document.builder()
                .id(chunk.getId())
                .text(chunk.getContent())
                .metadata("user_id", userId)
                .metadata("file_path", chunk.getFilePath())
                .metadata("file_type", chunk.getFileType().name())
                .metadata("language", chunk.getLanguage())
                .metadata("start_line", chunk.getStartLine())
                .metadata("end_line", chunk.getEndLine())
                .metadata("created_at", chunk.getCreatedAt())
                .build();
    }
}

