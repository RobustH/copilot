package com.alibaba.cloud.ai.copilot.knowledge.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus VectorStore 配置
 *
 * @author RobustH
 */
@Slf4j
@Configuration
public class MilvusVectorStoreConfig {

    @Value("${spring.ai.vectorstore.milvus.client.host:localhost}")
    private String host;

    @Value("${spring.ai.vectorstore.milvus.client.port:19530}")
    private Integer port;

    @Value("${spring.ai.vectorstore.milvus.database-name:default}")
    private String databaseName;

    @Value("${spring.ai.vectorstore.milvus.collection-name:copilot_knowledge}")
    private String collectionName;

    @Value("${spring.ai.vectorstore.milvus.embedding-dimension:1024}")
    private Integer embeddingDimension;

    @Value("${spring.ai.vectorstore.milvus.initialize-schema:true}")
    private Boolean initializeSchema;

    /**
     * 创建 Milvus 客户端
     */
    @Bean
    public MilvusServiceClient milvusClient() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .withDatabaseName(databaseName)
                .build();

        MilvusServiceClient client = new MilvusServiceClient(connectParam);
        log.info("Milvus 客户端已初始化: 主机={}, 端口={}, 数据库={}", host, port, databaseName);
        
        return client;
    }

    /**
     * 创建 Milvus VectorStore
     */
    @Bean
    public VectorStore vectorStore(MilvusServiceClient milvusClient, 
                                   @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        // 不指定索引类型,使用 Spring AI 默认配置
        MilvusVectorStore vectorStore = MilvusVectorStore.builder(milvusClient, embeddingModel)
                .collectionName(collectionName)
                .databaseName(databaseName)
                .metricType(MetricType.COSINE)
                .embeddingDimension(embeddingDimension)
                .initializeSchema(initializeSchema)
                .build();

        log.info("Milvus 向量存储已初始化: 集合={}, 维度={}", collectionName, embeddingDimension);
        
        return vectorStore;
    }
}

