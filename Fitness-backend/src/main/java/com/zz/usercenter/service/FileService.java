package com.zz.usercenter.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 腾讯云存储桶读取服务类
 *
 * @author zhouzhou
 */
public interface FileService {
    String uploadAvatar(MultipartFile file, Long userId);

    String uploadFoodImage(MultipartFile file, Long operatorId);
}
