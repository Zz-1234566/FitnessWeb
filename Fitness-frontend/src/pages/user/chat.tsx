import { Input, Button, Avatar, theme, message as antMessage, Tooltip, Dropdown } from 'antd';
import { SendOutlined, UserOutlined, ClearOutlined, RobotOutlined, DownOutlined } from '@ant-design/icons';
import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useModel } from '@umijs/max';
import {
  sendChatMessageStream,
  savePlan,
  getModelList,
  switchModel,
  addStructuredExerciseRecord,
  addDietRecord,
  quickSaveRecord,
} from '@/services/ant-design-pro/api';
import type { ChatStreamMeta } from '@/services/ant-design-pro/api';
import { FITNESS_QUOTES, SYSTEM_LOGO } from '@/constants';
import './chat.less';

type SavePlanType = 'training' | 'diet';

interface Message {
  id: string;
  content: string;
  sender: 'user' | 'ai';
  timestamp: Date;
  isWelcome?: boolean;
  isComplete?: boolean;
  planSaved?: boolean; // 计划已保存标记，持久化到 localStorage
  replyType?: string;
  savePlanType?: SavePlanType;
  showRecordButton?: boolean;
  recordType?: 'training' | 'diet' | '';
  recordSaved?: boolean;
  recordSaving?: boolean;
}

type ModelOption = { name: string; label: string };
type TrainingRecordItem = {
  name: string;
  muscleGroup?: string;
  sets?: string;
  duration?: string;
};
type TrainingRecordSession = {
  title: string;
  tags: string[];
  items: TrainingRecordItem[];
};
type DietRecordItem = {
  mealType: string;
  calories?: string;
  time?: string;
  foods: string[];
};

const CHAT_STORAGE_KEY = 'chat_history';
const CHAT_STORAGE_EVENT = 'chat-storage-updated';
const MAX_MESSAGES = 50;

const FALLBACK_MODEL_OPTIONS: ModelOption[] = [
  { name: 'qwen-plus', label: '通义千问 Plus' },
  { name: 'deepseek-v4-flash', label: 'DeepSeek V4 Flash' },
  { name: 'glm-4-flash', label: '智谱 GLM-4 Flash' },
];

function readStoredMessages(): Message[] {
  try {
    const saved = localStorage.getItem(CHAT_STORAGE_KEY);
    if (!saved) return [];
    const parsed = JSON.parse(saved);
    if (!Array.isArray(parsed)) return [];
    return parsed.map((m: any) => ({ ...m, timestamp: new Date(m.timestamp) }));
  } catch {
    return [];
  }
}

function writeStoredMessages(messages: Message[]) {
  try {
    localStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(messages.slice(-MAX_MESSAGES)));
  } catch {}
}

function emitStoredMessagesUpdated() {
  window.dispatchEvent(new CustomEvent(CHAT_STORAGE_EVENT));
}

function appendStoredMessages(extra: Message[]) {
  writeStoredMessages([...readStoredMessages(), ...extra]);
}

function patchStoredMessage(messageId: string, patch: (msg: Message) => Message) {
  const next = readStoredMessages().map((msg) => (msg.id === messageId ? patch(msg) : msg));
  writeStoredMessages(next);
}

// ====== 氛围背景 ======
const AMB_MODES = [
  { icon: '🌅', text: '清晨 · 适合晨间唤醒训练', colors: ['#fef3e2', '#fdf0d5', '#fdf8ee', '#fff9f0'] },
  { icon: '☀️', text: '午后 · 训练黄金时段', colors: ['#f0f4f3', '#e8ede9', '#f6f7f5', '#fdf8ee'] },
  { icon: '🌙', text: '傍晚 · 放松恢复时刻', colors: ['#e8edf3', '#dde4ef', '#eef0f5', '#f5f3f0'] },
  { icon: '🌌', text: '深夜 · 注意休息', colors: ['#e2e5eb', '#d8dce6', '#eaecf0', '#f0ede8'] },
];

function getAmbIndex() {
  const h = new Date().getHours();
  if (h >= 5 && h < 10) return 0;
  if (h >= 10 && h < 18) return 1;
  if (h >= 18 && h < 22) return 2;
  return 3;
}

function isTodayExerciseRecordMessage(msg: Message) {
  return msg.sender === 'ai' && (msg.replyType === 'today_exercise_record' || msg.content.startsWith('【今日训练记录】'));
}

function isTodayDietRecordMessage(msg: Message) {
  return msg.sender === 'ai' && (msg.replyType === 'today_diet_record' || msg.content.startsWith('【今日饮食记录】'));
}

function isTodayTrainingPlanMessage(msg: Message) {
  return msg.sender === 'ai' && msg.replyType === 'today_training_plan';
}

function isTodayDietPlanMessage(msg: Message) {
  return msg.sender === 'ai' && msg.replyType === 'today_diet_plan';
}

function parseTodayExerciseRecord(content: string): TrainingRecordSession[] {
  const lines = content
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean);
  const sessions: TrainingRecordSession[] = [];
  let currentSession: TrainingRecordSession | null = null;

  for (const line of lines) {
    if (line === '【今日训练记录】') {
      continue;
    }
    if (line.startsWith('训练：')) {
      const [title, ...tags] = line.replace(/^训练：/, '').split('｜').map((part) => part.trim()).filter(Boolean);
      currentSession = {
        title: title || '未命名训练',
        tags,
        items: [],
      };
      sessions.push(currentSession);
      continue;
    }
    if (line.startsWith('动作：')) {
      const [name, muscleGroup, sets, duration] = line
        .replace(/^动作：/, '')
        .split('｜')
        .map((part) => part.trim());
      if (!currentSession) {
        currentSession = { title: '训练记录', tags: [], items: [] };
        sessions.push(currentSession);
      }
      currentSession.items.push({
        name: name || '未命名动作',
        muscleGroup: muscleGroup || undefined,
        sets: sets || undefined,
        duration: duration || undefined,
      });
      continue;
    }
    if (line.startsWith('- ')) {
      const [namePart, ...detailParts] = line.replace(/^- /, '').split('，').map((part) => part.trim()).filter(Boolean);
      const muscleMatch = namePart.match(/^(.*?)(?:（(.*)）)?$/);
      if (!currentSession) {
        currentSession = { title: '训练记录', tags: [], items: [] };
        sessions.push(currentSession);
      }
      currentSession.items.push({
        name: muscleMatch?.[1]?.trim() || namePart,
        muscleGroup: muscleMatch?.[2]?.trim() || undefined,
        sets: detailParts.find((part) => part.includes('组')),
        duration: detailParts.find((part) => part.includes('分钟')),
      });
      continue;
    }
    currentSession = {
      title: line,
      tags: [],
      items: [],
    };
    sessions.push(currentSession);
  }

  return sessions.filter((session) => session.title || session.items.length > 0);
}

function parseTodayDietRecord(content: string): DietRecordItem[] {
  const lines = content
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean);
  const records: DietRecordItem[] = [];
  let current: DietRecordItem | null = null;

  for (const line of lines) {
    if (line === '【今日饮食记录】') {
      continue;
    }
    if (line.startsWith('餐次：')) {
      const [mealType, calories, time] = line
        .replace(/^餐次：/, '')
        .split('｜')
        .map((part) => part.trim());
      current = {
        mealType: mealType || '未分类',
        calories: calories && calories !== '-' ? calories : undefined,
        time: time || undefined,
        foods: [],
      };
      records.push(current);
      continue;
    }
    if (line.startsWith('食物：')) {
      const foods = line
        .replace(/^食物：/, '')
        .split(/、|,|，/)
        .map((part) => part.trim())
        .filter(Boolean);
      if (!current) {
        current = {
          mealType: '未分类',
          foods: [],
        };
        records.push(current);
      }
      current.foods.push(...foods);
    }
  }

  return records;
}

// ====== 庆祝粒子 ======
function spawnConfetti(x: number, y: number) {
  const container = document.getElementById('confetti-container');
  if (!container) return;
  const colors = ['#164A41', '#E8A838', '#10b981', '#6366f1', '#ff6b81', '#3b8eea', '#f5a623', '#a87bff'];
  for (let i = 0; i < 20; i++) {
    const el = document.createElement('div');
    el.className = 'confetti-particle';
    const angle = (Math.PI * 2 * i) / 20 + (Math.random() - 0.5) * 0.5;
    const dist = 60 + Math.random() * 100;
    const dx = Math.cos(angle) * dist;
    const dy = Math.sin(angle) * dist - 100;
    const size = 5 + Math.random() * 5;
    el.style.cssText = `left:${x}px;top:${y}px;width:${size}px;height:${size}px;background:${colors[i % colors.length]};border-radius:${Math.random() > 0.5 ? '50%' : '2px'};`;
    container.appendChild(el);
    el.animate([
      { transform: 'translate(0,0) rotate(0deg) scale(0)', opacity: 1 },
      { transform: `translate(${dx * 0.3}px,${dy * 0.3}px) rotate(180deg) scale(1.1)`, opacity: 1, offset: 0.3 },
      { transform: `translate(${dx}px,${dy + 300}px) rotate(720deg) scale(0.2)`, opacity: 0 },
    ], { duration: 1100 + Math.random() * 500, easing: 'cubic-bezier(0.2,0.8,0.4,1)', fill: 'forwards' });
    setTimeout(() => el.remove(), 2000);
  }
}

// ====== 主组件 ======
const ChatPage: React.FC = () => {
  const { useToken } = theme;
  const { token } = useToken();
  const { initialState } = useModel('@@initialState');
  const userAvatar = initialState?.currentUser?.avatarUrl;
  const [chatQuote] = useState(() => FITNESS_QUOTES[Math.floor(Math.random() * FITNESS_QUOTES.length)]);

  const [messages, setMessages] = useState<Message[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [modelOptions, setModelOptions] = useState<ModelOption[]>([]);
  const [currentModel, setCurrentModel] = useState<string>('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesBoxRef = useRef<HTMLDivElement>(null);
  const initializedScrollRef = useRef(false);
  const activeStreamMessageIdRef = useRef<string | null>(null);
  const mountedRef = useRef(false);

  // 氛围背景
  useEffect(() => {
    const amb = AMB_MODES[getAmbIndex()];
    const page = document.querySelector('.chat-page') as HTMLElement;
    if (page) {
      page.style.setProperty('--amb-1', amb.colors[0]);
      page.style.setProperty('--amb-2', amb.colors[1]);
      page.style.setProperty('--amb-3', amb.colors[2]);
      page.style.setProperty('--amb-4', amb.colors[3]);
    }
  }, []);

  const scrollToBottom = (smooth = true) => {
    const el = messagesBoxRef.current;
    if (!el) return;
    el.scrollTo({ top: el.scrollHeight, behavior: smooth ? 'smooth' : 'instant' });
  };

  const patchAiMessage = useCallback((messageId: string, patch: (msg: Message) => Message) => {
    patchStoredMessage(messageId, patch);
    if (mountedRef.current) {
      setMessages((prev) => prev.map((msg) => (msg.id === messageId ? patch(msg) : msg)));
    } else {
      emitStoredMessagesUpdated();
    }
  }, []);

  const finalizeStreamMessage = useCallback((fallbackText?: string) => {
    const messageId = activeStreamMessageIdRef.current;
    if (!messageId) return;

    activeStreamMessageIdRef.current = null;

    patchAiMessage(messageId, (msg) => ({
      ...msg,
      isComplete: true,
      content: msg.content.trim() || fallbackText || '不好意思，AI 调用失败，请联系工作人员或稍等片刻再试。',
    }));
    if (mountedRef.current) {
      setIsTyping(false);
    }
  }, [patchAiMessage]);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    const storedMessages = readStoredMessages();
    if (storedMessages.length > 0) {
      setMessages(storedMessages);
      setIsTyping(storedMessages.some((msg) => msg.sender === 'ai' && !msg.isComplete));
      return;
    }
    setMessages([{
      id: 'welcome',
      content: `你好！我是你的智能健身助手 Tatan 😊

我能帮你：
• 制定训练/饮食计划（生成后记得点保存按钮）
• 回答健身问题、陪你聊聊
• 查看历史记录和AI健康复盘

左上角可切换AI模型，直接打字跟我聊就行~`,
      sender: 'ai',
      timestamp: new Date(),
      isWelcome: true,
      isComplete: true,
    }]);
  }, []);

  useEffect(() => {
    const syncMessages = () => {
      const storedMessages = readStoredMessages();
      if (storedMessages.length === 0) return;
      setMessages(storedMessages);
      setIsTyping(storedMessages.some((msg) => msg.sender === 'ai' && !msg.isComplete));
    };
    window.addEventListener(CHAT_STORAGE_EVENT, syncMessages as EventListener);
    return () => {
      window.removeEventListener(CHAT_STORAGE_EVENT, syncMessages as EventListener);
    };
  }, []);

  useEffect(() => {
    writeStoredMessages(messages);
    if (messages.length === 0) return;
    if (!initializedScrollRef.current) {
      initializedScrollRef.current = true;
      requestAnimationFrame(() => scrollToBottom(false));
      return;
    }
    scrollToBottom(true);
  }, [messages]);

  useEffect(() => {
    getModelList()
      .then((res: any) => {
        const models = res?.models || res?.data?.models || [];
        const current = res?.current || res?.data?.current || '';
        if (models.length > 0) {
          setModelOptions(models);
          setCurrentModel(current || models[0].name);
        } else {
          setModelOptions(FALLBACK_MODEL_OPTIONS);
          setCurrentModel('qwen-plus');
        }
      })
      .catch(() => {
        setModelOptions(FALLBACK_MODEL_OPTIONS);
        setCurrentModel('qwen-plus');
      });
  }, []);

  const handleModelChange = async (name: string) => {
    try {
      await switchModel(name);
      setCurrentModel(name);
      const label = modelOptions.find(m => m.name === name)?.label || name;
      antMessage.success(`已切换到 ${label}`);
    } catch {
      antMessage.error('切换模型失败');
    }
  };

  const handleSend = useCallback(async () => {
    const text = inputValue.trim();
    if (!text) return;

    const userMessage: Message = { id: `user-${Date.now()}`, content: text, sender: 'user', timestamp: new Date() };
    const aiMessageId = `ai-${Date.now()}`;
    const aiMessage: Message = { id: aiMessageId, content: '', sender: 'ai', timestamp: new Date() };

    appendStoredMessages([userMessage, aiMessage]);
    setMessages(prev => [...prev, userMessage, aiMessage]);
    setInputValue('');
    setIsTyping(true);
    activeStreamMessageIdRef.current = aiMessageId;

    try {
      await sendChatMessageStream(
        { message: text },
        (chunk) => {
          patchAiMessage(aiMessageId, (msg) => ({ ...msg, content: msg.content + chunk }));
        },
        () => {
          finalizeStreamMessage();
        },
        () => {
          finalizeStreamMessage('不好意思，AI 调用失败，请联系工作人员或稍等片刻再试。');
        },
        (meta: ChatStreamMeta) => {
          patchAiMessage(aiMessageId, (msg) => ({
            ...msg,
            replyType: meta.replyType,
            savePlanType: meta.showSaveButton && meta.savePlanType ? meta.savePlanType : undefined,
            showRecordButton: meta.showRecordButton || false,
            recordType: meta.recordType || '',
          }));
        },
      );
    } catch {
      finalizeStreamMessage('不好意思，AI 调用失败，请联系工作人员或稍等片刻再试。');
    }
  }, [finalizeStreamMessage, inputValue, patchAiMessage]);

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(); }
  };

  const isTraining = (msg: Message) => msg.sender === 'ai' && msg.savePlanType === 'training';
  const isDiet = (msg: Message) => msg.sender === 'ai' && msg.savePlanType === 'diet';

  const formatTime = (ts: Date) => {
    const d = new Date(ts);
    const h = d.getHours().toString().padStart(2, '0');
    const m = d.getMinutes().toString().padStart(2, '0');
    return `${h}:${m}`;
  };

  const handleSave = (msg: Message, e: React.MouseEvent) => {
    const type = msg.savePlanType;
    if (!type) return;
    savePlan({ type })
      .then(() => {
        // 直接标记消息对象，跟随 localStorage 持久化
        setMessages(prev => prev.map(m => m.id === msg.id ? { ...m, planSaved: true } : m));
        antMessage.success(isTraining(msg) ? '训练计划已保存！' : '饮食计划已保存！');
        const rect = (e.target as HTMLElement).getBoundingClientRect();
        spawnConfetti(rect.left + rect.width / 2, rect.top);
        document.querySelectorAll('.chat-avatar-status').forEach(el => {
          el.classList.remove('idle');
          el.classList.add('celebrate');
          setTimeout(() => { el.classList.remove('celebrate'); el.classList.add('idle'); }, 800);
        });
      })
      .catch((error: any) => {
        const errorMessage =
          error?.info?.message || error?.data?.message || error?.message || '保存计划失败，请稍后重试';
        antMessage.warning(errorMessage);
      });
  };

  const handleClear = () => {
    activeStreamMessageIdRef.current = null;
    setIsTyping(false);
    setMessages([]);
    writeStoredMessages([]);
    setTimeout(() => {
      setMessages([{
        id: 'welcome',
        content: `对话已清空！随时开始新的对话 😊`,
        sender: 'ai',
        timestamp: new Date(),
        isWelcome: true,
        isComplete: true,
      }]);
    }, 300);
  };

  const renderMessageBody = (msg: Message) => {
    if (!isTodayExerciseRecordMessage(msg) && !isTodayDietRecordMessage(msg)) {
      return <span className="chat-bubble-text">{msg.content || ''}</span>;
    }

    if (isTodayExerciseRecordMessage(msg)) {
      const sessions = parseTodayExerciseRecord(msg.content || '');
      if (sessions.length === 0) {
        return <span className="chat-bubble-text">{msg.content || ''}</span>;
      }

      return (
        <div className="chat-training-record">
          <div className="chat-training-record-head">
            <span className="chat-training-record-title">今日训练记录</span>
            <span className="chat-training-record-subtitle">{sessions.length} 次训练</span>
          </div>
          {sessions.map((session, index) => (
            <div key={`${msg.id}-session-${index}`} className="chat-training-session">
              <div className="chat-training-session-top">
                <div className="chat-training-session-title">{session.title}</div>
                {session.tags.length > 0 && (
                  <div className="chat-training-session-tags">
                    {session.tags.map((tag) => (
                      <span key={`${msg.id}-session-${index}-${tag}`} className="chat-training-tag">
                        {tag}
                      </span>
                    ))}
                  </div>
                )}
              </div>
              {session.items.length > 0 && (
                <>
                  <div className="chat-training-table-head">
                    <span>动作</span>
                    <span>部位</span>
                    <span>组数</span>
                    <span>时长</span>
                  </div>
                  <div className="chat-training-mobile-list">
                    {session.items.map((item, itemIndex) => (
                      <div key={`${msg.id}-item-${index}-${itemIndex}`} className="chat-training-row">
                        <div className="chat-training-col chat-training-col-name">
                          <span className="chat-training-mobile-label">动作</span>
                          <span>{item.name}</span>
                        </div>
                        <div className="chat-training-col">
                          <span className="chat-training-mobile-label">部位</span>
                          <span>{item.muscleGroup || '-'}</span>
                        </div>
                        <div className="chat-training-col">
                          <span className="chat-training-mobile-label">组数</span>
                          <span>{item.sets || '-'}</span>
                        </div>
                        <div className="chat-training-col">
                          <span className="chat-training-mobile-label">时长</span>
                          <span>{item.duration || '-'}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </>
              )}
            </div>
          ))}
        </div>
      );
    }

    const records = parseTodayDietRecord(msg.content || '');
    if (records.length === 0) {
      return <span className="chat-bubble-text">{msg.content || ''}</span>;
    }

    return (
      <div className="chat-diet-record">
        <div className="chat-diet-record-head">
          <span className="chat-training-record-title">今日饮食记录</span>
          <span className="chat-training-record-subtitle">{records.length} 条记录</span>
        </div>
        {records.map((record, index) => (
          <div key={`${msg.id}-diet-${index}`} className="chat-diet-session">
            <div className="chat-diet-session-top">
              <div className="chat-diet-session-title">{record.mealType}</div>
              <div className="chat-training-session-tags">
                {record.calories && <span className="chat-training-tag">{record.calories}</span>}
                {record.time && <span className="chat-training-tag">{record.time}</span>}
              </div>
            </div>
            <div className="chat-diet-food-list">
              {record.foods.map((food, foodIndex) => (
                <span key={`${msg.id}-diet-${index}-${foodIndex}`} className="chat-diet-food-chip">
                  {food}
                </span>
              ))}
            </div>
          </div>
        ))}
      </div>
    );
  };

  return (
    <div className="chat-page">
      <div className="confetti-container" id="confetti-container" />
      <div className="chat-window">
        {/* 氛围提示 */}
        <div className="chat-amb-bar">{AMB_MODES[getAmbIndex()].icon} {AMB_MODES[getAmbIndex()].text}</div>

        {/* 顶部栏 */}
        <div className="chat-header">
          <div className="chat-header-left">
            <div className="chat-header-avatar-ring">
              <Avatar size={38} src={SYSTEM_LOGO} className="chat-header-avatar" />
            </div>
            <div>
              <div className="chat-header-name">Tatan</div>
              <div className="chat-header-desc">AI 智能教练 · 在线</div>
            </div>
          </div>
          <div className="chat-header-right">
            <Tooltip title="清空对话">
              <Button type="text" icon={<ClearOutlined />} className="chat-clear-btn" onClick={handleClear} />
            </Tooltip>
            <Dropdown
              menu={{
                items: modelOptions.map(m => ({ key: m.name, label: m.label })),
                selectedKeys: [currentModel],
                onClick: ({ key }) => handleModelChange(key),
              }}
              trigger={['click']}
            >
              <div className="chat-model-select">
                <RobotOutlined className="chat-model-icon" />
                <span className="chat-model-label">{modelOptions.find(m => m.name === currentModel)?.label || currentModel}</span>
                <DownOutlined style={{ fontSize: 10, color: 'var(--ant-color-text-quaternary)' }} />
              </div>
            </Dropdown>
          </div>
        </div>

        {/* 消息区 */}
        <div className="chat-messages" ref={messagesBoxRef}>
          {messages.map((msg, idx) => {
            const isUser = msg.sender === 'user';
            const showTime = idx === 0 || (new Date(msg.timestamp).getTime() - new Date(messages[idx - 1].timestamp).getTime() > 300000);
            const isStreamingMessage =
              isTyping && !isUser && msg.sender === 'ai' && msg === messages[messages.length - 1] && !msg.isComplete;
            return (
              <React.Fragment key={msg.id}>
                {showTime && (
                  <div className="chat-time-divider">
                    <span>{formatTime(msg.timestamp)}</span>
                  </div>
                )}
                <div className={`chat-msg ${isUser ? 'chat-msg-user' : ''} ${!isUser && msg.isWelcome ? 'chat-msg-welcome' : ''}`}>
                  {!isUser && (
                    <div className="chat-msg-avatar-wrap">
                      <Avatar size={40} src={SYSTEM_LOGO} />
                      <div className={`chat-avatar-status ${isStreamingMessage ? 'typing' : 'idle'}`} />
                    </div>
                  )}
                  <div className={`chat-bubble ${isUser ? 'chat-bubble-user' : 'chat-bubble-ai'}`}>
                    {renderMessageBody(msg)}
                    {isStreamingMessage && (
                      <span className="chat-typing">
                        <span className="chat-typing-dot" />
                        <span className="chat-typing-dot" style={{ animationDelay: '0.2s' }} />
                        <span className="chat-typing-dot" style={{ animationDelay: '0.4s' }} />
                      </span>
                    )}
                    {(isTraining(msg) || isDiet(msg)) && !msg.planSaved && (
                      <div className="chat-save-row">
                        <Button size="small" className="chat-save-btn" onClick={(e) => handleSave(msg, e)}>
                          {isTraining(msg) ? '保存训练计划' : '保存饮食计划'}
                        </Button>
                      </div>
                    )}
                    {msg.planSaved && (
                      <div className="chat-save-row">
                        <span style={{ fontSize: 12, color: token.colorTextQuaternary }}>已保存 ✓</span>
                      </div>
                    )}
                    {msg.showRecordButton && msg.recordType && !msg.recordSaved && (
                      <div className="chat-save-row">
                        <Button size="small" type="primary" className="chat-save-btn" loading={msg.recordSaving} onClick={() => {
                          setMessages(prev => prev.map(m => m.id === msg.id ? { ...m, recordSaving: true } : m));
                          quickSaveRecord({ type: msg.recordType as 'training' | 'diet' })
                            .then((res) => {
                              setMessages(prev => prev.map(m => m.id === msg.id ? { ...m, recordSaved: true, recordSaving: false } : m));
                              antMessage.success(msg.recordType === 'training' ? '已记录训练！' : '已记录饮食！');
                              document.querySelectorAll('.chat-avatar-status').forEach(el => {
                                el.classList.remove('idle');
                                el.classList.add('celebrate');
                                setTimeout(() => { el.classList.remove('celebrate'); el.classList.add('idle'); }, 800);
                              });
                            })
                            .catch((error: any) => {
                              setMessages(prev => prev.map(m => m.id === msg.id ? { ...m, recordSaving: false } : m));
                              antMessage.warning(error?.info?.message || '记录失败，请稍后重试');
                            });
                        }}>
                          {msg.recordType === 'training' ? '一键记录训练' : '一键记录饮食'}
                        </Button>
                      </div>
                    )}
                    {msg.recordSaved && (
                      <div className="chat-save-row">
                        <span style={{ fontSize: 12, color: token.colorTextQuaternary }}>已记录 ✓</span>
                      </div>
                    )}
                  </div>
                  {isUser && (
                    <div className="chat-msg-avatar-wrap">
                      <Avatar size={40} src={userAvatar} icon={<UserOutlined />} />
                    </div>
                  )}
                </div>
              </React.Fragment>
            );
          })}
          <div ref={messagesEndRef} />
        </div>

        {/* 输入区 */}
        <div className="chat-input-area">
          <div className="chat-input-hint">按 Enter 发送，Shift+Enter 换行</div>
          <div className="chat-input-row">
            <Input.TextArea
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyDown={handleKeyPress}
              placeholder="问问 Tatan 今天的训练安排是什么..."
              autoSize={{ minRows: 1, maxRows: 3 }}
              style={{ flex: 1, resize: 'none' }}
              className="chat-input-field"
            />
            <Button
              type="primary"
              icon={<SendOutlined />}
              onClick={handleSend}
              disabled={!inputValue.trim()}
              className="chat-send-btn"
            />
          </div>
          <div className="chat-quote">
            <span className="chat-quote-text">"{chatQuote}"</span>
            <span className="chat-quote-dash" />
          </div>
        </div>
      </div>
    </div>
  );
};

export default ChatPage;
