package com.alibaba.cloud.ai.copilot.knowledge.service;

import com.alibaba.cloud.ai.copilot.knowledge.classifier.FileTypeClassifier;
import com.alibaba.cloud.ai.copilot.knowledge.model.KnowledgeCategory;
import com.alibaba.cloud.ai.copilot.knowledge.model.KnowledgeChunk;
import com.alibaba.cloud.ai.copilot.knowledge.splitter.DocumentSplitter;
import com.alibaba.cloud.ai.copilot.knowledge.splitter.SplitterFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档处理服务
 * 负责文件读取、切割和转换为知识块
 *
 * @author RobustH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final FileTypeClassifier fileTypeClassifier;
    private final SplitterFactory splitterFactory;

    /**
     * 处理文件,返回知识块列表
     */
    public List<KnowledgeChunk> processFile(File file) {
        String filePath = file.getAbsolutePath();
        
        try {
            // 识别文件类型
            KnowledgeCategory.FileType fileType = fileTypeClassifier.classifyFileType(filePath);
            
            log.info("正在处理文件: 路径={}, 类型={}", filePath, fileType);

            // 读取文件内容
            String content = Files.readString(file.toPath());

            // 获取合适的切割器
            DocumentSplitter splitter = splitterFactory.getSplitter(fileType);

            // 切割文档
            List<KnowledgeChunk> chunks = splitter.split(content, filePath);

            log.info("文件已处理为 {} 个知识块: {}", chunks.size(), filePath);
            return chunks;
            
        } catch (IOException e) {
            log.error("读取文件失败: {}", filePath, e);
            return new ArrayList<>();
        }
    }

    /**
     * 处理目录,递归处理所有文件
     */
    public List<KnowledgeChunk> processDirectory(File directory) {
        List<KnowledgeChunk> allChunks = new ArrayList<>();
        
        File[] files = directory.listFiles();
        if (files == null) {
            return allChunks;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                allChunks.addAll(processDirectory(file));
            } else {
                try {
                    allChunks.addAll(processFile(file));
                } catch (Exception e) {
                    log.error("处理文件失败: {}", file.getAbsolutePath(), e);
                }
            }
        }

        return allChunks;
    }
}
