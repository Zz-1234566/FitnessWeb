package com.zz.usercenter.service;

import com.zz.usercenter.model.domain.ChatHistory;
import com.zz.usercenter.model.domain.User;
import com.zz.usercenter.model.domain.request.SaveRecognizedFoodRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI对话 Service
 */
public interface ChatService {


    /**
     * 发送消息（SSE流式响应）
     * @param userId 用户ID
     * @param message 用户消息
     * @param resultHolder
     * @param replyTypeHolder 工具调用后设置的 replyType，供 Controller 直接使用
     */
    void sendMessageStream(Long userId, String message, OutputStream outputStream, StringBuilder resultHolder, AtomicReference<String> replyTypeHolder);

    /**
     * 获取用户的对话记录
     * @param userId 用户ID
     * @return 对话记录（包含训练计划、饮食计划等）
     */
    ChatHistory getChatHistory(Long userId);

    /**
     * 更新数据库历史
     */
    void updateChatHistory(ChatHistory history);


    /**
     * 创建数据库历史
     */
    void createChatHistory(ChatHistory history);

    /**
     * 保存 AI 生成的训练/饮食计划到当前计划系统，并同步保留聊天里的原始文本
     */
    String saveGeneratedPlan(Long userId, String type, String content);

    /**
     * 一键记录今日训练/饮食计划为实际记录
     */
    String quickSaveTodayPlan(Long userId, String type);

    /**
     * 根据用户信息 + 画像表单数据，调用AI生成用户画像摘要
     */
    String generateUserProfile(User user, String profileFormData);

    Map<String, Object> recognizeFoodImage(Long userId, MultipartFile file, String type);

    Map<String, Object> recognizeImage(Long userId, MultipartFile file, String type, String customText, boolean deepThinking);

    void recognizeSummaryStream(Long userId, String type, String equipmentName, String rawData, boolean deepThinking, OutputStream outputStream);

    boolean saveRecognizedFood(Long userId, SaveRecognizedFoodRequest request);

}
