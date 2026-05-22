package com.zz.usercenter.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云COS配置类
 * 用来读取 yml 配置 创建一个 cosclient Bean 供其他类使用（上传文件）
 * @author zhouzhou
 */
@Configuration
@Data
public class CosConfig {

    @Value("${cos.secret-id}")
    private String secretId;

    @Value("${cos.secret-key}")
    private String secretKey;

    @Value("${cos.region}")
    private String region;

    @Value("${cos.bucket}")
    private String bucket;

    @Bean
    public COSClient cosClient() {
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        Region reg = new Region(region);
        ClientConfig config = new ClientConfig(reg);
        return new COSClient(cred, config);
    }
}