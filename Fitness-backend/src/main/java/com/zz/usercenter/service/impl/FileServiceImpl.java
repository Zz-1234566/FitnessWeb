package com.zz.usercenter.service.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.service.FileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

import static com.zz.usercenter.common.StateCode.*;


/**
 * 校验上传文件类型和大小 → 生成唯一文件名 → 上传到 COS → 返回图片 URL
 *
 * @author zhouzhou
 */
@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Resource
    private COSClient cosClient;

    @Value("${cos.bucket}")
    private String bucket;

    @Value("${cos.region}")
    private String region;

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif",
            "image/webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    @Override
    public String uploadAvatar(MultipartFile file, Long userId) {
        validateImage(file, "头像");

        // 2. 生成文件路径
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String key = "avatar/" + userId + "_" + System.currentTimeMillis() + ext;

        return uploadToCos(file, key, "头像上传失败");
    }

    @Override
    public String uploadFoodImage(MultipartFile file, Long operatorId) {
        validateImage(file, "食物图片");

        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String key = "food/" + operatorId + "_" + System.currentTimeMillis() + ext;

        return uploadToCos(file, key, "食物图片上传失败");
    }

    private void validateImage(MultipartFile file, String fileLabel) {
        if (file == null || file.isEmpty()) {
            throw new BusincessException(PARAMS_ERROR, "文件不能为空");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusincessException(PARAMS_ERROR, "只支持 jpg/png/gif/webp 格式");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusincessException(PARAMS_ERROR, fileLabel + "大小不能超过5MB");
        }
    }

    private String uploadToCos(MultipartFile file, String key, String failMessage) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.setContentDisposition("inline");
            cosClient.putObject(bucket, key, file.getInputStream(), metadata);
        } catch (Exception e) {
            log.error("{}，key={}", failMessage, key, e);
            throw new BusincessException(SYSTEM_ERROR, failMessage);
        }

        return "https://" + bucket + ".cos." + region + ".myqcloud.com/" + key;
    }
}
