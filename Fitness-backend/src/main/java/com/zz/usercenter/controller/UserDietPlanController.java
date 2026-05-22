package com.zz.usercenter.controller;

import com.zz.usercenter.common.BaseResponse;
import com.zz.usercenter.common.ResultUtils;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.model.domain.User;
import com.zz.usercenter.model.domain.request.SaveUserDietCycleRequest;
import com.zz.usercenter.model.domain.request.SaveUserDietDayTemplateRequest;
import com.zz.usercenter.model.domain.request.SaveUserDietTemplateRequest;
import com.zz.usercenter.model.domain.vo.UserDietCycleVO;
import com.zz.usercenter.model.domain.vo.UserDietDayTemplateVO;
import com.zz.usercenter.model.domain.vo.UserDietTemplateVO;
import com.zz.usercenter.service.UserDietCycleService;
import com.zz.usercenter.service.UserDietDayTemplateService;
import com.zz.usercenter.service.UserDietTemplateService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.zz.usercenter.common.StateCode.NOT_LOGIN;

@RestController
@RequestMapping("/diet-plan")
public class UserDietPlanController {

    @Resource
    private UserDietTemplateService userDietTemplateService;

    @Resource
    private UserDietDayTemplateService userDietDayTemplateService;

    @Resource
    private UserDietCycleService userDietCycleService;

    private User getLoginUser(HttpServletRequest request) {
        Object obj = request.getSession().getAttribute("userLoginState");
        if (obj == null) {
            throw new BusincessException(NOT_LOGIN, "请先登录");
        }
        return (User) obj;
    }

    // ========== 餐次模板 ==========

    @GetMapping("/templates")
    public BaseResponse<List<UserDietTemplateVO>> listTemplates(HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userDietTemplateService.listTemplates(user.getId()));
    }

    @PostMapping("/template/save")
    public BaseResponse<Long> saveTemplate(@RequestBody SaveUserDietTemplateRequest body, HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userDietTemplateService.saveTemplate(user.getId(), body));
    }

    @PostMapping("/template/delete")
    public BaseResponse<Boolean> deleteTemplate(@RequestBody Long templateId, HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userDietTemplateService.deleteTemplate(user.getId(), templateId));
    }

    // ========== 日模板 ==========

    @GetMapping("/day-templates")
    public BaseResponse<List<UserDietDayTemplateVO>> listDayTemplates(HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userDietDayTemplateService.listDayTemplates(user.getId()));
    }

    @GetMapping("/day-template")
    public BaseResponse<UserDietDayTemplateVO> getDayTemplate(@RequestParam Long id, HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userDietDayTemplateService.getDayTemplate(user.getId(), id));
    }

    @PostMapping("/day-template/save")
    public BaseResponse<Long> saveDayTemplate(@RequestBody SaveUserDietDayTemplateRequest body, HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userDietDayTemplateService.saveDayTemplate(user.getId(), body));
    }

    @PostMapping("/day-template/delete")
    public BaseResponse<Boolean> deleteDayTemplate(@RequestBody Long id, HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userDietDayTemplateService.deleteDayTemplate(user.getId(), id));
    }

    // ========== 循环计划 ==========

    @GetMapping("/cycles")
    public BaseResponse<List<UserDietCycleVO>> listCycles(HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userDietCycleService.listCycles(user.getId()));
    }

    @GetMapping("/cycle/active")
    public BaseResponse<UserDietCycleVO> getActiveCycle(HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userDietCycleService.getActiveCycle(user.getId()));
    }

    @PostMapping("/cycle/save")
    public BaseResponse<Long> saveCycle(@RequestBody SaveUserDietCycleRequest body, HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userDietCycleService.saveCycle(user.getId(), body));
    }

    @PostMapping("/cycle/activate")
    public BaseResponse<Boolean> activateCycle(@RequestBody Long cycleId, HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userDietCycleService.activateCycle(user.getId(), cycleId));
    }

    @PostMapping("/cycle/delete")
    public BaseResponse<Boolean> deleteCycle(@RequestBody Long cycleId, HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(userDietCycleService.deleteCycle(user.getId(), cycleId));
    }
}
