package com.zz.usercenter.controller;

import com.zz.usercenter.common.BaseResponse;
import com.zz.usercenter.common.ResultUtils;
import com.zz.usercenter.common.WhisperTranscriptionHelper;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.model.domain.ChatHistory;
import com.zz.usercenter.model.domain.User;
import com.zz.usercenter.model.domain.request.ChatRequest;
import com.zz.usercenter.model.domain.request.SaveRecognizedFoodRequest;
import com.zz.usercenter.service.ChatService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.zz.usercenter.common.StateCode.AI_ERROR;
import static com.zz.usercenter.common.StateCode.NOT_LOGIN;
import static com.zz.usercenter.common.StateCode.NULL_ERROR;
import static com.zz.usercenter.constant.UserConstant.USER_LOGIN_STATE;

/**
 * AI对话 Controller
 */
@RestController
@RequestMapping("/chat")
@Slf4j
public class ChatController {

    @Resource
    private ChatService chatService;

    @Resource
    private WhisperTranscriptionHelper whisperTranscriptionHelper;

    @Resource
    private com.zz.usercenter.config.AiModelConfig aiModelConfig;

    /**
     * 获取对话记录（包含训练计划、饮食计划等）
     */
    @GetMapping("/history")
    public BaseResponse<ChatHistory> getHistory(HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (currentUser == null) {
            throw new BusincessException(NOT_LOGIN, "未登录");
        }

        ChatHistory history = chatService.getChatHistory(currentUser.getId());
        return ResultUtils.success(history);
    }

    /** 确认保存计划到数据库 */
    @PostMapping("/save-plan")
    public BaseResponse<String> savePlan(@RequestBody Map<String, String> body,
                                         HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (currentUser == null) {
            throw new BusincessException(NOT_LOGIN, "未登录");
        }

        String type = body.get("type");
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResultUtils.error(NULL_ERROR, "没有待保存的计划");
        }
        String result = chatService.saveGeneratedPlan(currentUser.getId(), type, content);
        return ResultUtils.success(result);
    }

    /** 一键记录今日训练/饮食计划 */
    @PostMapping("/quick-save-record")
    public BaseResponse<String> quickSaveRecord(@RequestBody Map<String, String> body,
                                                HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (currentUser == null) {
            throw new BusincessException(NOT_LOGIN, "未登录");
        }
        String type = body.get("type");
        String result = chatService.quickSaveTodayPlan(currentUser.getId(), type);
        return ResultUtils.success(result);
    }

    /**
     * 发送消息（SSE 流式响应）
     * <p>
     * replyType 由 ChatServiceImpl 的 function calling 工具调用决定，
     * 不再通过解析回复文本来猜测类型。
     */
    @PostMapping(value = "/send/stream", produces = "text/event-stream")
    public ResponseEntity<StreamingResponseBody> sendMessageStream(@RequestBody ChatRequest chatRequest,
                                                                   HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (currentUser == null) {
            throw new BusincessException(NOT_LOGIN, "未登录");
        }
        StringBuilder resultHolder = new StringBuilder();
        AtomicReference<String> replyTypeRef = new AtomicReference<>("general_chat");

        StreamingResponseBody bodyWithSession = outputStream -> {
            // 设置客户端 IP 供天气服务等使用
            String clientIp = request.getHeader("X-Forwarded-For");
            if (clientIp == null || clientIp.isBlank() || "unknown".equalsIgnoreCase(clientIp)) {
                clientIp = request.getHeader("X-Real-IP");
            }
            if (clientIp == null || clientIp.isBlank() || "unknown".equalsIgnoreCase(clientIp)) {
                clientIp = request.getRemoteAddr();
            }
            if (clientIp != null && clientIp.contains(",")) {
                clientIp = clientIp.split(",")[0].trim();
            }
            com.zz.usercenter.service.impl.ChatServiceImpl.setClientIp(clientIp);

            try {
                chatService.sendMessageStream(currentUser.getId(), chatRequest.getMessage(),
                        outputStream, resultHolder, replyTypeRef);
            } finally {
                com.zz.usercenter.service.impl.ChatServiceImpl.clearClientIp();
            }
            try {
                String replyType = replyTypeRef.get();
                String savePlanType = "";

                if ("training_plan".equals(replyType)) {
                    savePlanType = "training";
                } else if ("diet_plan".equals(replyType)) {
                    savePlanType = "diet";
                }

                writeStreamMeta(outputStream, replyType, savePlanType);
            } catch (Exception e) {
                log.warn("发送聊天流元数据失败", e);
            }
        };

        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(bodyWithSession);
    }

    private void writeStreamMeta(OutputStream outputStream, String replyType, String savePlanType) throws Exception {
        boolean showSaveButton = !savePlanType.isBlank();
        boolean showRecordButton = "today_training_plan".equals(replyType) || "today_diet_plan".equals(replyType);
        String recordType = "today_training_plan".equals(replyType) ? "training" : "";
        if ("today_diet_plan".equals(replyType)) recordType = "diet";
        String payload = String.format(
                "{\"replyType\":\"%s\",\"showSaveButton\":%s,\"savePlanType\":\"%s\",\"showRecordButton\":%s,\"recordType\":\"%s\"}",
                replyType,
                showSaveButton,
                savePlanType,
                showRecordButton,
                recordType
        );
        outputStream.write(("event: meta\ndata:" + payload + "\n\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    /** 语音转录 (Groq Whisper) */
    @PostMapping("/transcribe-audio")
    public BaseResponse<String> transcribeAudio(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (currentUser == null) {
            throw new BusincessException(NOT_LOGIN, "未登录");
        }
        if (file.isEmpty()) {
            return ResultUtils.error(NULL_ERROR, "音频文件为空");
        }
        try {
            String originalFilename = file.getOriginalFilename();
            String text = whisperTranscriptionHelper.transcribe(
                    file.getBytes(),
                    originalFilename != null ? originalFilename : "recording.webm"
            );
            if (text == null || text.isBlank()) {
                return ResultUtils.error(AI_ERROR, "语音识别失败，未获取到有效文本");
            }
            return ResultUtils.success(text.trim());
        } catch (Exception e) {
            log.error("语音转录异常", e);
            return ResultUtils.error(AI_ERROR, "语音转录服务异常");
        }
    }

    /** 获取图片识别配置（超时时长、深度思索开关） */
    @GetMapping("/recognize-config")
    public BaseResponse<Map<String, Object>> getRecognizeConfig() {
        Map<String, Object> config = new java.util.HashMap<>();
        config.put("deepThinking", aiModelConfig.isDeepThinking());
        config.put("recognitionTimeout", aiModelConfig.getRecognitionTimeout());
        return ResultUtils.success(config);
    }

    /** 图片识别（食物/器械/营养标签/姿势纠错） */
    @PostMapping("/recognize-food")
    public BaseResponse<Map<String, Object>> recognizeFood(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "food") String type,
            @RequestParam(value = "customText", required = false) String customText,
            @RequestParam(value = "deepThinking", defaultValue = "false") boolean deepThinking,
            HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (currentUser == null) {
            throw new BusincessException(NOT_LOGIN, "未登录");
        }
        long controllerStartedAt = System.currentTimeMillis();
        Map<String, Object> result = chatService.recognizeImage(currentUser.getId(), file, type, customText, deepThinking);
        result.put("controllerReturnAtMs", System.currentTimeMillis());
        result.put("recognitionTimeout", aiModelConfig.getRecognitionTimeout());
        result.put("deepThinking", aiModelConfig.isDeepThinking());
        log.info("[VisionTrace] controller return, userId={}, elapsed={}ms, traceId={}",
                currentUser.getId(),
                System.currentTimeMillis() - controllerStartedAt,
                result.get("debugTimings") instanceof Map<?, ?> timings ? timings.get("traceId") : null);
        return ResultUtils.success(result);
    }

    /** 图片识别 SSE 流式总结（只做 Step3 AI 总结推送） */
    @PostMapping(value = "/recognize-stream", produces = "text/event-stream;charset=UTF-8")
    public ResponseEntity<StreamingResponseBody> recognizeStream(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (currentUser == null) {
            return ResponseEntity.ok().body(out -> out.write(("event:error\ndata:{\"message\":\"未登录\"}\n\n").getBytes(StandardCharsets.UTF_8)));
        }

        String type = (String) body.getOrDefault("type", "equipment");
        String equipmentName = (String) body.get("equipmentName");
        String rawData = (String) body.get("rawData");
        boolean deepThinking = Boolean.parseBoolean(String.valueOf(body.getOrDefault("deepThinking", "false")));

        StreamingResponseBody streamBody = outputStream -> {
            try {
                chatService.recognizeSummaryStream(currentUser.getId(), type, equipmentName, rawData, deepThinking, outputStream);
            } catch (Exception e) {
                log.error("SSE总结异常", e);
                try {
                    outputStream.write(("event:error\ndata:{\"message\":\"生成失败\"}\n\n").getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (Exception ignored) {}
            }
        };

        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(streamBody);
    }

    /** 保存识别结果为饮食记录 */
    @PostMapping("/recognize-food/save")
    public BaseResponse<Boolean> saveRecognizedFood(@RequestBody SaveRecognizedFoodRequest body,
                                                    HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (currentUser == null) {
            throw new BusincessException(NOT_LOGIN, "未登录");
        }
        boolean ok = chatService.saveRecognizedFood(currentUser.getId(), body);
        return ResultUtils.success(ok);
    }

}
