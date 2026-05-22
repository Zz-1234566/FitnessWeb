package com.zz.usercenter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@SpringBootTest
class UserCenterApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void testDigest() throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        String newPassword = DigestUtils.md5DigestAsHex(("abcd" + "myPassword").getBytes());
        System.out.println(newPassword);
    }


}
