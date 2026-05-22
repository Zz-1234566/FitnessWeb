import { Footer } from '@/components';
import PasswordStrengthBar from '@/components/PasswordStrengthBar';
import { SYSTEM_LOGO } from '@/constants';
import { useCaptchaImage } from '@/hooks/useCaptchaImage';
import { login, register, updatePassword } from '@/services/ant-design-pro/api';
import {
  HeartOutlined,
  LockOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
  ThunderboltFilled,
  TrophyFilled,
  UserOutlined,
} from '@ant-design/icons';
import { Helmet, history, useModel } from '@umijs/max';
import { App, Button, Checkbox, Form, Input } from 'antd';
import React, { useCallback, useEffect, useState } from 'react';
import { flushSync } from 'react-dom';
import { useLocation } from 'react-router-dom';
import '../auth.less';

const APP_TITLE = 'Tatan';

type AuthMode = 'login' | 'register' | 'updatePassword';

const loginFeatures = [
  { icon: <ThunderboltFilled />, title: 'AI 智能教练', desc: '多模型驱动，个性化训练方案' },
  { icon: <HeartOutlined />, title: '科学训练', desc: '肌群精准定位，动作视频指导' },
  { icon: <TrophyFilled />, title: '持续蜕变', desc: 'AI 用户画像，记录每一步成长' },
];

const registerFeatures = [
  { icon: <TeamOutlined />, title: '免费注册', desc: '一键创建账号，即刻开始训练' },
  { icon: <ThunderboltFilled />, title: '个性化方案', desc: 'AI 根据你的目标定制训练计划' },
  { icon: <SafetyCertificateOutlined />, title: '隐私保护', desc: '数据加密存储，安全可信赖' },
];

const updatePasswordFeatures = [
  { icon: <SafetyCertificateOutlined />, title: '安全加密', desc: '密码加密存储，多重安全防护' },
  { icon: <LockOutlined />, title: '定期更新', desc: '建议定期更换密码，保障账号安全' },
  { icon: <HeartOutlined />, title: '数据守护', desc: '你的健身数据，我们用心守护' },
];

const getInitialMode = (pathname: string): AuthMode => {
  if (pathname === '/user/register') return 'register';
  if (pathname === '/user/updatePassword') return 'updatePassword';
  return 'login';
};

const Auth: React.FC = () => {
  const location = useLocation();
  const { initialState, setInitialState } = useModel('@@initialState');
  const { message } = App.useApp();

  const [mode, setMode] = useState<AuthMode>(getInitialMode(location.pathname));
  const [animating, setAnimating] = useState(false);
  const [isMobileLayout, setIsMobileLayout] = useState(() =>
    typeof window !== 'undefined' ? window.matchMedia('(max-width: 768px)').matches : false,
  );

  // 登录状态
  const [loading, setLoading] = useState(false);

  // 注册 & 修改密码 共用
  const { captchaImage, refreshCaptcha } = useCaptchaImage();
  const [regPassword, setRegPassword] = useState('');
  const [updPassword, setUpdPassword] = useState('');

  // 视差
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 });
  const [spotlight, setSpotlight] = useState({ x: 50, y: 50 });
  const [ripples, setRipples] = useState<{ id: number; x: number; y: number; delay: number }[]>([]);

  useEffect(() => {
    const nextMode = getInitialMode(location.pathname);
    setMode((prev) => (prev === nextMode ? prev : nextMode));
  }, [location.pathname]);

  useEffect(() => {
    if (typeof window === 'undefined') return undefined;
    const media = window.matchMedia('(max-width: 768px)');
    const update = () => setIsMobileLayout(media.matches);
    update();
    media.addEventListener('change', update);
    return () => media.removeEventListener('change', update);
  }, []);

  const fetchUserInfo = async () => {
    const userInfo = await initialState?.fetchUserInfo?.();
    if (userInfo) {
      flushSync(() => {
        setInitialState((s) => ({ ...s, currentUser: userInfo }));
      });
    }
  };

  const switchMode = (next: AuthMode) => {
    if (animating || next === mode) return;
    if (isMobileLayout) {
      setAnimating(true);
      setMode(next);
      window.setTimeout(() => setAnimating(false), 360);
      return;
    }

    setAnimating(true);
    setMode(next);
    window.setTimeout(() => setAnimating(false), 950);
  };

  const handleLogin = async (values: API.LoginParams) => {
    setLoading(true);
    try {
      const user = await login({ ...values });
      if (user?.id > 0) {
        message.success('登录成功！');
        setTimeout(async () => {
          await fetchUserInfo();
          const urlParams = new URL(window.location.href).searchParams;
          history.push(urlParams.get('redirect') || '/welcome');
        }, 1000);
      } else {
        message.error('登录失败，请重试！');
      }
    } catch {
      message.error('登录失败，请重试！');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (values: API.RegisterParams) => {
    const { userPassword, checkPassword } = values;
    if (userPassword !== checkPassword) {
      message.error('两次输入的密码不一致');
      return;
    }
    try {
      const id = await register(values);
      if (id) {
        message.success('注册成功！');
        setTimeout(() => switchMode('login'), 800);
      }
    } catch {
      await refreshCaptcha();
    }
  };

  const handleUpdatePassword = async (values: API.UpdatePasswordParams) => {
    const { newPassword, checkPassword } = values;
    if (newPassword !== checkPassword) {
      message.error('两次输入的密码不一致');
      return;
    }
    if (newPassword.length < 8) {
      message.error('密码不能少于8位');
      return;
    }
    try {
      await updatePassword(values);
      message.success('密码修改成功，请重新登录');
      setTimeout(() => switchMode('login'), 1500);
    } catch {
      await refreshCaptcha();
    }
  };

  const handleMouseMove = useCallback(
    (e: React.MouseEvent<HTMLDivElement>) => {
      if (isMobileLayout) return;
      const rect = e.currentTarget.getBoundingClientRect();
      const x = ((e.clientX - rect.left) / rect.width - 0.5) * 2;
      const y = ((e.clientY - rect.top) / rect.height - 0.5) * 2;
      setMousePos({ x, y });
      setSpotlight({
        x: ((e.clientX - rect.left) / rect.width) * 100,
        y: ((e.clientY - rect.top) / rect.height) * 100,
      });
    },
    [isMobileLayout],
  );

  const handleBrandClick = useCallback(
    (e: React.MouseEvent<HTMLDivElement>) => {
      if (isMobileLayout) return;
      const rect = e.currentTarget.getBoundingClientRect();
      const baseId = Date.now();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;
      const newRipples = [0, 250, 500, 750].map((delay, i) => ({
        id: baseId + i,
        x,
        y,
        delay,
      }));
      setRipples((prev) => [...prev, ...newRipples]);
      setTimeout(() => {
        const ids = new Set(newRipples.map((r) => r.id));
        setRipples((prev) => prev.filter((r) => !ids.has(r.id)));
      }, 2800);
    },
    [isMobileLayout],
  );

  const parallax = (depth: number) => ({
    transform: `translate(${mousePos.x * depth * 14}px, ${mousePos.y * depth * 14}px)`,
    transition: 'transform 0.15s ease-out',
  });

  // 滑动方向：login 品牌在左，register / updatePassword 品牌在右
  const slideTo = mode === 'login' ? 'login' : 'register';
  const isBrandLeft = mode === 'login';

  const heartbeat = (
    <div className="auth-heartbeat">
      <svg viewBox="0 0 300 60" preserveAspectRatio="none">
        <path
          className="auth-pulse-line"
          d="M0 30 L60 30 L70 30 L80 10 L90 50 L100 20 L110 40 L120 30 L180 30 L190 30 L200 15 L210 45 L220 25 L230 35 L240 30 L300 30"
        />
      </svg>
    </div>
  );

  // ——— 品牌内容 ———
  const renderBrandContent = (m: AuthMode) => {
    const map = {
      login: { title: 'Tatan 智能健身平台', subtitle: 'AI 驱动 · 科学训练 · 高效塑形', features: loginFeatures, icons: [<HeartOutlined />, <ThunderboltFilled />, <TrophyFilled />] },
      register: { title: '加入 Tatan', subtitle: '开启你的智能健身之旅', features: registerFeatures, icons: [<TeamOutlined />, <ThunderboltFilled />, <SafetyCertificateOutlined />] },
      updatePassword: { title: '安全设置', subtitle: '保护你的账号安全', features: updatePasswordFeatures, icons: [<SafetyCertificateOutlined />, <LockOutlined />, <HeartOutlined />] },
    };
    const cfg = map[m];
    return (
      <div className="auth-left-content">
        <div className="auth-brand-logo">
          <img src={SYSTEM_LOGO} alt="Tatan" />
        </div>
        <h1 className="auth-brand-title">{cfg.title}</h1>
        <p className="auth-brand-subtitle">{cfg.subtitle}</p>
        {heartbeat}
        <div className="auth-features">
          {cfg.features.map((f, i) => (
            <div className="auth-feature-item" key={i}>
              <span className="auth-feature-icon">{f.icon}</span>
              <div>
                <span className="auth-feature-title">{f.title}</span>
                <span className="auth-feature-desc">{f.desc}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  };

  // ——— 表单内容 ———
  const renderLoginForm = () => (
    <div className="auth-form-card">
      <div className="auth-form-header">
        <div className="auth-form-logo"><img src={SYSTEM_LOGO} alt="Tatan" /></div>
        <div className="auth-form-header-text">
          <h2 className="auth-form-title">欢迎回来</h2>
          <p className="auth-form-subtitle">登录以继续你的健身之旅</p>
        </div>
      </div>
      <Form<API.LoginParams> onFinish={handleLogin} autoComplete="off" size="large" initialValues={{ autoLogin: true }}>
        <Form.Item name="userAccount" rules={[{ required: true, message: '请输入账号' }, { min: 4, message: '账号不能少于4位' }]}>
          <Input prefix={<UserOutlined style={{ color: '#8FA69A' }} />} placeholder="请输入账号" className="auth-input" />
        </Form.Item>
        <Form.Item name="userPassword" rules={[{ required: true, message: '请输入密码' }, { min: 8, message: '密码不能少于八位' }]}>
          <Input.Password prefix={<LockOutlined style={{ color: '#8FA69A' }} />} placeholder="请输入密码" className="auth-input" />
        </Form.Item>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
          <Form.Item name="autoLogin" valuePropName="checked" noStyle>
            <Checkbox>自动登录</Checkbox>
          </Form.Item>
          <a className="auth-link" onClick={() => switchMode('register')}>&larr; 没有账号？去注册</a>
        </div>
        <Button type="primary" htmlType="submit" loading={loading} block className="auth-submit-btn">
          登录
        </Button>
      </Form>
    </div>
  );

  const renderRegisterForm = () => (
    <div className="auth-form-card">
      <div className="auth-form-header">
        <div className="auth-form-logo"><img src={SYSTEM_LOGO} alt="Tatan" /></div>
        <div className="auth-form-header-text">
          <h2 className="auth-form-title">创建账号</h2>
          <p className="auth-form-subtitle">注册后即可体验 AI 健身教练</p>
        </div>
      </div>
      <Form<API.RegisterParams> onFinish={handleRegister} autoComplete="off" size="large">
        <Form.Item name="userAccount" rules={[{ required: true, message: '请输入账号' }, { min: 4, message: '账号不能少于4位' }]}>
          <Input prefix={<UserOutlined style={{ color: '#8FA69A' }} />} placeholder="请输入账号" className="auth-input" />
        </Form.Item>
        <Form.Item name="userPassword" rules={[{ required: true, message: '请输入密码' }, { min: 8, message: '密码不能少于八位' }]}>
          <Input.Password prefix={<LockOutlined style={{ color: '#8FA69A' }} />} placeholder="请输入密码" className="auth-input" onChange={(e) => setRegPassword(e.target.value)} />
        </Form.Item>
        <PasswordStrengthBar password={regPassword} />
        <Form.Item name="checkPassword" rules={[{ required: true, message: '请再次输入密码' }, { min: 8, message: '密码不能少于八位' }]}>
          <Input.Password prefix={<LockOutlined style={{ color: '#8FA69A' }} />} placeholder="请再次输入密码" className="auth-input" />
        </Form.Item>
        <div className="auth-captcha-row">
          <Form.Item name="captcha" rules={[{ required: true, message: '请输入验证码' }]} style={{ flex: 1, minWidth: 0 }}>
            <Input placeholder="验证码不区分大小写" className="auth-input" />
          </Form.Item>
          <div>
            <img src={captchaImage || ''} alt="验证码" onClick={refreshCaptcha} className="auth-captcha-img" />
            <div className="auth-captcha-hint">点击图片刷新</div>
          </div>
        </div>
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 16 }}>
          <a className="auth-link" onClick={() => switchMode('login')}>已有账号？去登录 &rarr;</a>
        </div>
        <Button type="primary" htmlType="submit" block className="auth-submit-btn">
          注册
        </Button>
      </Form>
    </div>
  );

  const renderUpdatePasswordForm = () => (
    <div className="auth-form-card">
      <div className="auth-form-header">
        <div className="auth-form-logo"><img src={SYSTEM_LOGO} alt="Tatan" /></div>
        <div className="auth-form-header-text">
          <h2 className="auth-form-title">修改密码</h2>
          <p className="auth-form-subtitle">设置新的登录密码</p>
        </div>
      </div>
      <Form<API.UpdatePasswordParams> onFinish={handleUpdatePassword} autoComplete="off" size="large">
        <Form.Item name="newPassword" rules={[{ required: true, message: '请输入新密码' }, { min: 8, message: '密码不能少于8位' }]}>
          <Input.Password prefix={<LockOutlined style={{ color: '#8FA69A' }} />} placeholder="请输入新密码" className="auth-input" onChange={(e) => setUpdPassword(e.target.value)} />
        </Form.Item>
        <PasswordStrengthBar password={updPassword} />
        <Form.Item name="checkPassword" rules={[{ required: true, message: '请再次输入新密码' }, { min: 8, message: '密码不能少于8位' }]}>
          <Input.Password prefix={<LockOutlined style={{ color: '#8FA69A' }} />} placeholder="请再次输入新密码" className="auth-input" />
        </Form.Item>
        <div className="auth-captcha-row">
          <Form.Item name="captcha" rules={[{ required: true, message: '请输入验证码' }]} style={{ flex: 1, minWidth: 0 }}>
            <Input placeholder="验证码不区分大小写" className="auth-input" />
          </Form.Item>
          <div>
            <img src={captchaImage || ''} alt="验证码" onClick={refreshCaptcha} className="auth-captcha-img" />
            <div className="auth-captcha-hint">点击图片刷新</div>
          </div>
        </div>
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 16 }}>
          <a className="auth-link" onClick={() => switchMode('login')}>返回登录 &rarr;</a>
        </div>
        <Button type="primary" htmlType="submit" block className="auth-submit-btn">
          确认修改
        </Button>
      </Form>
    </div>
  );

  const renderCurrentForm = () => {
    if (mode === 'register') return renderRegisterForm();
    if (mode === 'updatePassword') return renderUpdatePasswordForm();
    return renderLoginForm();
  };

  return (
    <div className={`auth-slide-container${animating ? ' sliding' : ''}`} data-slide-to={slideTo}>
      <Helmet>
        <title>{mode === 'login' ? '登录' : mode === 'register' ? '注册' : '修改密码'} - {APP_TITLE}</title>
      </Helmet>

      {/* ===== 品牌面板 ===== */}
      <div
        className={`auth-left ${isBrandLeft ? 'panel-left' : 'panel-right'}`}
        onMouseMove={handleMouseMove}
        onClick={handleBrandClick}
        style={{ '--sl-x': `${spotlight.x}%`, '--sl-y': `${spotlight.y}%` } as React.CSSProperties}
      >
        {/* 鼠标聚光灯 */}
        <div className="auth-spotlight" />

        {/* 点击水波纹 */}
        {ripples.map((r) => (
          <div
            key={r.id}
            className="auth-ripple"
            style={{ left: r.x, top: r.y, animationDelay: `${r.delay}ms` }}
          />
        ))}
        <div style={parallax(1.6)}><div className="auth-orb auth-orb-1" /></div>
        <div style={parallax(-1.1)}><div className="auth-orb auth-orb-2" /></div>
        <div style={parallax(0.7)}><div className="auth-orb auth-orb-3" /></div>
        <div style={parallax(-1.4)}><div className="auth-orb auth-orb-4" /></div>
        <div style={parallax(0.9)}><div className="auth-orb auth-orb-5" /></div>

        <div className="auth-fitness-icon auth-fi-1" style={parallax(2.2)}><HeartOutlined /></div>
        <div className="auth-fitness-icon auth-fi-2" style={parallax(-1.6)}><ThunderboltFilled /></div>
        <div className="auth-fitness-icon auth-fi-3" style={parallax(1.8)}><TrophyFilled /></div>

        {/* 浮动健身器材 SVG */}
        <div className="auth-decor-svg auth-decor-dumbbell" style={parallax(1.2)}>
          <svg viewBox="0 0 120 36" fill="none"><rect x="30" y="14" width="60" height="8" rx="3" fill="currentColor" /><rect x="6" y="4" width="26" height="28" rx="5" fill="currentColor" /><rect x="88" y="4" width="26" height="28" rx="5" fill="currentColor" /></svg>
        </div>
        <div className="auth-decor-svg auth-decor-kettlebell" style={parallax(-0.9)}>
          <svg viewBox="0 0 56 72" fill="none"><ellipse cx="28" cy="48" rx="22" ry="20" fill="currentColor" /><path d="M16 28Q16 10 28 10Q40 10 40 28" stroke="currentColor" strokeWidth="6" fill="none" strokeLinecap="round" /></svg>
        </div>
        <div className="auth-decor-svg auth-decor-barbell" style={parallax(0.6)}>
          <svg viewBox="0 0 140 24" fill="none"><rect x="30" y="10" width="80" height="4" rx="2" fill="currentColor" /><rect x="4" y="2" width="28" height="20" rx="4" fill="currentColor" /><rect x="14" y="0" width="10" height="24" rx="2" fill="currentColor" /><rect x="108" y="2" width="28" height="20" rx="4" fill="currentColor" /><rect x="116" y="0" width="10" height="24" rx="2" fill="currentColor" /></svg>
        </div>
        <div className="auth-decor-svg auth-decor-speed" style={parallax(-1.3)}>
          <svg viewBox="0 0 64 64" fill="none"><circle cx="32" cy="32" r="28" stroke="currentColor" strokeWidth="4" /><line x1="32" y1="32" x2="44" y2="18" stroke="currentColor" strokeWidth="4" strokeLinecap="round" /><circle cx="32" cy="32" r="4" fill="currentColor" /></svg>
        </div>
        <div className="auth-decor-svg auth-decor-target" style={parallax(1.5)}>
          <svg viewBox="0 0 64 64" fill="none"><circle cx="32" cy="32" r="28" stroke="currentColor" strokeWidth="3" /><circle cx="32" cy="32" r="18" stroke="currentColor" strokeWidth="3" /><circle cx="32" cy="32" r="8" fill="currentColor" /></svg>
        </div>

        {(['login', 'register', 'updatePassword'] as AuthMode[]).map((m) => (
          <div key={m} className={`auth-content-fade ${mode === m ? 'show' : 'hide'}`}>
            {renderBrandContent(m)}
          </div>
        ))}
      </div>

      {/* ===== 表单面板 ===== */}
      <div className={`auth-right ${isBrandLeft ? 'panel-right' : 'panel-left'}`}>
        <div className="auth-decor-line" />
        <div className="auth-particles">
          {[1, 2, 3, 4, 5, 6].map((n) => (
            <div key={n} className={`auth-particle auth-p-${n}`} />
          ))}
        </div>

        {isMobileLayout ? (
          <div key={mode} className="auth-mobile-form-stage">
            {renderCurrentForm()}
          </div>
        ) : (
          <>
            <div className={`auth-content-fade ${mode === 'login' ? 'show' : 'hide'}`}>
              {renderLoginForm()}
            </div>
            <div className={`auth-content-fade ${mode === 'register' ? 'show' : 'hide'}`}>
              {renderRegisterForm()}
            </div>
            <div className={`auth-content-fade ${mode === 'updatePassword' ? 'show' : 'hide'}`}>
              {renderUpdatePasswordForm()}
            </div>
          </>
        )}

        <div className="auth-footer"><Footer /></div>
      </div>
    </div>
  );
};

export default Auth;
