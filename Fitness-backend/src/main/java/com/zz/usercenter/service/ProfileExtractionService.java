package com.zz.usercenter.service;

import java.util.concurrent.CompletableFuture;

public interface ProfileExtractionService {

    void appendDirtyText(Long userId, String message);

    /**
     * 异步提纯当天脏文本（免费模型），返回提纯后的文本。
     * 提纯结果同时回填 Redis，供定时任务和后续请求使用。
     */
    CompletableFuture<String> purifyDirtyText(Long userId);

    /**
     * 获取已提纯的脏文本（从 Redis 读取，不调 AI）
     */
    String getPurifiedText(Long userId);

    /**
     * 每日画像更新：读取前一天提纯后的文本，合并旧画像更新 user.userProfile
     */
    void extractAllPendingProfiles();

    /**
     * 每周画像全量刷新：读取最近7天的提纯文本 + 旧画像，用聪明模型重新生成画像
     */
    void weeklyProfileRefresh();
}
