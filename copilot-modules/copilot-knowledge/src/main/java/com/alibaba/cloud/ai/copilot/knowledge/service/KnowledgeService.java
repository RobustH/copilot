package com.alibaba.cloud.ai.copilot.knowledge.service;

import com.alibaba.cloud.ai.copilot.knowledge.model.KnowledgeCategory;
import com.alibaba.cloud.ai.copilot.knowledge.model.KnowledgeChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库服务门面
 * 提供统一的知识库操作接口,供其他模块调用
 *
 * @author RobustH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final DocumentProcessingService documentProcessingService;
    private final KnowledgeVectorStoreService vectorStoreService;

    /**
     * 添加单个文件到知识库
     *
     * @param userId   用户ID
     * @param filePath 文件路径
     * @return 添加的知识块数量
     */
    public int addFile(String userId, String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            log.warn("文件不存在或不是文件: {}", filePath);
            return 0;
        }

        try {
            List<KnowledgeChunk> chunks = documentProcessingService.processFile(file);
            vectorStoreService.addKnowledgeBatch(userId, chunks);
            log.info("已添加文件到知识库: 用户ID={}, 文件={}, 知识块数={}", userId, filePath, chunks.size());
            return chunks.size();
        } catch (Exception e) {
            log.error("添加文件到知识库失败: {}", filePath, e);
            return 0;
        }
    }

    /**
     * 添加目录到知识库 (递归处理所有文件)
     *
     * @param userId        用户ID
     * @param directoryPath 目录路径
     * @return 添加的知识块数量
     */
    public int addDirectory(String userId, String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            log.warn("目录不存在或不是目录: {}", directoryPath);
            return 0;
        }

        try {
            List<KnowledgeChunk> chunks = documentProcessingService.processDirectory(directory);
            vectorStoreService.addKnowledgeBatch(userId, chunks);
            log.info("已添加目录到知识库: 用户ID={}, 目录={}, 知识块数={}", userId, directoryPath, chunks.size());
            return chunks.size();
        } catch (Exception e) {
            log.error("添加目录到知识库失败: {}", directoryPath, e);
            return 0;
        }
    }

    /**
     * 搜索知识库
     *
     * @param userId 用户ID
     * @param query  查询文本
     * @param topK   返回结果数量
     * @return 搜索结果列表
     */
    public List<Document> search(String userId, String query, int topK) {
        return vectorStoreService.searchKnowledge(userId, query, topK);
    }

    /**
     * 按文件类型搜索知识库
     *
     * @param userId   用户ID
     * @param query    查询文本
     * @param fileType 文件类型
     * @param topK     返回结果数量
     * @return 搜索结果列表
     */
    public List<Document> searchByFileType(
            String userId, 
            String query, 
            KnowledgeCategory.FileType fileType,
            int topK) {
        return vectorStoreService.searchKnowledgeByFileType(userId, query, fileType, topK);
    }

    /**
     * 搜索代码文件
     *
     * @param userId 用户ID
     * @param query  查询文本
     * @param topK   返回结果数量
     * @return 搜索结果列表
     */
    public List<Document> searchCode(String userId, String query, int topK) {
        return searchByFileType(userId, query, KnowledgeCategory.FileType.CODE, topK);
    }

    /**
     * 搜索文档文件
     *
     * @param userId 用户ID
     * @param query  查询文本
     * @param topK   返回结果数量
     * @return 搜索结果列表
     */
    public List<Document> searchDocuments(String userId, String query, int topK) {
        return searchByFileType(userId, query, KnowledgeCategory.FileType.DOCUMENT, topK);
    }

    /**
     * 搜索配置文件
     *
     * @param userId 用户ID
     * @param query  查询文本
     * @param topK   返回结果数量
     * @return 搜索结果列表
     */
    public List<Document> searchConfig(String userId, String query, int topK) {
        return searchByFileType(userId, query, KnowledgeCategory.FileType.CONFIG, topK);
    }

    /**
     * 获取搜索结果的内容列表
     *
     * @param documents 搜索结果
     * @return 内容列表
     */
    public List<String> extractContents(List<Document> documents) {
        return documents.stream()
                .map(Document::getText)
                .collect(Collectors.toList());
    }

    /**
     * 将搜索结果格式化为上下文文本
     *
     * @param documents 搜索结果
     * @return 格式化的上下文文本
     */
    public String formatAsContext(List<Document> documents) {
        return documents.stream()
                .map(doc -> {
                    String filePath = (String) doc.getMetadata().get("file_path");
                    String content = doc.getText();
                    return String.format("文件: %s\n内容:\n%s", filePath, content);
                })
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * 删除用户的所有知识
     *
     * @param userId 用户ID
     */
    public void deleteUserKnowledge(String userId) {
        vectorStoreService.deleteUserKnowledge(userId);
        log.info("已删除用户的所有知识: {}", userId);
    }
}
