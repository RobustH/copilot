package com.alibaba.cloud.ai.copilot.knowledge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识块模型
 *
 * @author RobustH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChunk {

    /**
     * 唯一标识
     */
    private String id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 文本内容
     */
    private String content;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件类型
     */
    private KnowledgeCategory.FileType fileType;

    /**
     * 编程语言 (对于代码文件)
     */
    private String language;

    /**
     * 代码符号名称 (函数名、类名等)
     */
    private String symbolName;

    /**
     * 起始行号
     */
    private Integer startLine;

    /**
     * 结束行号
     */
    private Integer endLine;

    /**
     * 创建时间
     */
    private Long createdAt;

    /**
     * 是否为代码文件
     */
    public boolean isCode() {
        return fileType == KnowledgeCategory.FileType.CODE;
    }
}
