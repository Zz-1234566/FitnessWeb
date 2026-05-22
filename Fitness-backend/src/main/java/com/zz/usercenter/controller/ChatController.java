package com.zz.usercenter.controller;

import com.zz.usercenter.common.BaseResponse;
import com.zz.usercenter.common.ResultUtils;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.model.domain.ChatHistory;
import com.zz.usercenter.model.domain.User;
import com.zz.usercenter.model.domain.request.ChatRequest;
import com.zz.usercenter.service.ChatService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
        String pendingContent = null;
        if ("training".equals(type)) {
            pendingContent = (String) request.getSession().getAttribute("PENDING_TRAINING_PLAN");
        } else if ("diet".equals(type)) {
            pendingContent = (String) request.getSession().getAttribute("PENDING_DIET_PLAN");
        }

        if (pendingContent == null) {
            return ResultUtils.error(NULL_ERROR, "没有待保存的计划");
        }
        String result = chatService.saveGeneratedPlan(currentUser.getId(), type, pendingContent);

        request.getSession().removeAttribute("PENDING_TRAINING_PLAN");
        request.getSession().removeAttribute("PENDING_DIET_PLAN");

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
            chatService.sendMessageStream(currentUser.getId(), chatRequest.getMessage(),
                    outputStream, resultHolder, replyTypeRef);
            try {
                String replyType = replyTypeRef.get();
                String savePlanType = "";

                if ("training_plan".equals(replyType)) {
                    request.getSession().setAttribute("PENDING_TRAINING_PLAN", resultHolder.toString());
                    savePlanType = "training";
                } else if ("diet_plan".equals(replyType)) {
                    request.getSession().setAttribute("PENDING_DIET_PLAN", resultHolder.toString());
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

}
