package com.alibaba.cloud.ai.copilot.knowledge.splitter;

import com.alibaba.cloud.ai.copilot.knowledge.model.KnowledgeCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 文档切割器工厂
 * 根据文件类型返回合适的切割器
 *
 * @author RobustH
 */
@Component
@RequiredArgsConstructor
public class SplitterFactory {

    private final TokenDocumentSplitter tokenDocumentSplitter;

    /**
     * 获取文档切割器
     *
     * @param fileType 文件类型
     * @return 文档切割器
     */
    public DocumentSplitter getSplitter(KnowledgeCategory.FileType fileType) {
        // 当前所有类型都使用 Token 切割器
        // 未来可以根据文件类型返回不同的切割器:
        // - CODE -> CodeSymbolSplitter
        // - DOCUMENT -> SemanticSplitter
        // - CONFIG -> TokenSplitter
        return tokenDocumentSplitter;
    }

    /**
     * 获取默认切割器
     *
     * @return 文档切割器
     */
    public DocumentSplitter getDefaultSplitter() {
        return tokenDocumentSplitter;
    }
}
