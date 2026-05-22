import { Footer } from '@/components';
import { login } from '@/services/ant-design-pro/api';
import {
  HeartOutlined,
  LockOutlined,
  ThunderboltFilled,
  TrophyFilled,
  UserOutlined,
} from '@ant-design/icons';
import { Helmet, history, useModel } from '@umijs/max';
import { App, Button, Checkbox, Form, Input } from 'antd';
import React, { useCallback, useState } from 'react';
import { SYSTEM_LOGO } from '@/constants';
import { flushSync } from 'react-dom';
import { Link } from 'react-router-dom';
import '../auth.less';

const APP_TITLE = 'Tatan';

const features = [
  { icon: <ThunderboltFilled />, title: 'AI 智能教练', desc: '多模型驱动，个性化训练方案' },
  { icon: <HeartOutlined />, title: '科学训练', desc: '肌群精准定位，动作视频指导' },
  { icon: <TrophyFilled />, title: '持续蜕变', desc: 'AI 用户画像，记录每一步成长' },
];

const Login: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const { initialState, setInitialState } = useModel('@@initialState');
  const { message } = App.useApp();
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 });

  const fetchUserInfo = async () => {
    const userInfo = await initialState?.fetchUserInfo?.();
    if (userInfo) {
      flushSync(() => {
        setInitialState((s) => ({ ...s, currentUser: userInfo }));
      });
    }
  };

  const handleSubmit = async (values: API.LoginParams) => {
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
        <title>登录 - {APP_TITLE}</title>
      </Helmet>

      <div className="auth-left" onMouseMove={handleMouseMove}>
        <div style={parallax(1.6)}><div className="auth-orb auth-orb-1" /></div>
        <div style={parallax(-1.1)}><div className="auth-orb auth-orb-2" /></div>
        <div style={parallax(0.7)}><div className="auth-orb auth-orb-3" /></div>
        <div style={parallax(-1.4)}><div className="auth-orb auth-orb-4" /></div>
        <div style={parallax(0.9)}><div className="auth-orb auth-orb-5" /></div>

        <div className="auth-fitness-icon auth-fi-1" style={parallax(2.2)}><HeartOutlined /></div>
        <div className="auth-fitness-icon auth-fi-2" style={parallax(-1.6)}><ThunderboltFilled /></div>
        <div className="auth-fitness-icon auth-fi-3" style={parallax(1.8)}><TrophyFilled /></div>

        <div className="auth-left-content">
          <div className="auth-brand-logo">
            <img src={SYSTEM_LOGO} alt="Tatan" />
          </div>
          <h1 className="auth-brand-title">Tatan 智能健身平台</h1>
          <p className="auth-brand-subtitle">AI 驱动 · 科学训练 · 高效塑形</p>

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
              <h2 className="auth-form-title">欢迎回来</h2>
              <p className="auth-form-subtitle">登录以继续你的健身之旅</p>
            </div>
          </div>

          <Form<API.LoginParams>
            onFinish={handleSubmit}
            autoComplete="off"
            size="large"
            initialValues={{ autoLogin: true }}
          >
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
              <Link to="/user/register" className="auth-link">没有账号？立即注册</Link>
            </div>

            <Button type="primary" htmlType="submit" loading={loading} block className="auth-submit-btn">
              登录
            </Button>
          </Form>
        </div>

        <div className="auth-footer"><Footer /></div>
      </div>
    </div>
  );
};

export default Login;
