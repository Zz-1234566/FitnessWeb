package com.zz.usercenter.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zz.usercenter.common.BaseResponse;
import com.zz.usercenter.common.CryptoUtils;
import com.zz.usercenter.common.ResultUtils;
import com.zz.usercenter.config.AiModelConfig;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.model.domain.User;
import com.zz.usercenter.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.zz.usercenter.common.StateCode.*;

@RestController
@RequestMapping("/model")
public class ModelController {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Resource
    private UserService userService;

    @Resource
    private AiModelConfig aiModelConfig;

    // ========================= 接口 =========================

    @GetMapping("/list")
    public BaseResponse<Map<String, Object>> listModels(HttpServletRequest request) {
        User user = getLoginUser(request);
        User dbUser = userService.getById(user.getId());

        List<Map<String, String>> models = new ArrayList<>();

        for (AiModelConfig.ModelProvider p : aiModelConfig.getAvailableProviders()) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("name", p.getName());
            m.put("label", p.getLabel());
            m.put("type", p.getType() != null ? p.getType() : "text");
            models.add(m);
        }

        Map<String, String> whisperEntry = new LinkedHashMap<>();
        whisperEntry.put("name", aiModelConfig.getWhisper().getModel());
        whisperEntry.put("label", aiModelConfig.getWhisper().getModel() + " (语音识别)");
        whisperEntry.put("type", "asr");
        models.add(whisperEntry);

        List<Map<String, String>> customModels = getCustomModels(user.getId());
        for (Map<String, String> cm : customModels) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("name", cm.get("name"));
            m.put("label", cm.get("label"));
            m.put("type", cm.getOrDefault("type", "text"));
            m.put("isCustom", "true");
            models.add(m);
        }

        Map<String, String> prefs = parseModelPreference(dbUser.getModelPreference());
        Map<String, Object> result = new HashMap<>();
        result.put("models", models);
        result.put("current", prefs.getOrDefault("current", aiModelConfig.getDefaultModel()));
        return ResultUtils.success(result);
    }

    @PostMapping("/switch")
    public BaseResponse<String> switchModel(@RequestBody Map<String, String> body,
                                            HttpServletRequest request) {
        User user = getLoginUser(request);
        String modelName = body.get("name");

        if (modelName == null || modelName.isBlank()) {
            throw new BusincessException(PARAMS_ERROR, "模型名称不能为空");
        }

        boolean isSystem = aiModelConfig.getProvider(modelName) != null
                || modelName.equals(aiModelConfig.getWhisper().getModel());
        if (!isSystem) {
            List<Map<String, String>> customModels = getCustomModels(user.getId());
            boolean isCustom = customModels.stream().anyMatch(cm -> modelName.equals(cm.get("name")));
            if (!isCustom) {
                throw new BusincessException(PARAMS_ERROR, "不存在的模型");
            }
        }

        User dbUser = userService.getById(user.getId());
        Map<String, String> prefs = parseModelPreference(dbUser.getModelPreference());
        prefs.put("current", modelName);
        saveModelPreference(user.getId(), prefs);

        return ResultUtils.success("已切换到 " + modelName);
    }

    @PostMapping("/custom")
    public BaseResponse<String> addCustomModel(@RequestBody Map<String, String> body,
                                                HttpServletRequest request) {
        User user = getLoginUser(request);
        String name = body.get("name");
        String label = body.get("label");
        String baseUrl = body.get("baseUrl");
        String apiKey = body.get("apiKey");
        String model = body.get("model");
        String type = body.get("type");

        if (name == null || name.isBlank()) throw new BusincessException(PARAMS_ERROR, "模型标识不能为空");
        if (baseUrl == null || baseUrl.isBlank()) throw new BusincessException(PARAMS_ERROR, "API 地址不能为空");
        if (apiKey == null || apiKey.isBlank()) throw new BusincessException(PARAMS_ERROR, "API Key 不能为空");
        if (model == null || model.isBlank()) throw new BusincessException(PARAMS_ERROR, "模型 ID 不能为空");
        if (label == null || label.isBlank()) label = name;
        if (type == null || type.isBlank()) type = "text";

        List<Map<String, String>> customModels = getCustomModels(user.getId());

        if (customModels.stream().anyMatch(cm -> name.equals(cm.get("name")))
                || aiModelConfig.getProvider(name) != null) {
            throw new BusincessException(PARAMS_ERROR, "模型标识已存在");
        }

        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("name", name);
        entry.put("label", label);
        entry.put("baseUrl", baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl);
        entry.put("apiKey", CryptoUtils.encrypt(apiKey, aiModelConfig.getCryptoSecret()));
        entry.put("model", model);
        entry.put("type", type);
        customModels.add(entry);
        saveCustomModels(user.getId(), customModels);

        return ResultUtils.success("自定义模型已添加");
    }

    @PutMapping("/custom")
    public BaseResponse<String> updateCustomModel(@RequestBody Map<String, String> body,
                                                   HttpServletRequest request) {
        User user = getLoginUser(request);
        String name = body.get("name");
        String label = body.get("label");
        String baseUrl = body.get("baseUrl");
        String apiKey = body.get("apiKey");
        String model = body.get("model");
        String type = body.get("type");

        if (name == null || name.isBlank()) throw new BusincessException(PARAMS_ERROR, "模型标识不能为空");

        List<Map<String, String>> customModels = getCustomModels(user.getId());
        Optional<Map<String, String>> target = customModels.stream()
                .filter(cm -> name.equals(cm.get("name"))).findFirst();
        if (target.isEmpty()) {
            throw new BusincessException(PARAMS_ERROR, "自定义模型不存在");
        }

        Map<String, String> entry = target.get();
        if (label != null && !label.isBlank()) entry.put("label", label);
        if (baseUrl != null && !baseUrl.isBlank()) {
            String url = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            entry.put("baseUrl", url);
        }
        if (apiKey != null && !apiKey.isBlank()) entry.put("apiKey", CryptoUtils.encrypt(apiKey, aiModelConfig.getCryptoSecret()));
        if (model != null && !model.isBlank()) entry.put("model", model);
        if (type != null && !type.isBlank()) entry.put("type", type);
        saveCustomModels(user.getId(), customModels);

        return ResultUtils.success("自定义模型已更新");
    }

    @DeleteMapping("/custom")
    public BaseResponse<String> deleteCustomModel(@RequestBody Map<String, String> body,
                                                   HttpServletRequest request) {
        User user = getLoginUser(request);
        String name = body.get("name");
        if (name == null || name.isBlank()) throw new BusincessException(PARAMS_ERROR, "模型名称不能为空");

        List<Map<String, String>> customModels = getCustomModels(user.getId());
        int before = customModels.size();
        customModels.removeIf(cm -> name.equals(cm.get("name")));
        if (customModels.size() == before) {
            throw new BusincessException(PARAMS_ERROR, "自定义模型不存在");
        }
        saveCustomModels(user.getId(), customModels);

        User dbUser = userService.getById(user.getId());
        Map<String, String> prefs = parseModelPreference(dbUser.getModelPreference());
        if (name.equals(prefs.get("current"))) {
            prefs.remove("current");
            saveModelPreference(user.getId(), prefs);
        }

        return ResultUtils.success("自定义模型已删除");
    }

    @GetMapping("/custom/detail")
    public BaseResponse<Map<String, String>> getCustomModelDetail(@RequestParam String name,
                                                                   HttpServletRequest request) {
        User user = getLoginUser(request);
        List<Map<String, String>> customModels = getCustomModels(user.getId());
        Map<String, String> target = customModels.stream()
                .filter(cm -> name.equals(cm.get("name"))).findFirst().orElse(null);
        if (target == null) {
            throw new BusincessException(PARAMS_ERROR, "自定义模型不存在");
        }
        Map<String, String> result = new LinkedHashMap<>(target);
        String apiKey = result.get("apiKey");
        if (apiKey != null && apiKey.length() > 8) {
            result.put("apiKey", apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4));
        }
        return ResultUtils.success(result);
    }

    @GetMapping("/config")
    public BaseResponse<Map<String, String>> getModelConfig(HttpServletRequest request) {
        User user = getLoginUser(request);
        User dbUser = userService.getById(user.getId());

        Map<String, String> prefs = parseModelPreference(dbUser.getModelPreference());

        Map<String, String> config = new HashMap<>();
        config.put("defaultModel", prefs.getOrDefault("current", aiModelConfig.getDefaultModel()));
        config.put("purificationModel", prefs.getOrDefault("purificationModel", aiModelConfig.getPurificationModel()));
        config.put("chatModel", prefs.getOrDefault("chatModel", aiModelConfig.getChatModel()));
        config.put("whisperModel", prefs.getOrDefault("whisperModel", aiModelConfig.getWhisper().getModel()));
        config.put("visionModel", prefs.getOrDefault("visionModel",
                (aiModelConfig.getVisionModel() != null && !aiModelConfig.getVisionModel().isBlank())
                        ? aiModelConfig.getVisionModel() : "glm-4v-flash"));
        return ResultUtils.success(config);
    }

    @PostMapping("/config")
    public BaseResponse<String> updateModelConfig(@RequestBody Map<String, String> body,
                                                   HttpServletRequest request) {
        User user = getLoginUser(request);
        User dbUser = userService.getById(user.getId());

        Map<String, String> prefs = parseModelPreference(dbUser.getModelPreference());

        String purificationModel = body.get("purificationModel");
        String chatModel = body.get("chatModel");
        String whisperModel = body.get("whisperModel");
        String visionModel = body.get("visionModel");

        if (purificationModel != null && !purificationModel.isBlank()) {
            prefs.put("purificationModel", purificationModel);
        }
        if (chatModel != null && !chatModel.isBlank()) {
            prefs.put("chatModel", chatModel);
        }
        if (whisperModel != null && !whisperModel.isBlank()) {
            prefs.put("whisperModel", whisperModel);
        }
        if (visionModel != null && !visionModel.isBlank()) {
            prefs.put("visionModel", visionModel);
        }

        saveModelPreference(user.getId(), prefs);
        return ResultUtils.success("模型角色配置已更新");
    }

    @PostMapping("/config/saveAll")
    public BaseResponse<String> saveAllModelConfig(@RequestBody Map<String, String> body,
                                                    HttpServletRequest request) {
        User user = getLoginUser(request);
        User dbUser = userService.getById(user.getId());

        Map<String, String> prefs = parseModelPreference(dbUser.getModelPreference());

        String purificationModel = body.get("purificationModel");
        String chatModel = body.get("chatModel");
        String whisperModel = body.get("whisperModel");
        String visionModel = body.get("visionModel");

        // 全量保存，未传的字段用系统默认值兜底
        String pm = (purificationModel != null && !purificationModel.isBlank()) ? purificationModel : aiModelConfig.getPurificationModel();
        String cm = (chatModel != null && !chatModel.isBlank()) ? chatModel : aiModelConfig.getChatModel();
        String wm = (whisperModel != null && !whisperModel.isBlank()) ? whisperModel : aiModelConfig.getWhisper().getModel();
        String vm = (visionModel != null && !visionModel.isBlank()) ? visionModel :
                (aiModelConfig.getVisionModel() != null && !aiModelConfig.getVisionModel().isBlank())
                        ? aiModelConfig.getVisionModel() : "glm-4v-flash";

        prefs.put("purificationModel", pm);
        prefs.put("chatModel", cm);
        prefs.put("whisperModel", wm);
        prefs.put("visionModel", vm);

        // 只写用户级配置，不动全局 aiModelConfig（避免多用户互相覆盖）
        saveModelPreference(user.getId(), prefs);
        return ResultUtils.success("模型配置已保存");
    }

    // ========================= modelPreference 辅助 =========================

    /**
     * 解析 modelPreference，兼容旧格式（纯字符串）和新格式（JSON）
     * 旧格式 "qwen3.6-plus" → {current: "qwen3.6-plus"}
     * 新格式 {"current":"qwen","purificationModel":"deepseek-v4-flash",...}
     */
    private Map<String, String> parseModelPreference(String raw) {
        if (raw == null || raw.isBlank()) return new HashMap<>();
        try {
            Map<String, String> map = JSON.readValue(raw, new TypeReference<Map<String, String>>() {});
            // JSON 解析成功 → 新格式
            return map;
        } catch (Exception e) {
            // JSON 解析失败 → 旧格式（纯字符串），当 current 处理
            Map<String, String> map = new HashMap<>();
            map.put("current", raw);
            return map;
        }
    }

    private void saveModelPreference(Long userId, Map<String, String> prefs) {
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setModelPreference(toJson(prefs));
        userService.updateById(updateUser);
    }

    // ========================= 自定义模型 DB 辅助 =========================

    List<Map<String, String>> getCustomModels(Long userId) {
        User dbUser = userService.getById(userId);
        String json = dbUser.getCustomModels();
        List<Map<String, String>> models = parseCustomModels(json);
        String secret = aiModelConfig.getCryptoSecret();
        for (Map<String, String> m : models) {
            String encrypted = m.get("apiKey");
            if (encrypted != null && !encrypted.isBlank()) {
                try {
                    m.put("apiKey", CryptoUtils.decrypt(encrypted, secret));
                } catch (Exception e) {
                    // 兼容旧数据（未加密的明文 apiKey）
                }
            }
        }
        return models;
    }

    private void saveCustomModels(Long userId, List<Map<String, String>> customModels) {
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setCustomModels(customModels.isEmpty() ? null : toJson(customModels));
        userService.updateById(updateUser);
    }

    // ========================= JSON 辅助 =========================

    private List<Map<String, String>> parseCustomModels(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return JSON.readValue(json, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String toJson(Object obj) {
        try {
            return JSON.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private User getLoginUser(HttpServletRequest request) {
        Object obj = request.getSession().getAttribute("userLoginState");
        if (obj == null) {
            throw new BusincessException(NOT_LOGIN, "请先登录后继续操作");
        }
        return (User) obj;
    }
}
