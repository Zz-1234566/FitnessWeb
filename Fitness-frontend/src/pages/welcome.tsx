import { history, useModel } from '@umijs/max';
import React, { useEffect, useMemo, useRef, useState } from 'react';
import useEnsureCurrentUser from '@/hooks/useEnsureCurrentUser';
import { getGreeting } from '@/utils/greeting';
import './welcome.less';

const FEATURES = [
  { key: 'coach', icon: '/icons/chat.svg', title: 'AI 教练', href: '/user/chat' },
  { key: 'muscles', icon: '/icons/nav.svg', title: '肌肉导航', href: '/muscles' },
  { key: 'training', icon: '/icons/training.svg', title: '训练计划', href: '/exercises?panel=training' },
  { key: 'diet', icon: '/icons/diet.svg', title: '饮食管理', href: '/exercises?panel=diet' },
];

/** 最终就位角度：右上 → 右下 → 左下 → 左上（顺时针） */
const REST_ANGLES = [315, 45, 135, 225];

const Welcome: React.FC = () => {
  useEnsureCurrentUser();
  const { initialState } = useModel('@@initialState');
  const currentUser = initialState?.currentUser;
  const greeting = getGreeting();
  const displayName = currentUser?.username?.trim() || '朋友';
  const [phase, setPhase] = useState<'intro' | 'settled'>('intro');
  const [activeIndex, setActiveIndex] = useState(0);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const nodesRef = useRef<(HTMLDivElement | null)[]>([]);

  /** 获取轨道半径（从 CSS 变量） */
  const getRadius = () => {
    const base = parseInt(
      getComputedStyle(document.documentElement).getPropertyValue('--wc-orbit-r'),
    );
    return base + 25;
  };

  /** 用 rotate+translateX+counter-rotate 定位 */
  const placeNode = (el: HTMLDivElement, deg: number) => {
    const r = getRadius();
    el.style.transform = `rotate(${deg}deg) translateX(${r}px) rotate(-${deg}deg)`;
  };

  /** 开场动画结束 → settled */
  const onIntroEnd = () => {
    const nodes = nodesRef.current.filter(Boolean) as HTMLDivElement[];

    nodes.forEach((el, i) => {
      // 读取浏览器计算的最终 transform 快照
      const computed = getComputedStyle(el).transform;
      // 去掉动画，冻结在当前位置
      el.style.animation = 'none';
      el.style.transform = computed;
      el.classList.add('wc-node--settled');

      // 双 rAF：下一帧切换为 rotate+translateX 格式（位置一致无跳动）
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          placeNode(el, REST_ANGLES[i]);
        });
      });
    });

    // 显示轨道环 + 品牌信息
    setTimeout(() => {
      setPhase('settled');
      setActiveIndex(0);
    }, 350);
  };

  /** 切换高亮 */
  useEffect(() => {
    if (phase !== 'settled') return;

    timerRef.current = setInterval(() => {
      setActiveIndex((prev) => (prev + 1) % FEATURES.length);
    }, 3000);

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [phase]);

  /** 开场动画计时 */
  useEffect(() => {
    const t = setTimeout(onIntroEnd, 3500);
    return () => clearTimeout(t);
  }, []);

  const handleClick = (href: string) => {
    if (phase !== 'settled') return;
    history.push(href);
  };

  const heroMessage = useMemo(
    () => `${greeting}，${displayName}`,
    [greeting, displayName],
  );

  return (
    <div className="wc-page">
      <div className="wc-stage">
        <div className="wc-orbit-stage">
          <div className="wc-orbit-ring" />

          {FEATURES.map((item, i) => (
            <div
              key={item.key}
              ref={(el) => { nodesRef.current[i] = el; }}
              className={[
                'wc-node',
                'wc-node--spin',
                i === 1 && 'wc-node--d1',
                i === 2 && 'wc-node--d2',
                i === 3 && 'wc-node--d3',
                phase === 'settled' && i === activeIndex && 'wc-node--active',
                phase === 'settled' && i !== activeIndex && 'wc-node--dim',
              ]
                .filter(Boolean)
                .join(' ')}
              onClick={() => handleClick(item.href)}
            >
              <img className="wc-node__icon" src={item.icon} alt={item.title} draggable={false} />
              <div className="wc-node__label">{item.title}</div>
            </div>
          ))}

          <img className="wc-logo" src="/ZZ.png" alt="Tatan" draggable={false} />
        </div>

        <div className={['wc-brand', phase === 'settled' && 'wc-brand--show'].filter(Boolean).join(' ')}>
          <div className="wc-brand__name">Tatan</div>
          <div className="wc-brand__sub">{heroMessage}</div>
        </div>
      </div>
    </div>
  );
};

export default Welcome;
