package com.zz.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UpdatePasswordRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String newPassword;

    private String checkPassword;

    private String captcha;
}