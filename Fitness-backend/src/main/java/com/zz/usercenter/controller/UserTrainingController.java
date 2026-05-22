package com.zz.usercenter.controller;

import com.zz.usercenter.common.BaseResponse;
import com.zz.usercenter.common.ResultUtils;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.model.domain.User;
import com.zz.usercenter.model.domain.request.SaveUserTrainingCycleRequest;
import com.zz.usercenter.model.domain.request.SaveUserTrainingTemplateRequest;
import com.zz.usercenter.model.domain.vo.UserTrainingCycleVO;
import com.zz.usercenter.model.domain.vo.UserTrainingTemplateVO;
import com.zz.usercenter.service.UserTrainingCycleService;
import com.zz.usercenter.service.UserTrainingTemplateService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.zz.usercenter.common.StateCode.NOT_LOGIN;

@RestController
@RequestMapping("/training")
public class UserTrainingController {

    @Resource
    private UserTrainingTemplateService userTrainingTemplateService;

    @Resource
    private UserTrainingCycleService userTrainingCycleService;

    private User getLoginUser(HttpServletRequest request) {
        Object obj = request.getSession().getAttribute("userLoginState");
        if (obj == null) {
            throw new BusincessException(NOT_LOGIN, "请先登录");
        }
        return (User) obj;
    }

    // ========== 训练日模板 ==========

    @GetMapping("/templates")
    public BaseResponse<List<UserTrainingTemplateVO>> listTemplates(HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userTrainingTemplateService.listTemplates(user.getId()));
    }

    @PostMapping("/template/save")
    public BaseResponse<Long> saveTemplate(@RequestBody SaveUserTrainingTemplateRequest body, HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userTrainingTemplateService.saveTemplate(user.getId(), body));
    }

    @PostMapping("/template/delete")
    public BaseResponse<Boolean> deleteTemplate(@RequestBody Long templateId, HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userTrainingTemplateService.deleteTemplate(user.getId(), templateId));
    }

    // ========== 训练循环 ==========

    @GetMapping("/cycles")
    public BaseResponse<List<UserTrainingCycleVO>> listCycles(HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userTrainingCycleService.listCycles(user.getId()));
    }

    @GetMapping("/cycle/active")
    public BaseResponse<UserTrainingCycleVO> getActiveCycle(HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userTrainingCycleService.getActiveCycle(user.getId()));
    }

    @PostMapping("/cycle/save")
    public BaseResponse<Long> saveCycle(@RequestBody SaveUserTrainingCycleRequest body, HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userTrainingCycleService.saveCycle(user.getId(), body));
    }

    @PostMapping("/cycle/activate")
    public BaseResponse<Boolean> activateCycle(@RequestBody Long cycleId, HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userTrainingCycleService.activateCycle(user.getId(), cycleId));
    }

    @PostMapping("/cycle/delete")
    public BaseResponse<Boolean> deleteCycle(@RequestBody Long cycleId, HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userTrainingCycleService.deleteCycle(user.getId(), cycleId));
    }
}
