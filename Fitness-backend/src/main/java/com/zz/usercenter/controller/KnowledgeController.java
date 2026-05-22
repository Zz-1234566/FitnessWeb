package com.zz.usercenter.controller;

import com.zz.usercenter.common.BaseResponse;
import com.zz.usercenter.common.ResultUtils;
import com.zz.usercenter.model.domain.Knowledge;
import com.zz.usercenter.model.domain.User;
import com.zz.usercenter.model.domain.request.KnowledgeRequest;
import com.zz.usercenter.service.KnowledgeService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.zz.usercenter.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 知识库管理
 */
@RestController
@RequestMapping("/knowledge")
@Slf4j
public class KnowledgeController {

    @Resource
    private KnowledgeService knowledgeService;

    @PostMapping("/add")
    public BaseResponse<Boolean> addKnowledge(@RequestBody KnowledgeRequest request, HttpServletRequest httpRequest) {
        checkAdmin(httpRequest);
        Knowledge knowledge = new Knowledge();
        BeanUtils.copyProperties(request, knowledge);
        return ResultUtils.success(knowledgeService.addKnowledge(knowledge));
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateKnowledge(@RequestBody KnowledgeRequest request, HttpServletRequest httpRequest) {
        checkAdmin(httpRequest);
        Knowledge knowledge = new Knowledge();
        BeanUtils.copyProperties(request, knowledge);
        return ResultUtils.success(knowledgeService.updateKnowledge(knowledge));
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteKnowledge(@RequestBody KnowledgeRequest request, HttpServletRequest httpRequest) {
        checkAdmin(httpRequest);
        return ResultUtils.success(knowledgeService.deleteKnowledge(request.getId()));
    }

    @GetMapping("/list")
    public BaseResponse<List<Knowledge>> listKnowledge(@RequestParam(required = false) String category) {
        if (category != null && !category.isEmpty()) {
            return ResultUtils.success(knowledgeService.listByCategory(category));
        }
        return ResultUtils.success(knowledgeService.list());
    }

    @GetMapping("/detail")
    public BaseResponse<Knowledge> detail(@RequestParam Long id) {
        return ResultUtils.success(knowledgeService.getById(id));
    }

    /**
     * 触发增量同步（异步执行，立即返回）
     */
    @PostMapping("/sync")
    public BaseResponse<String> syncToVectorStore(HttpServletRequest httpRequest) {
        checkAdmin(httpRequest);
        knowledgeService.syncToVectorStoreAsync();
        return ResultUtils.success("同步已启动");
    }

    /**
     * 查询同步状态（前端轮询用）
     */
    @GetMapping("/sync/status")
    public BaseResponse<Map<String, String>> getSyncStatus(HttpServletRequest httpRequest) {
        checkAdmin(httpRequest);
        String status = knowledgeService.getSyncStatus();
        String state = "syncing".equals(status) ? "syncing" : "done";
        return ResultUtils.success(Map.of("state", state, "message", status));
    }

    private void checkAdmin(HttpServletRequest request) {
        Object obj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (obj == null || !(obj instanceof User) || ((User) obj).getUserRole() != 1) {
            throw new com.zz.usercenter.exception.BusincessException(
                    com.zz.usercenter.common.StateCode.NO_AUTH, "无权限操作");
        }
    }
}
