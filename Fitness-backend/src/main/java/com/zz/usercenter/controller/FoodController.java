package com.zz.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zz.usercenter.common.BaseResponse;
import com.zz.usercenter.common.ResultUtils;
import com.zz.usercenter.constant.UserConstant;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.model.domain.FoodItem;
import com.zz.usercenter.model.domain.User;
import com.zz.usercenter.model.domain.request.SaveFoodItemRequest;
import com.zz.usercenter.service.FileService;
import com.zz.usercenter.service.FoodItemService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.zz.usercenter.common.StateCode.NO_AUTH;
import static com.zz.usercenter.common.StateCode.NOT_LOGIN;
import static com.zz.usercenter.common.StateCode.NULL_ERROR;
import static com.zz.usercenter.common.StateCode.PARAMS_ERROR;

@RestController
@RequestMapping("/food")
public class FoodController {

    @Resource
    private FoodItemService foodItemService;

    @Resource
    private FileService fileService;

    @GetMapping("/search")
    public BaseResponse<List<FoodItem>> searchFoods(@RequestParam(value = "keyword", required = false) String keyword,
                                                    HttpServletRequest request) {
        User user = requireLogin(request);
        return ResultUtils.success(foodItemService.searchVisibleFoods(user.getId(), keyword));
    }

    @GetMapping("/my")
    public BaseResponse<List<FoodItem>> listMyFoods(HttpServletRequest request) {
        User user = requireLogin(request);
        return ResultUtils.success(foodItemService.lambdaQuery()
                .eq(FoodItem::getCreatedBy, user.getId())
                .eq(FoodItem::getIsDelete, 0)
                .orderByDesc(FoodItem::getUpdateTime)
                .list());
    }

    @GetMapping("/admin/list")
    public BaseResponse<List<FoodItem>> listAllFoods(@RequestParam(value = "keyword", required = false) String keyword,
                                                     HttpServletRequest request) {
        requireAdmin(request);
        LambdaQueryWrapper<FoodItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodItem::getIsDelete, 0);
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(FoodItem::getName, keyword.trim());
        }
        wrapper.orderByDesc(FoodItem::getIsSystem)
                .orderByDesc(FoodItem::getUpdateTime);
        return ResultUtils.success(foodItemService.list(wrapper));
    }

    @PostMapping("/save")
    public BaseResponse<FoodItem> saveFood(@RequestBody SaveFoodItemRequest requestBody,
                                           HttpServletRequest request) {
        User user = requireLogin(request);
        if (requestBody == null || StringUtils.isBlank(requestBody.getName())) {
            throw new BusincessException(PARAMS_ERROR, "食物名称不能为空");
        }

        boolean isAdmin = isAdmin(user);
        FoodItem foodItem;
        if (requestBody.getId() == null) {
            foodItem = new FoodItem();
            foodItem.setCreatedBy(user.getId());
            foodItem.setIsSystem(isAdmin ? 1 : 0);
        } else {
            foodItem = foodItemService.getById(requestBody.getId());
            if (foodItem == null || Integer.valueOf(1).equals(foodItem.getIsDelete())) {
                throw new BusincessException(NULL_ERROR, "食物不存在");
            }
            ensureEditable(user, foodItem);
        }

        fillFoodItem(foodItem, requestBody);
        foodItemService.saveOrUpdate(foodItem);
        return ResultUtils.success(foodItemService.getById(foodItem.getId()));
    }

    @PostMapping("/upload-image")
    public BaseResponse<String> uploadFoodImage(@RequestParam("file") MultipartFile file,
                                                @RequestParam(value = "foodId", required = false) Long foodId,
                                                HttpServletRequest request) {
        User user = requireLogin(request);
        if (foodId != null) {
            FoodItem foodItem = foodItemService.getById(foodId);
            if (foodItem == null) {
                throw new BusincessException(NULL_ERROR, "食物不存在");
            }
            ensureEditable(user, foodItem);
        }
        return ResultUtils.success(fileService.uploadFoodImage(file, user.getId()));
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteFood(@RequestBody Long foodId,
                                            HttpServletRequest request) {
        User user = requireLogin(request);
        if (foodId == null || foodId <= 0) {
            throw new BusincessException(PARAMS_ERROR, "食物ID不能为空");
        }
        FoodItem foodItem = foodItemService.getById(foodId);
        if (foodItem == null || Integer.valueOf(1).equals(foodItem.getIsDelete())) {
            throw new BusincessException(NULL_ERROR, "食物不存在");
        }
        ensureEditable(user, foodItem);
        foodItem.setIsDelete(1);
        return ResultUtils.success(foodItemService.updateById(foodItem));
    }

    private void fillFoodItem(FoodItem foodItem, SaveFoodItemRequest body) {
        foodItem.setName(body.getName().trim());
        foodItem.setImageUrl(StringUtils.trimToNull(body.getImageUrl()));
        foodItem.setCategory(StringUtils.trimToNull(body.getCategory()));
        foodItem.setUnit(StringUtils.defaultIfBlank(body.getUnit(), "g"));
        foodItem.setBaseAmount(normalize(body.getBaseAmount(), BigDecimal.valueOf(100)));
        foodItem.setCalories(normalize(body.getCalories(), BigDecimal.ZERO));
        foodItem.setProtein(normalize(body.getProtein(), BigDecimal.ZERO));
        foodItem.setCarbs(normalize(body.getCarbs(), BigDecimal.ZERO));
        foodItem.setFat(normalize(body.getFat(), BigDecimal.ZERO));
        foodItem.setFiber(normalize(body.getFiber(), BigDecimal.ZERO));
    }

    private BigDecimal normalize(BigDecimal value, BigDecimal defaultValue) {
        return value == null ? defaultValue : value.setScale(2, RoundingMode.HALF_UP);
    }

    private void ensureEditable(User user, FoodItem foodItem) {
        if (isAdmin(user)) {
            return;
        }
        if (foodItem.getCreatedBy() == null || !foodItem.getCreatedBy().equals(user.getId())) {
            throw new BusincessException(NO_AUTH, "只能编辑自己创建的食物");
        }
    }

    private User requireLogin(HttpServletRequest request) {
        Object current = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (!(current instanceof User user)) {
            throw new BusincessException(NOT_LOGIN, "未登录");
        }
        return user;
    }

    private void requireAdmin(HttpServletRequest request) {
        User user = requireLogin(request);
        if (!isAdmin(user)) {
            throw new BusincessException(NO_AUTH, "无权限");
        }
    }

    private boolean isAdmin(User user) {
        return user != null && user.getUserRole() != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
    }
}
