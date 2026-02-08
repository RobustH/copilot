package com.alibaba.cloud.ai.copilot.knowledge.model;

/**
 * 知识分类
 *
 * @author RobustH
 */
public class KnowledgeCategory {

    /**
     * 文件类型 (主要分类维度)
     */
    public enum FileType {
        /** 代码文件 */
        CODE,
        /** 文档文件 (Markdown, Text 等) */
        DOCUMENT,
        /** 配置文件 (JSON, YAML, XML 等) */
        CONFIG,
        /** 其他文件 */
        OTHER
    }
}
