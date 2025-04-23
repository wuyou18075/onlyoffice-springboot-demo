package cn.superlu.onlyoffice.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {
    // MinIO服务器地址
    private String endpoint;
    // MinIO服务访问用户名
    private String accessKey;
    // MinIO服务访问密码
    private String secretKey;
    // 文档存储桶名称
    private String bucketName;
    // 是否使用安全连接
    private boolean secure;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
