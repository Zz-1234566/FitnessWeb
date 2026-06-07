import Footer from '@/components/Footer';
import CustomLayout from '@/components/CustomLayout';
import { SitePreferenceProvider } from '@/contexts/SitePreferenceContext';
import {currentUser as queryCurrentUser} from '@/services/ant-design-pro/api';
import '@ant-design/v5-patch-for-react-19';
import type {
  RequestConfig,
  RunTimeLayoutConfig
} from '@umijs/max';
import {history} from '@umijs/max';
import {errorConfig} from "@/requestErrorConfig";
import {message} from "antd";
import {NOT_LOGIN} from "@/constants";

const loginPath = '/user/Login';
const authMessageKey = 'auth-required';
/**
 * 不需要登录态就可以访问的页面
 */
const NO_NEED_LOGIN_WHITELIST = ['/', '/user/register', loginPath, '/welcome', '/exercises'];

const normalizePath = (pathname: string) => {
  if (!pathname) return '/';
  const normalized = pathname.replace(/\/+$/, '') || '/';
  return normalized.toLowerCase();
};

const PUBLIC_ROUTE_SET = new Set(NO_NEED_LOGIN_WHITELIST.map(normalizePath));
const AUTH_PAGE_SET = new Set(['/user/login', '/user/register', '/user/updatepassword']);

const isPublicRoute = (pathname: string) => PUBLIC_ROUTE_SET.has(normalizePath(pathname));
const isAuthPage = (pathname: string) => AUTH_PAGE_SET.has(normalizePath(pathname));

const isAuthErrorMessage = (msg?: string) =>
  Boolean(msg && /(未登录|请先登录|登录已失效|login)/i.test(msg));

/**
 * @see https://umijs.org/docs/api/runtime-config#getinitialstate
 * */
export async function getInitialState(): Promise<{
  currentUser?: API.CurrentUser;
  loading?: boolean;
  fetchUserInfo?: () => Promise<API.CurrentUser | undefined>;
  summaryNotice?: API.SummaryNotification | null;
}> {
  const fetchUserInfo = async () => {
    try {
      const currentUser = await queryCurrentUser({
        skipErrorHandler: true,
      });
      return currentUser;
    } catch (_error) {
      // 不在这里跳转，交给 onPageChange 处理
    }
    return undefined;
  };

  // 无论什么页面都先尝试获取用户信息
  const currentUser = await fetchUserInfo();
  return {
    fetchUserInfo,
    currentUser,
  };
}

// 隐藏 ProLayout 原生渲染，使用自定义导航栏
export const layout: RunTimeLayoutConfig = ({ initialState }) => {
  return {
    // 隐藏 ProLayout 所有原生渲染
    headerRender: false,
    footerRender: false,
    menuRender: false,
    headerTitleRender: () => null,

    // 路由切换时检查登录态
    onPageChange: () => {
      const { location } = history;
      const pathname = location.pathname;
      const authPage = isAuthPage(pathname);
      document.body.classList.toggle('auth-page', authPage);
      document.documentElement.classList.toggle('auth-page', authPage);
      if (isPublicRoute(pathname)) {
        return;
      }
      if (!initialState?.currentUser) {
        const redirect = `${location.pathname}${location.search || ''}`;
        history.replace({
          pathname: loginPath,
          search: `?redirect=${encodeURIComponent(redirect)}`,
        });
        message.open({
          key: authMessageKey,
          type: 'error',
          content: NOT_LOGIN,
        });
      }
    },

    // 自定义布局：导航栏 + 内容 + 页脚
    childrenRender: (children) => {
      const pathname = history.location.pathname;
      const noLayoutPages = ['/user/Login', '/user/register'];
      if (noLayoutPages.includes(pathname)) {
        return <SitePreferenceProvider>{children}</SitePreferenceProvider>;
      }
      return (
        <SitePreferenceProvider>
          <CustomLayout>{children}</CustomLayout>
        </SitePreferenceProvider>
      );
    },
  };
};

/**
 * @name request 配置，可以配置错误处理
 * 它基于 axios 和 ahooks 的 useRequest 提供了一套统一的网络请求和错误处理方案。
 * @doc https://umijs.org/docs/max/request#配置
 */
export const request: RequestConfig = {
  ...errorConfig,
  timeout: 30000,
  // 响应拦截器：提取数据，失败时抛错（不弹窗）
  responseInterceptors: [
    (response) => {
      const res = response.data as API.BaseResponse<any>;
      if (res.code === 0) {
        response.data = res.data;
        return response;
      } else {
        throw new Error(res.description || '请求失败');
      }
    },
  ],

  // 错误处理器：通过 opts.skipErrorHandler 控制是否弹窗
  errorConfig: {
    errorHandler: (error: any, opts: any) => {
      if (!opts?.skipErrorHandler && !isAuthErrorMessage(error?.message)) {
        message.error(error.message);
      }
      throw error;
    },
  },
};
