package com.alibaba.cloud.ai.copilot.knowledge.splitter;

import com.alibaba.cloud.ai.copilot.knowledge.model.KnowledgeChunk;

import java.util.List;

/**
 * 文档切割器接口
 * 负责将文档内容切割为知识块
 *
 * @author RobustH
 */
public interface DocumentSplitter {

    /**
     * 切割文档内容
     *
     * @param content  文档内容
     * @param filePath 文件路径
     * @return 知识块列表
     */
    List<KnowledgeChunk> split(String content, String filePath);
}
