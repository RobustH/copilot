package com.alibaba.cloud.ai.copilot.knowledge.service;

import com.alibaba.cloud.ai.copilot.knowledge.classifier.FileTypeClassifier;
import com.alibaba.cloud.ai.copilot.knowledge.model.KnowledgeCategory;
import com.alibaba.cloud.ai.copilot.knowledge.model.KnowledgeChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    /**
     * 处理文件,返回知识块列表
     */
    public List<KnowledgeChunk> processFile(File file) {
        String filePath = file.getAbsolutePath();
        
        // 识别文件类型和语言
        KnowledgeCategory.FileType fileType = fileTypeClassifier.classifyFileType(filePath);
        String language = fileTypeClassifier.detectLanguage(filePath);

        log.info("正在处理文件: 路径={}, 类型={}, 语言={}", filePath, fileType, language);

        // 读取文件内容
        TextReader textReader = new TextReader(new FileSystemResource(file));
        List<Document> documents = textReader.get();

        // 使用 TokenTextSplitter 切割文档
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
        List<Document> splitDocuments = tokenTextSplitter.apply(documents);

        // 转换为 KnowledgeChunk
        List<KnowledgeChunk> chunks = new ArrayList<>();
        for (Document doc : splitDocuments) {
            KnowledgeChunk chunk = KnowledgeChunk.builder()
                    .id(UUID.randomUUID().toString())
                    .content(doc.getText())
                    .filePath(filePath)
                    .fileType(fileType)
                    .language(language)
                    .startLine(1) // 待优化: 从 metadata 中提取行号
                    .endLine(1)   // 待优化: 从 metadata 中提取行号
                    .createdAt(System.currentTimeMillis())
                    .build();
            
            chunks.add(chunk);
        }

        log.info("文件已处理为 {} 个知识块: {}", chunks.size(), filePath);
        return chunks;
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
