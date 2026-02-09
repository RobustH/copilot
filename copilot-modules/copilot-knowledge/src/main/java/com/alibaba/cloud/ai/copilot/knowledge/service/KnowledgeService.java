package com.alibaba.cloud.ai.copilot.knowledge.service;

import com.alibaba.cloud.ai.copilot.knowledge.model.KnowledgeCategory;
import com.alibaba.cloud.ai.copilot.knowledge.model.KnowledgeChunk;
import com.alibaba.cloud.ai.copilot.knowledge.splitter.DocumentSplitter;
import com.alibaba.cloud.ai.copilot.knowledge.splitter.SplitterFactory;
import com.alibaba.cloud.ai.copilot.knowledge.splitter.SplitterStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 知识库核心服务
 * 
 * 职责:
 * 1. 文件/目录处理: 读取、切割、转换为知识块
 * 2. 知识库操作: 添加、搜索、删除
 * 3. 结果格式化: 提取内容、格式化上下文
 * 
 * 这是知识库模块的统一入口,供其他模块调用
 *
 * @author RobustH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final SplitterFactory splitterFactory;
    private final KnowledgeVectorStoreService vectorStoreService;

    // ==================== 文件处理 ====================

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

        List<KnowledgeChunk> chunks = processFile(file);
        return saveChunks(userId, chunks);
    }

    /**
     * 添加目录到知识库 (递归处理所有文件)
     * 使用 Files.walk 遍历目录，自动过滤非文件，并递归处理子目录
     *
     * @param userId        用户ID
     * @param directoryPath 目录绝对路径
     * @return 成功添加的知识块总数量
     */
    public int addDirectory(String userId, String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            log.warn("目录不存在或不是目录: {}", directoryPath);
            return 0;
        }

        try (var paths = Files.walk(directory.toPath())) {
            List<KnowledgeChunk> chunks = paths
                    .filter(Files::isRegularFile)
                    .map(path -> processFile(path.toFile()))
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            return saveChunks(userId, chunks);
        } catch (IOException e) {
            log.error("处理目录失败: {}", directoryPath, e);
            return 0;
        }
    }

    /**
     * 添加通用文本内容到知识库
     * 默认使用 TOKEN 切割策略，适用于大多数非代码文本
     *
     * @param userId  用户ID
     * @param content 内容字符串
     * @return 添加的知识块数量
     */
    public int addKnowledge(String userId, String content) {
        return addKnowledge(userId, content, SplitterStrategy.TOKEN);
    }

    /**
     * 指定策略可以直接添加内容
     * 适用于已知内容类型的场景，无需提供文件路径
     *
     * @param userId   用户ID
     * @param content  内容字符串
     * @param strategy 切割策略 (如 SENTENCE 用于 RAG，SMART_CODE 用于代码片段)
     * @return 添加的知识块数量
     */
    public int addKnowledge(String userId, String content, SplitterStrategy strategy) {
        if (content == null || content.trim().isEmpty()) return 0;
        
        // 生成虚拟路径，避免 ID 冲突
        String virtualPath = "dynamic-" + UUID.randomUUID();
        DocumentSplitter splitter = splitterFactory.getSplitter(strategy);
        List<KnowledgeChunk> chunks = splitter.split(content, virtualPath);
        
        return saveChunks(userId, chunks);
    }

    /**
     * 添加知识内容，并指定虚拟文件路径
     * 工厂将根据 filePath 的扩展名自动推断最合适的切割策略
     *
     * @param userId   用户ID
     * @param content  内容字符串
     * @param filePath 虚拟文件路径 (例如 "docs/manual.md" 或 "src/Main.java")
     * @return 添加的知识块数量
     */
    public int addKnowledge(String userId, String content, String filePath) {
        if (content == null || content.trim().isEmpty()) return 0;
        
        DocumentSplitter splitter = splitterFactory.getSplitterByPath(filePath);
        List<KnowledgeChunk> chunks = splitter.split(content, filePath);
        
        return saveChunks(userId, chunks);
    }

    // ==================== 内部处理方法 ====================

    /**
     * 读取并切割单个文件
     * 注意：捕获所有 IO 异常并返回空列表，确保目录遍历（Files.walk）不会因单个文件错误而中断
     */
    private List<KnowledgeChunk> processFile(File file) {
        try {
            String content = Files.readString(file.toPath());
            String filePath = file.getAbsolutePath();
            return splitterFactory.getSplitterByPath(filePath).split(content, filePath);
        } catch (IOException e) {
            log.error("读取文件失败: {}", file.getAbsolutePath(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 统一保存知识块到向量数据库
     */
    private int saveChunks(String userId, List<KnowledgeChunk> chunks) {
        if (chunks.isEmpty()) return 0;
        try {
            vectorStoreService.addKnowledgeBatch(userId, chunks);
            log.info("已存储知识: 用户={}, 块数={}", userId, chunks.size());
            return chunks.size();
        } catch (Exception e) {
            log.error("存储知识失败: 用户={}", userId, e);
            return 0;
        }
    }

    // ==================== 知识库搜索 ====================

    /**
     * 通用搜索
     * @param topK 返回最相似的前 K 个结果
     */
    public List<Document> search(String userId, String query, int topK) {
        return vectorStoreService.searchKnowledge(userId, query, topK);
    }

    /**
     * 按文件类型过滤搜索
     */
    public List<Document> searchByFileType(String userId, String query, KnowledgeCategory.FileType fileType, int topK) {
        return vectorStoreService.searchKnowledgeByFileType(userId, query, fileType, topK);
    }

    // 便捷搜索方法 (Delegate to searchByFileType)

    public List<Document> searchCode(String userId, String query, int topK) {
        return searchByFileType(userId, query, KnowledgeCategory.FileType.CODE, topK);
    }

    public List<Document> searchDocuments(String userId, String query, int topK) {
        return searchByFileType(userId, query, KnowledgeCategory.FileType.DOCUMENT, topK);
    }

    public List<Document> searchConfig(String userId, String query, int topK) {
        return searchByFileType(userId, query, KnowledgeCategory.FileType.CONFIG, topK);
    }

    // ==================== 辅助方法 ====================

    /**
     * 提取搜索结果中的纯文本内容
     */
    public List<String> extractContents(List<Document> documents) {
        return documents.stream().map(Document::getText).collect(Collectors.toList());
    }

    /**
     * 将搜索结果格式化为适合 LLM 上下文的字符串 (RAG 格式)
     * 
     * 输出示例:
     * 文件: src/Main.java
     * 内容:
     * public class Main { ... }
     * 
     * ---
     */
    public String formatAsContext(List<Document> documents) {
        return documents.stream()
                .map(doc -> String.format("文件: %s\n内容:\n%s", 
                        doc.getMetadata().getOrDefault("file_path", "unknown"), 
                        doc.getText()))
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * 删除指定用户的所有知识库数据
     */
    public void deleteUserKnowledge(String userId) {
        vectorStoreService.deleteUserKnowledge(userId);
        log.info("已删除用户知识: {}", userId);
    }
}
