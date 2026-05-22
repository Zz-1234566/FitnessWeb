import { getCaptcha } from '@/services/ant-design-pro/login';
import { useCallback, useEffect, useState } from 'react';

export const useCaptchaImage = () => {
  const [captchaImage, setCaptchaImage] = useState('');

  const refreshCaptcha = useCallback(async () => {
    const res = await getCaptcha();
    if (res?.captchaImage) {
      setCaptchaImage(res.captchaImage);
    }
  }, []);

  useEffect(() => {
    refreshCaptcha();
  }, [refreshCaptcha]);

  return {
    captchaImage,
    refreshCaptcha,
  };
};
