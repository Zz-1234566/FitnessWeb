import { Footer } from '@/components';
import { register } from '@/services/ant-design-pro/api';
import {
  HeartOutlined,
  LockOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
  ThunderboltFilled,
  UserOutlined,
} from '@ant-design/icons';
import { Helmet } from '@umijs/max';
import { App, Button, Form, Input } from 'antd';
import React, { useCallback, useState } from 'react';
import { SYSTEM_LOGO } from '@/constants';
import PasswordStrengthBar from '@/components/PasswordStrengthBar';
import { useCaptchaImage } from '@/hooks/useCaptchaImage';
import { flushSync } from 'react-dom';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useModel } from '@umijs/max';
import '../auth.less';

const APP_TITLE = 'Tatan';

const features = [
  { icon: <TeamOutlined />, title: '免费注册', desc: '一键创建账号，即刻开始训练' },
  { icon: <ThunderboltFilled />, title: '个性化方案', desc: 'AI 根据你的目标定制训练计划' },
  { icon: <SafetyCertificateOutlined />, title: '隐私保护', desc: '数据加密存储，安全可信赖' },
];

const Register: React.FC = () => {
  const { captchaImage, refreshCaptcha } = useCaptchaImage();
  const [password, setPassword] = useState('');
  const { initialState, setInitialState } = useModel('@@initialState');
  const { message } = App.useApp();
  const navigate = useNavigate();
  const location = useLocation();
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 });

  const fetchUserInfo = async () => {
    const userInfo = await initialState?.fetchUserInfo?.();
    if (userInfo) {
      flushSync(() => {
        setInitialState((s) => ({ ...s, currentUser: userInfo }));
      });
    }
  };

  const handleSubmit = async (values: API.RegisterParams) => {
    const { userPassword, checkPassword } = values;
    if (userPassword !== checkPassword) {
      message.error('两次输入的密码不一致');
      return;
    }
    try {
      const id = await register(values);
      if (id) {
        message.success('注册成功！');
        setTimeout(async () => {
          await fetchUserInfo();
          const query = new URLSearchParams(location.search);
          navigate(query.get('redirect') || '/user/Login', { replace: true });
        }, 1500);
      }
    } catch {
      await refreshCaptcha();
    }
  };

  const handleMouseMove = useCallback(
    (e: React.MouseEvent<HTMLDivElement>) => {
      const rect = e.currentTarget.getBoundingClientRect();
      const x = ((e.clientX - rect.left) / rect.width - 0.5) * 2;
      const y = ((e.clientY - rect.top) / rect.height - 0.5) * 2;
      setMousePos({ x, y });
    },
    [],
  );

  const parallax = (depth: number) => ({
    transform: `translate(${mousePos.x * depth * 14}px, ${mousePos.y * depth * 14}px)`,
    transition: 'transform 0.15s ease-out',
  });

  return (
    <div className="auth-container">
      <Helmet>
        <title>注册 - {APP_TITLE}</title>
      </Helmet>

      <div className="auth-left" onMouseMove={handleMouseMove}>
        <div style={parallax(1.6)}><div className="auth-orb auth-orb-1" /></div>
        <div style={parallax(-1.1)}><div className="auth-orb auth-orb-2" /></div>
        <div style={parallax(0.7)}><div className="auth-orb auth-orb-3" /></div>
        <div style={parallax(-1.4)}><div className="auth-orb auth-orb-4" /></div>
        <div style={parallax(0.9)}><div className="auth-orb auth-orb-5" /></div>

        <div className="auth-fitness-icon auth-fi-1" style={parallax(2.2)}><HeartOutlined /></div>
        <div className="auth-fitness-icon auth-fi-2" style={parallax(-1.6)}><ThunderboltFilled /></div>
        <div className="auth-fitness-icon auth-fi-3" style={parallax(1.8)}><TeamOutlined /></div>

        <div className="auth-left-content">
          <div className="auth-brand-logo">
            <img src={SYSTEM_LOGO} alt="Tatan" />
          </div>
          <h1 className="auth-brand-title">加入 Tatan</h1>
          <p className="auth-brand-subtitle">开启你的智能健身之旅</p>

          <div className="auth-heartbeat">
            <svg viewBox="0 0 300 60" preserveAspectRatio="none">
              <path
                className="auth-pulse-line"
                d="M0 30 L60 30 L70 30 L80 10 L90 50 L100 20 L110 40 L120 30 L180 30 L190 30 L200 15 L210 45 L220 25 L230 35 L240 30 L300 30"
              />
            </svg>
          </div>

          <div className="auth-features">
            {features.map((f, i) => (
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
      </div>

      <div className="auth-right">
        <div className="auth-decor-line" />
        <div className="auth-particles">
          {[1, 2, 3, 4, 5, 6].map((n) => (
            <div key={n} className={`auth-particle auth-p-${n}`} />
          ))}
        </div>

        <div className="auth-form-card">
          <div className="auth-form-header">
            <div className="auth-form-logo">
              <img src={SYSTEM_LOGO} alt="Tatan" />
            </div>
            <div className="auth-form-header-text">
              <h2 className="auth-form-title">创建账号</h2>
              <p className="auth-form-subtitle">注册后即可体验 AI 健身教练</p>
            </div>
          </div>

          <Form<API.RegisterParams>
            onFinish={handleSubmit}
            autoComplete="off"
            size="large"
          >
            <Form.Item name="userAccount" rules={[{ required: true, message: '请输入账号' }, { min: 4, message: '账号不能少于4位' }]}>
              <Input prefix={<UserOutlined style={{ color: '#8FA69A' }} />} placeholder="请输入账号" className="auth-input" />
            </Form.Item>
            <Form.Item name="userPassword" rules={[{ required: true, message: '请输入密码' }, { min: 8, message: '密码不能少于八位' }]}>
              <Input.Password
                prefix={<LockOutlined style={{ color: '#8FA69A' }} />}
                placeholder="请输入密码"
                className="auth-input"
                onChange={(e) => setPassword(e.target.value)}
              />
            </Form.Item>
            <PasswordStrengthBar password={password} />
            <Form.Item name="checkPassword" rules={[{ required: true, message: '请再次输入密码' }, { min: 8, message: '密码不能少于八位' }]}>
              <Input.Password prefix={<LockOutlined style={{ color: '#8FA69A' }} />} placeholder="请再次输入密码" className="auth-input" />
            </Form.Item>
            <div className="auth-captcha-row">
              <Form.Item name="captcha" rules={[{ required: true, message: '请输入验证码' }]} style={{ flex: 1, minWidth: 0 }}>
                <Input placeholder="验证码不区分大小写" className="auth-input" />
              </Form.Item>
              <div>
                <img
                  src={captchaImage || ''}
                  alt="验证码"
                  onClick={refreshCaptcha}
                  className="auth-captcha-img"
                />
                <div className="auth-captcha-hint">点击图片刷新</div>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 16 }}>
              <Link to="/user/Login" className="auth-link">已有账号？去登录</Link>
            </div>

            <Button type="primary" htmlType="submit" block className="auth-submit-btn">
              注册
            </Button>
          </Form>
        </div>

        <div className="auth-footer"><Footer /></div>
      </div>
    </div>
  );
};

export default Register;
