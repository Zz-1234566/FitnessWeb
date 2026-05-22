package com.zz.usercenter.controller;

import com.zz.usercenter.common.BaseResponse;
import com.zz.usercenter.common.ResultUtils;
import com.zz.usercenter.config.AiModelConfig;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.model.domain.User;
import com.zz.usercenter.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.zz.usercenter.common.StateCode.*;

@RestController
@RequestMapping("/model")
public class ModelController {

    @Resource
    private UserService userService;

    @Resource
    private AiModelConfig aiModelConfig;

    /**
     * 获取可用模型列表 + 当前用户选择的模型
     */
    @GetMapping("/list")
    public BaseResponse<Map<String, Object>> listModels(HttpServletRequest request) {
        User user = getLoginUser(request);
        User dbUser = userService.getById(user.getId());

        List<AiModelConfig.ModelProvider> providers = aiModelConfig.getAvailableProviders();
        List<Map<String, String>> models = providers.stream().map(p -> {
            Map<String, String> m = new HashMap<>();
            m.put("name", p.getName());
            m.put("label", p.getLabel());
            return m;
        }).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("models", models);
        result.put("current", dbUser.getModelPreference() != null ? dbUser.getModelPreference() : aiModelConfig.getDefaultModel());
        return ResultUtils.success(result);
    }

    /**
     * 切换模型
     */
    @PostMapping("/switch")
    public BaseResponse<String> switchModel(@RequestBody Map<String, String> body,
                                            HttpServletRequest request) {
        User user = getLoginUser(request);
        String modelName = body.get("name");

        if (modelName == null || modelName.isBlank()) {
            throw new BusincessException(PARAMS_ERROR, "模型名称不能为空");
        }

        AiModelConfig.ModelProvider provider = aiModelConfig.getProvider(modelName);
        if (provider == null) {
            throw new BusincessException(PARAMS_ERROR, "不存在的模型");
        }

        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setModelPreference(modelName);
        userService.updateById(updateUser);

        user.setModelPreference(modelName);

        return ResultUtils.success("已切换到 " + provider.getLabel());
    }

    private User getLoginUser(HttpServletRequest request) {
        Object obj = request.getSession().getAttribute("userLoginState");
        if (obj == null) {
            throw new BusincessException(NOT_LOGIN, "请先登录后继续操作");
        }
        return (User) obj;
    }
}
