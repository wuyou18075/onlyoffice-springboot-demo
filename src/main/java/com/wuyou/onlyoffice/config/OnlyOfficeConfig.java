package cn.superlu.onlyoffice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "onlyoffice")
public class OnlyOfficeConfig {
    // OnlyOffice Document Server地址
    private String documentServerUrl;
    // 回调URL
    private String callbackUrl;
    // API密钥 (若Document Server配置了JWT安全认证)
    private String apiKey;
    // 文件预览地址前缀(用于生成完整的文件访问URL)
    private String fileUrlPrefix;
}
