import React, { useEffect, useRef, useState } from 'react';
import { history, useAccess, useLocation, useModel } from '@umijs/max';
import { flushSync } from 'react-dom';
import {
  BellOutlined,
  DeleteOutlined,
  DownOutlined,
  HomeOutlined,
  LockOutlined,
  LogoutOutlined,
  MoonOutlined,
  SettingOutlined,
  ShopOutlined,
  StarOutlined,
  SunOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { notification } from 'antd';
import {
  getSummaryNotification,
  markAllSummaryNotificationsRead,
  outLogin,
  updateSummaryNotificationStatus,
} from '@/services/ant-design-pro/api';
import { useSitePreferences } from '@/contexts/SitePreferenceContext';
import './index.less';
const NAV_ITEMS = [
  { path: '/welcome', label: '首页', icon: <HomeOutlined /> },
  { path: '/muscles', label: '肌肉导航', icon: <img className="nav-svg-icon" src="/icons/nav.svg" alt="" /> },
  { path: '/exercises', label: '动作库', icon: <img className="nav-svg-icon" src="/icons/exercises.svg" alt="" /> },
  { path: '/user/chat', label: 'AI聊天', icon: <img className="nav-svg-icon" src="/icons/chat.svg" alt="" /> },
];

const noticeToastKey = (id: string) => `summary_notice_toast_${id}`;

const getNextHalfHour = () => {
  const now = new Date();
  const minutes = now.getMinutes();
  const next = new Date(now);
  if (minutes < 30) {
    next.setMinutes(30, 0, 0);
  } else {
    next.setHours(next.getHours() + 1, 0, 0, 0);
  }
  return next;
};

type NoticeSection = {
  label: string;
  content: string;
};

const DAILY_SECTIONS = [
  { label: '总结', aliases: ['总结'] },
  { label: '建议', aliases: ['建议'] },
  { label: '训练', aliases: ['训练', '运动'] },
  { label: '饮食', aliases: ['饮食'] },
  { label: '问题', aliases: ['问题'] },
];

const WEEKLY_SECTIONS = [
  { label: '总结', aliases: ['总结'] },
  { label: '训练', aliases: ['训练'] },
  { label: '饮食', aliases: ['饮食'] },
  { label: '建议', aliases: ['建议'] },
];

const findSectionConfig = (line: string, type: 'daily' | 'weekly') => {
  const configs = type === 'weekly' ? WEEKLY_SECTIONS : DAILY_SECTIONS;
  return configs.find((config) =>
    config.aliases.some((alias) => line.startsWith(`${alias}：`) || line.startsWith(`${alias}:`)),
  );
};

const buildSections = (content: string, type: 'daily' | 'weekly'): NoticeSection[] => {
  const lines = content
    .replace(/\r/g, '')
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean);

  const sections: NoticeSection[] = [];
  let current: NoticeSection | null = null;

  lines.forEach((line) => {
    const config = findSectionConfig(line, type);
    if (config) {
      if (current?.content) {
        sections.push(current);
      }
      current = {
        label: config.label,
        content: line.replace(/^[^：:]+[：:]\s*/, '').trim(),
      };
      return;
    }

    if (current) {
      current.content = `${current.content}\n${line}`.trim();
      return;
    }

    if (!sections.length) {
      sections.push({ label: '内容', content: line });
    } else {
      sections[sections.length - 1].content = `${sections[sections.length - 1].content}\n${line}`.trim();
    }
  });

  if (current) {
    sections.push(current);
  }

  return sections.length ? sections : [{ label: '内容', content }];
};

const getCollapsedPreview = (card: API.SummaryNotificationCard) => {
  const sections = buildSections(card.content, card.type);
  const preferred = sections.find((section) => section.label === '建议') || sections.find((section) => section.label === '总结');
  return preferred?.content || card.preview;
};

const stripSectionLabels = (content: string, type: 'daily' | 'weekly') =>
  buildSections(content, type)
    .map((section) => section.content)
    .join('\n\n');

const CustomNavbar: React.FC = () => {
  const location = useLocation();
  const access = useAccess();
  const { initialState, setInitialState } = useModel('@@initialState');
  const { theme, toggleTheme } = useSitePreferences();
  const currentUser = initialState?.currentUser;

  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [noticeOpen, setNoticeOpen] = useState(false);
  const [summaryNotice, setSummaryNotice] = useState<API.SummaryNotification | null>(null);
  const [expandedNoticeCards, setExpandedNoticeCards] = useState<Set<string>>(new Set());
  const [deleteMode, setDeleteMode] = useState(false);
  const [selectedNoticeCardIds, setSelectedNoticeCardIds] = useState<Set<string>>(new Set());

  const navCenterRef = useRef<HTMLDivElement>(null);
  const indicatorRef = useRef<HTMLDivElement>(null);
  const itemRefs = useRef<(HTMLAnchorElement | null)[]>([]);
  const avatarBtnRef = useRef<HTMLButtonElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const noticeBtnRef = useRef<HTMLButtonElement>(null);
  const noticePanelRef = useRef<HTMLDivElement>(null);

  const activeIndex = NAV_ITEMS.findIndex(
    (item) => location.pathname === item.path || location.pathname.startsWith(`${item.path}/`),
  );

  const syncSummaryNotice = async (silent = false) => {
    if (!currentUser) {
      setSummaryNotice(null);
      return;
    }

    try {
      const notice = await getSummaryNotification({ skipErrorHandler: true });
      setSummaryNotice(notice);
      setInitialState((state) => ({ ...state, summaryNotice: notice || undefined }));

      if (!notice || !notice.cards?.length || !notice.hasUnread) {
        return;
      }

      if (!silent && localStorage.getItem(noticeToastKey(notice.id)) !== '1') {
        const firstCard = notice.cards[0];
        notification.open({
          key: notice.id,
          message: notice.title,
          description: firstCard ? getCollapsedPreview(firstCard) : '',
          placement: 'topRight',
          duration: 5,
        });
        localStorage.setItem(noticeToastKey(notice.id), '1');
      }
    } catch {
      setSummaryNotice(null);
    }
  };

  useEffect(() => {
    const moveIndicator = () => {
      if (!indicatorRef.current || !navCenterRef.current) return;
      if (activeIndex < 0) {
        indicatorRef.current.style.opacity = '0';
        return;
      }
      const el = itemRefs.current[activeIndex];
      if (!el) return;
      indicatorRef.current.style.left = `${el.offsetLeft}px`;
      indicatorRef.current.style.width = `${el.offsetWidth}px`;
      indicatorRef.current.style.opacity = '1';
      if (window.innerWidth <= 768) {
        el.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
      }
    };

    moveIndicator();
    window.addEventListener('resize', moveIndicator);
    return () => window.removeEventListener('resize', moveIndicator);
  }, [activeIndex]);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', onScroll);
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      const target = e.target as Node;
      if (
        avatarBtnRef.current &&
        !avatarBtnRef.current.contains(target) &&
        dropdownRef.current &&
        !dropdownRef.current.contains(target)
      ) {
        setMenuOpen(false);
      }
      if (
        noticeBtnRef.current &&
        !noticeBtnRef.current.contains(target) &&
        noticePanelRef.current &&
        !noticePanelRef.current.contains(target)
      ) {
        setNoticeOpen(false);
      }
    };

    document.addEventListener('click', handleClick);
    return () => document.removeEventListener('click', handleClick);
  }, []);

  useEffect(() => {
    let cancelled = false;
    let timer = 0;

    const scheduleNextRefresh = () => {
      window.clearTimeout(timer);
      timer = window.setTimeout(async () => {
        if (cancelled) return;
        await syncSummaryNotice(true);
        scheduleNextRefresh();
      }, Math.max(1000, getNextHalfHour().getTime() - Date.now()));
    };

    void syncSummaryNotice(false);
    scheduleNextRefresh();

    const handleVisibilityChange = () => {
      if (!document.hidden) {
        void syncSummaryNotice(true);
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [currentUser?.id]);

  useEffect(() => {
    setExpandedNoticeCards(new Set());
    setDeleteMode(false);
    setSelectedNoticeCardIds(new Set());
  }, [summaryNotice?.id]);

  const toggleNoticeCard = async (card: API.SummaryNotificationCard) => {
    const isExpanding = !expandedNoticeCards.has(card.id);
    setExpandedNoticeCards((prev) => {
      const next = new Set(prev);
      if (next.has(card.id)) {
        next.delete(card.id);
      } else {
        next.add(card.id);
      }
      return next;
    });

    if (isExpanding && !card.read) {
      setSummaryNotice((prev) => {
        if (!prev) return prev;
        const cards = prev.cards.map((item) => (item.id === card.id ? { ...item, read: true } : item));
        const unreadCount = cards.filter((item) => !item.read).length;
        return { ...prev, cards, unreadCount, hasUnread: unreadCount > 0 };
      });
      try {
        await updateSummaryNotificationStatus(card.id, { read: true }, { skipErrorHandler: true });
      } catch {
        void syncSummaryNotice(true);
      }
    }
  };

  const handleClearNotice = async (cardId: string) => {
    const prevNotice = summaryNotice;
    setSummaryNotice((prev) => {
      if (!prev) return prev;
      const cards = prev.cards.filter((item) => item.id !== cardId);
      const unreadCount = cards.filter((item) => !item.read).length;
      if (!cards.length) return null;
      return {
        ...prev,
        cards,
        unreadCount,
        hasUnread: unreadCount > 0,
      };
    });
    setExpandedNoticeCards((prev) => {
      const next = new Set(prev);
      next.delete(cardId);
      return next;
    });
    try {
      await updateSummaryNotificationStatus(cardId, { cleared: true }, { skipErrorHandler: true });
    } catch {
      setSummaryNotice(prevNotice);
      void syncSummaryNotice(true);
    }
  };

  const handleMarkAllRead = async () => {
    const prevNotice = summaryNotice;
    setSummaryNotice((prev) => {
      if (!prev) return prev;
      const cards = prev.cards.map((item) => ({ ...item, read: true }));
      return { ...prev, cards, unreadCount: 0, hasUnread: false };
    });
    try {
      await markAllSummaryNotificationsRead({ skipErrorHandler: true });
    } catch {
      setSummaryNotice(prevNotice);
      void syncSummaryNotice(true);
    }
  };

  const toggleDeleteSelection = (cardId: string) => {
    setSelectedNoticeCardIds((prev) => {
      const next = new Set(prev);
      if (next.has(cardId)) {
        next.delete(cardId);
      } else {
        next.add(cardId);
      }
      return next;
    });
  };

  const handleClearSelectedNotices = async () => {
    if (!summaryNotice?.cards?.length || selectedNoticeCardIds.size === 0) return;
    const prevNotice = summaryNotice;
    const cardIds = summaryNotice.cards
      .filter((card) => selectedNoticeCardIds.has(card.id))
      .map((card) => card.id);
    setSummaryNotice((prev) => {
      if (!prev) return prev;
      const cards = prev.cards.filter((card) => !selectedNoticeCardIds.has(card.id));
      const unreadCount = cards.filter((item) => !item.read).length;
      if (!cards.length) return null;
      return { ...prev, cards, unreadCount, hasUnread: unreadCount > 0 };
    });
    setExpandedNoticeCards((prev) => {
      const next = new Set(prev);
      cardIds.forEach((id) => next.delete(id));
      return next;
    });
    setDeleteMode(false);
    setSelectedNoticeCardIds(new Set());
    try {
      await Promise.all(
        cardIds.map((cardId) =>
          updateSummaryNotificationStatus(cardId, { cleared: true }, { skipErrorHandler: true }),
        ),
      );
    } catch {
      setSummaryNotice(prevNotice);
      void syncSummaryNotice(true);
    }
  };

  const handleNoticeClick = () => {
    const nextOpen = !noticeOpen;
    setNoticeOpen(nextOpen);
    setMenuOpen(false);
  };

  const handleLogout = async () => {
    setMenuOpen(false);
    setNoticeOpen(false);
    flushSync(() => {
      setInitialState((state) => ({ ...state, currentUser: undefined }));
    });
    await outLogin();
    history.replace('/user/Login');
  };

  const handleMenuAction = (key: string) => {
    setMenuOpen(false);
    if (key === 'profile') history.push('/user/profile');
    if (key === 'favorites') history.push('/user/favorites');
    if (key === 'password') history.push('/user/updatePassword');
    if (key === 'logout') void handleLogout();
  };

  const firstChar = currentUser?.username?.charAt(0)?.toUpperCase() || 'U';
  const unreadCount = summaryNotice?.unreadCount || 0;

  return (
    <div className={`navbar${scrolled ? ' scrolled' : ''}`}>
      <div className="navInner">
        <a className="navBrand" onClick={() => history.push('/welcome')}>
          <div className="navLogo">
            <img src="/ZZ.png" alt="Tatan" />
          </div>
          <span className="navTitle">Tatan</span>
        </a>

        <div className="navCenter" ref={navCenterRef}>
          <div className="navIndicator" ref={indicatorRef} />
          {NAV_ITEMS.map((item, idx) => (
            <a
              key={item.path}
              ref={(el) => {
                itemRefs.current[idx] = el;
              }}
              className={`navItem${activeIndex === idx ? ' active' : ''}`}
              onClick={() => history.push(item.path)}
            >
              {item.icon}
              <span>{item.label}</span>
            </a>
          ))}
        </div>

        <div className="navRight">
          <div className="navSwitchGroup" aria-label="模式">
            <button
              type="button"
              className={`navSwitchBtn${theme === 'light' ? ' active' : ''}`}
              onClick={toggleTheme}
              title="模式"
            >
              {theme === 'light' ? <SunOutlined /> : <MoonOutlined />}
            </button>
          </div>
          {access.canAdmin && (
            <button
              className="navAdmin"
              onClick={() => history.push('/admin/user-manage')}
              aria-label="管理"
              title="用户管理"
            >
              <SettingOutlined />
            </button>
          )}
          <button
            className="navAdmin"
            onClick={() => history.push('/user/food-manage')}
            aria-label="食物管理"
            title="食物管理"
          >
            <ShopOutlined />
          </button>

          {currentUser ? (
            <>
              <button
                ref={noticeBtnRef}
                className={`noticeBtn${noticeOpen ? ' open' : ''}`}
                onClick={handleNoticeClick}
                aria-label="消息通知"
              >
                <BellOutlined />
                {unreadCount > 0 && (
                  <span className="noticeBadge">{unreadCount > 99 ? '99+' : unreadCount}</span>
                )}
              </button>

              <div ref={noticePanelRef} className={`noticePanel${noticeOpen ? ' open' : ''}`} onClick={(e) => e.stopPropagation()}>
                {summaryNotice?.cards?.length ? (
                  <>
                    <div className="noticePanelHead">
                      <div className="noticePanelHeadMain">
                        <div className="noticeKicker">
                          {summaryNotice.pushDate} {summaryNotice.pushTime}
                        </div>
                        <div className="noticeTitle">{summaryNotice.title}</div>
                      </div>
                      <div className="noticePanelMeta">
                        <button
                          className="noticePanelAction"
                          type="button"
                          onClick={() => {
                            void handleMarkAllRead();
                          }}
                          disabled={!summaryNotice.hasUnread}
                        >
                          全部已读
                        </button>
                        {deleteMode ? (
                          <>
                            <button
                              className="noticePanelAction"
                              type="button"
                              onClick={() => {
                                setDeleteMode(false);
                                setSelectedNoticeCardIds(new Set());
                              }}
                            >
                              取消
                            </button>
                            <button
                              className="noticePanelAction danger"
                              type="button"
                              onClick={() => {
                                void handleClearSelectedNotices();
                              }}
                              disabled={selectedNoticeCardIds.size === 0}
                            >
                              删除所选
                            </button>
                          </>
                        ) : (
                          <button
                            className="noticePanelAction danger"
                            type="button"
                            onClick={() => {
                              setDeleteMode(true);
                              setSelectedNoticeCardIds(new Set());
                            }}
                          >
                            批量删除
                          </button>
                        )}
                      </div>
                    </div>

                    <div className="noticeCards">
                      {summaryNotice.cards.map((card) => {
                        const expanded = expandedNoticeCards.has(card.id);
                        return (
                          <div
                            className={`noticeCard ${card.type}${expanded ? ' expanded' : ''}`}
                            key={card.id}
                          >
                            {!card.read && <span className="noticeCardDot" />}
                            <div
                              className="noticeCardHead"
                              onClick={() => {
                                if (deleteMode) {
                                  toggleDeleteSelection(card.id);
                                  return;
                                }
                                void toggleNoticeCard(card);
                              }}
                              role="button"
                              tabIndex={0}
                              onKeyDown={(e) => {
                                if (e.key === 'Enter' || e.key === ' ') {
                                  e.preventDefault();
                                  if (deleteMode) {
                                    toggleDeleteSelection(card.id);
                                    return;
                                  }
                                  void toggleNoticeCard(card);
                                }
                              }}
                            >
                              <span>
                                <strong>{card.title}</strong>
                                <em>{card.subtitle}</em>
                              </span>
                              <div className="noticeCardActions">
                                {deleteMode ? (
                                  <button
                                    className={`noticeSelectBtn${selectedNoticeCardIds.has(card.id) ? ' selected' : ''}`}
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      toggleDeleteSelection(card.id);
                                    }}
                                    aria-label="选择通知"
                                    title="选择通知"
                                    type="button"
                                  >
                                    {selectedNoticeCardIds.has(card.id) ? '已选' : '勾选'}
                                  </button>
                                ) : (
                                  <>
                                    <button
                                      className="noticeDeleteBtn"
                                      onClick={(e) => {
                                        e.stopPropagation();
                                        void handleClearNotice(card.id);
                                      }}
                                      aria-label="删除通知"
                                      title="删除通知"
                                      type="button"
                                    >
                                      <DeleteOutlined />
                                    </button>
                                    <DownOutlined />
                                  </>
                                )}
                              </div>
                            </div>
                            {expanded && (
                              <div className="noticeCardSummary expanded">
                                {card.content || card.preview || ''}
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </>
                ) : (
                  <div className="noticeEmpty">
                    <div className="noticeTitle">暂无通知</div>
                    <div className="noticeHint">07:50 早间提醒 · 19:50 晚间提醒</div>
                  </div>
                )}
              </div>

              <button
                ref={avatarBtnRef}
                className={`avatarBtn${menuOpen ? ' open' : ''}`}
                onClick={() => {
                  setMenuOpen((prev) => !prev);
                  setNoticeOpen(false);
                }}
              >
                {currentUser.avatarUrl ? (
                  <div className="avatarCircle">
                    <img src={currentUser.avatarUrl} alt={currentUser.username} />
                  </div>
                ) : (
                  <div className="avatarCircle">{firstChar}</div>
                )}
                <span className="avatarName">{currentUser.username}</span>
                <svg className="avatarChevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </button>

              <div
                ref={dropdownRef}
                className="dropdown"
                style={avatarBtnRef.current ? { width: avatarBtnRef.current.offsetWidth } : undefined}
                onClick={(e) => e.stopPropagation()}
              >
                <div className="ddItem" onClick={() => handleMenuAction('profile')}>
                  <div className="ddIcon blue">
                    <UserOutlined />
                  </div>
                  个人中心
                </div>
                <div className="ddItem" onClick={() => handleMenuAction('favorites')}>
                  <div className="ddIcon amber">
                    <StarOutlined />
                  </div>
                  我的收藏
                </div>
                <div className="ddItem" onClick={() => handleMenuAction('password')}>
                  <div className="ddIcon violet">
                    <LockOutlined />
                  </div>
                  修改密码
                </div>
                <div className="ddSep" />
                <div className="ddItem danger" onClick={() => handleMenuAction('logout')}>
                  <div className="ddIcon">
                    <LogoutOutlined />
                  </div>
                  退出登录
                </div>
              </div>
            </>
          ) : (
            <button className="avatarBtn" onClick={() => history.push('/user/Login')}>
              <div className="avatarCircle" style={{ background: '#ccc' }}>
                ?
              </div>
              <span className="avatarName">登录</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default CustomNavbar;
