// @ts-ignore
/* eslint-disable */
import { request } from '@umijs/max';

/** 获取验证码 GET /api/user/captcha */
export async function getCaptcha() {
  return request<API.CaptchaResult>('/api/user/captcha', {
    method: 'GET',
  });
}
