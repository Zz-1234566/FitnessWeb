package com.zz.usercenter.model.domain.request;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CaptchaVO {

    @Serial
    private static final long serialVersionUID = -2732157919277108022L;

    /**
     * 验证码图片
     */
    private String captchaImage;
}
