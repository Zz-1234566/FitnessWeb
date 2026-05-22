import type { RequestConfig } from '@umijs/max';
import envDev from '../config/env.dev';
import envProd from '../config/env.prod';

const env = process.env.NODE_ENV === 'production' ? envProd : envDev;

export const errorConfig: RequestConfig = {
  // API 基础地址 - 从 config/env.*.ts 读取
  baseURL: env.API_BASE_URL,
};
