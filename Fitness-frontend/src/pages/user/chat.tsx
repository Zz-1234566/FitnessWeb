import { Input, Button, Avatar, theme, message as antMessage, Tooltip, Spin, Switch, InputNumber, Select, Form } from 'antd';
import { SendOutlined, UserOutlined, ClearOutlined, RobotOutlined, SettingOutlined, PlusOutlined, DeleteOutlined, CloseOutlined, AudioOutlined, CameraOutlined, EditOutlined, SaveOutlined, ThunderboltOutlined, FireOutlined, FormOutlined, UnorderedListOutlined, BarChartOutlined, ToolOutlined } from '@ant-design/icons';
import React, { useState, useRef, useEffect, useCallback, useLayoutEffect } from 'react';
import { useModel, history } from '@umijs/max';
import { MUSCLE_GROUP_LABELS } from '@/constants/exercise';
import BilibiliIcon from '@/assets/icons/bilibili';
import DouyinIcon from '@/assets/icons/douyin';
import SaveIcon from '@/assets/icons/save';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import {
  sendChatMessageStream,
  savePlan,
  getModelList,
  switchModel,
  getModelConfig,
  saveAllModelConfig,
  addCustomModel,
  deleteCustomModel,
  getCustomModelDetail,
  updateCustomModel,
  addStructuredExerciseRecord,
  addDietRecord,
  quickSaveRecord,
  recognizeFoodImage,
  recognizeSummaryStream,
  getRecognizeConfig,
  saveRecognizedFood,
  transcribeAudio,
} from '@/services/ant-design-pro/api';
import useBottomSheetGesture from '@/hooks/useBottomSheetGesture';
import type { ChatStreamMeta } from '@/services/ant-design-pro/api';
import { getChatHistory } from '@/services/ant-design-pro/api';
import { FITNESS_QUOTES, SYSTEM_LOGO } from '@/constants';
import './chat.less';

/* eslint-disable @typescript-eslint/no-explicit-any */

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
  // 图片识别相关
  imageInfo?: {
    imageUrl: string;
    recognitionType: 'food' | 'nutrition_label';
    foodName: string;
    confidence?: number;
    unit: string;
    suggestedAmount: number;
    currentAmount?: number;
    nutritionPerUnit: { calories: number; protein: number; carbs: number; fat: number; fiber: number };
    perUnitAmount: number;
    dataSource?: string;
    amountEstimated?: boolean;
    debugTimings?: Record<string, any>;
  };
  recognitionLoading?: boolean;
  recognitionStep?: number;
  recognitionError?: string;
  userImageUrl?: string;
  statusText?: string;
  // 器械视频教程链接
  bilibiliUrl?: string;
  douyinUrl?: string;
  // 器械识别匹配的动作
  matchedExercises?: { name: string; muscleGroup: string }[];
  // 识别到的器械名称（用于跳转动作库筛选）
  equipmentName?: string;
}

type ModelOption = { name: string; label: string; type: string; isCustom?: string };
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
  { name: 'qwen-plus', label: '通义千问 Plus', type: 'text' },
  { name: 'deepseek-v4-flash', label: 'DeepSeek V4 Flash', type: 'text' },
  { name: 'glm-4-flash', label: '智谱 GLM-4 Flash', type: 'text' },
];

const DEFAULT_MODEL_ROLES = {
  purificationModel: 'deepseek-v4-flash',
  chatModel: 'qwen3.6-plus',
  whisperModel: 'qwen3-asr-flash',
  visionModel: 'glm-4v-flash',
};

const logVisionTrace = (phase: string, payload: Record<string, any>) => {
  console.info(`[VisionTrace][${phase}]`, payload);
};

const createMessageId = (prefix: 'user' | 'ai') => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return `${prefix}-${crypto.randomUUID()}`;
  }
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
};

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
    const slim = messages.slice(-MAX_MESSAGES).map(m => {
      if (m.userImageUrl?.startsWith('data:')) return { ...m, userImageUrl: undefined };
      return m;
    });
    localStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(slim));
  } catch {}
}

function emitStoredMessagesUpdated() {
  window.dispatchEvent(new CustomEvent(CHAT_STORAGE_EVENT));
}

function appendStoredMessages(extra: Message[]) {
  writeStoredMessages([...readStoredMessages(), ...extra]);
}

function storeMessagesAndNotify(extra: Message[]) {
  writeStoredMessages([...readStoredMessages(), ...extra]);
  emitStoredMessagesUpdated();
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

// ====== 食物识别结果卡片（提到组件外部避免每次渲染重建） ======
const FoodRecognitionCard: React.FC<{
  message: Message;
  patchAiMessage: (id: string, patch: (msg: Message) => Message) => void;
  scrollToBottom: () => void;
  getCurrentMeal: () => string;
}> = ({ message, patchAiMessage, scrollToBottom: _scrollToBottom, getCurrentMeal }) => {
  const info = message.imageInfo!;
  const [amountStr, setAmountStr] = useState(String(info.currentAmount || info.suggestedAmount || 100));
  const [mealType, setMealType] = useState(getCurrentMeal());
  const [saving, setSaving] = useState(false);
  const renderLoggedRef = useRef(false);

  useLayoutEffect(() => {
    if (renderLoggedRef.current || !info.debugTimings) return;
    renderLoggedRef.current = true;

    const clientCommitAtMs = Date.now();
    const backendReadyAtMs = Number(info.debugTimings.backendResponseReadyAtMs || 0);
    const clientResponseReceivedAtMs = Number(info.debugTimings.clientResponseReceivedAtMs || 0);
    logVisionTrace('react-commit', {
      traceId: info.debugTimings.traceId,
      backendReadyAtMs,
      clientResponseReceivedAtMs,
      clientCommitAtMs,
      backendToReceiveMs: backendReadyAtMs > 0 && clientResponseReceivedAtMs > 0
        ? clientResponseReceivedAtMs - backendReadyAtMs
        : null,
      receiveToCommitMs: clientResponseReceivedAtMs > 0
        ? clientCommitAtMs - clientResponseReceivedAtMs
        : null,
    });

    requestAnimationFrame(() => {
      const clientPaintAtMs = Date.now();
      logVisionTrace('paint', {
        traceId: info.debugTimings?.traceId,
        clientPaintAtMs,
        commitToPaintMs: clientPaintAtMs - clientCommitAtMs,
        backendToPaintMs: backendReadyAtMs > 0 ? clientPaintAtMs - backendReadyAtMs : null,
      });
    });
  }, [info.debugTimings]);

  if (message.recordSaved) {
    return (
      <div className="chat-food-card">
        <div className="chat-food-card-header">
          <img src={info.imageUrl} className="chat-food-card-img" />
          <div className="chat-food-card-info">
            <div className="chat-food-card-name">{info.foodName}</div>
            <div className="chat-food-card-tag"><span className="chat-food-card-tag-badge">{info.recognitionType === 'food' ? '食物识别' : '营养标签'}</span></div>
          </div>
        </div>
        <div className="chat-food-card-saved">已记录到今日饮食 ✓</div>
      </div>
    );
  }

  const amount = Number(amountStr) || 0;
  const factor = info.perUnitAmount > 0 ? amount / info.perUnitAmount : 0;
  const kcal = Math.round(info.nutritionPerUnit.calories * factor / 4.184);
  const protein = +(info.nutritionPerUnit.protein * factor).toFixed(1);
  const carbs = +(info.nutritionPerUnit.carbs * factor).toFixed(1);
  const fat = +(info.nutritionPerUnit.fat * factor).toFixed(1);
  const fiber = +(info.nutritionPerUnit.fiber * factor).toFixed(1);

  const handleSave = () => {
    if (amount <= 0) {
      antMessage.warning('请输入有效的食用量');
      return;
    }
    setSaving(true);
    saveRecognizedFood({
      imageUrl: info.imageUrl, foodName: info.foodName, unit: info.unit,
      actualAmount: amount, perUnitAmount: info.perUnitAmount,
      calories: info.nutritionPerUnit.calories,
      protein: info.nutritionPerUnit.protein,
      carbs: info.nutritionPerUnit.carbs,
      fat: info.nutritionPerUnit.fat,
      fiber: info.nutritionPerUnit.fiber,
      mealType, source: 'image_recognition',
    }).then(() => {
      patchAiMessage(message.id, (msg: Message) => ({ ...msg, recordSaved: true }));
      antMessage.success('饮食已记录');
    }).catch((err: any) => {
      antMessage.warning(err?.info?.message || '记录失败，请稍后重试');
      setSaving(false);
    });
  };

  return (
    <div className="chat-food-card">
      <div className="chat-food-card-header">
        <img src={info.imageUrl} className="chat-food-card-img" />
        <div className="chat-food-card-info">
          <div className="chat-food-card-name">{info.foodName}</div>
          <div className="chat-food-card-tag">
            <span className="chat-food-card-tag-badge">食物识别</span>
          </div>
        </div>
      </div>
      <div className="chat-food-card-nutrition">
        <div className="chat-food-nutrition-row"><span className="chat-food-nutrition-label">热量</span><span className="chat-food-nutrition-value highlight">{kcal} kcal</span></div>
        <div className="chat-food-nutrition-row"><span className="chat-food-nutrition-label">蛋白质</span><span className="chat-food-nutrition-value">{protein} g</span></div>
        <div className="chat-food-nutrition-row"><span className="chat-food-nutrition-label">碳水化合物</span><span className="chat-food-nutrition-value">{carbs} g</span></div>
        <div className="chat-food-nutrition-row"><span className="chat-food-nutrition-label">脂肪</span><span className="chat-food-nutrition-value">{fat} g</span></div>
        {fiber > 0 && <div className="chat-food-nutrition-row"><span className="chat-food-nutrition-label">膳食纤维</span><span className="chat-food-nutrition-value">{fiber} g</span></div>}
      </div>
      <div className="chat-food-card-tip">数据来源于网络查询，仅供参考，建议仔细甄别后保存</div>
      <div className="chat-food-card-actions">
        <div className="chat-food-amount-row">
          <span>食用量</span>
          <InputNumber<string> min={1} max={5000} value={amountStr} onChange={(v) => setAmountStr(v || '')} onBlur={() => { if (!amountStr || Number(amountStr) < 1) setAmountStr('100'); }} addonAfter={info.unit} size="small" className="chat-food-amount-input" />
          <Select value={mealType} onChange={setMealType} size="small" className="chat-food-meal-select"
            options={[{ value: '早餐', label: '早餐' }, { value: '午餐', label: '午餐' }, { value: '晚餐', label: '晚餐' }, { value: '加餐', label: '加餐' }]} />
        </div>
        {info.amountEstimated && <div className="chat-food-amount-hint">AI 预估分量，仅供参考，可自行修改</div>}
        <Button type="primary" size="small" loading={saving} onClick={handleSave} className="chat-food-save-btn">记录饮食</Button>
      </div>
    </div>
  );
};

// ====== 主组件 ======
const ChatPage: React.FC = () => {
  const { useToken } = theme;
  const { token } = useToken();
  const { initialState } = useModel('@@initialState');
  const userAvatar = initialState?.currentUser?.avatarUrl;
  const [chatQuote] = useState(() => FITNESS_QUOTES[Math.floor(Math.random() * FITNESS_QUOTES.length)]);

  const [messages, setMessages] = useState<Message[]>([]);
  const [inputValue, setInputValue] = useState('');
  const inputValueRef = useRef(inputValue);
  inputValueRef.current = inputValue;

  /** 点击快捷指令按钮：追加前缀到输入框，如果已有内容则换行拼接 */
  const insertPrefix = (prefix: string) => {
    setInputValue(prev => prev ? prev + '\n' + prefix : prefix);
  };
  const [isTyping, setIsTyping] = useState(false);
  const [modelOptions, setModelOptions] = useState<ModelOption[]>([]);
  const [currentModel, setCurrentModel] = useState<string>('');
  const [modelConfigOpen, setModelConfigOpen] = useState(false);
  const [modelRoles, setModelRoles] = useState(DEFAULT_MODEL_ROLES);
  const [modelConfigLoading, setModelConfigLoading] = useState(false);
  const [customModelFormOpen, setCustomModelFormOpen] = useState(false);
  const [customModelLoading, setCustomModelLoading] = useState(false);
  const [editingModel, setEditingModel] = useState<string | null>(null);
  const [modelConfigDirty, setModelConfigDirty] = useState(false);
  const [customForm] = Form.useForm();
  const [isMobile, setIsMobile] = useState(() => window.innerWidth <= 640);
  const modelConfigSheet = useBottomSheetGesture(modelConfigOpen && isMobile, () => {
    if (modelConfigDirty) { antMessage.warning('有未保存的配置更改'); return; }
    setModelConfigOpen(false);
  });
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesBoxRef = useRef<HTMLDivElement>(null);
  const initializedScrollRef = useRef(false);
  const activeStreamMessageIdRef = useRef<string | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);
  const mountedRef = useRef(false);

  // 语音输入
  const [isRecording, setIsRecording] = useState(false);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);

  // 图片识别
  const foodFileInputRef = useRef<HTMLInputElement>(null);
  const [pendingImageFile, setPendingImageFile] = useState<File | null>(null);
  const [pendingImageDataUrl, setPendingImageDataUrl] = useState<string | null>(null);
  const [imagePanelOpen, setImagePanelOpen] = useState(false);
  const [recognitionTimeout, setRecognitionTimeout] = useState(60);
  const [deepThinking, setDeepThinking] = useState(false);

  useEffect(() => {
    getRecognizeConfig().then((res: any) => {
      if (res) {
        setRecognitionTimeout(res.recognitionTimeout || 60);
        setDeepThinking(res.deepThinking || false);
      }
    }).catch(() => {});
  }, []);

  const IMAGE_OPTIONS = [
    { key: 'food', label: '帮我看看这是什么食物，有多少营养' },
    { key: 'equipment', label: '这是什么器械，教我怎么用' },
    { key: 'nutrition_label', label: '帮我读取这个营养成分表' },
    { key: 'form_check', label: '帮我看看这个动作标准吗' },
  ];

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
    if (mountedRef.current) {
      setMessages((prev) => prev.map((msg) => (msg.id === messageId ? patch(msg) : msg)));
    }
    patchStoredMessage(messageId, patch);
  }, []);

  const patchMessages = useCallback((patches: Array<{ id: string; patch: (msg: Message) => Message }>) => {
    if (mountedRef.current) {
      setMessages((prev) => {
        let next = prev;
        for (const { id, patch } of patches) {
          next = next.map((msg) => (msg.id === id ? patch(msg) : msg));
        }
        return next;
      });
    }
    const stored = readStoredMessages();
    let updated = stored;
    for (const { id, patch } of patches) {
      updated = updated.map((msg) => (msg.id === id ? patch(msg) : msg));
    }
    writeStoredMessages(updated);
    emitStoredMessagesUpdated();
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
      // 卸载时先把未完成的流消息标记为完成，避免切页回来三个点一直闪
      const streamId = activeStreamMessageIdRef.current;
      if (streamId) {
        patchStoredMessage(streamId, (msg) => ({
          ...msg,
          isComplete: false,
          content: msg.content.trim(),
        }));
      }
      activeStreamMessageIdRef.current = null;
      // 不 abort，让后端继续生成并保存完整结果
      if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
        mediaRecorderRef.current.stop();
      }
    };
  }, []);

  useEffect(() => {
    const storedMessages = readStoredMessages();
    if (storedMessages.length > 0) {
      // 迁移旧的欢迎语（含 • 符号的旧格式）
      const updated = storedMessages.map((m) => {
        if (m.isWelcome && m.content.includes('•')) {
          return { ...m, content: `你好！我是你的智能健身助手 Tatan 😊\n\n我能帮你：\n\n1. 制定训练/饮食计划（生成后记得点保存按钮）\n2. 回答健身问题、陪你聊聊\n3. 查看历史记录和AI健康复盘\n\n左上角可切换AI模型，直接打字跟我聊就行~` };
        }
        return m;
      });
      if (updated !== storedMessages) writeStoredMessages(updated);
      setMessages(updated);
      const hasIncomplete = storedMessages.some((msg) => msg.sender === 'ai' && !msg.isComplete);
      if (!hasIncomplete) {
        return;
      }
      // 有未完成的消息，从服务端补全
      getChatHistory({ skipErrorHandler: true }).then((res: any) => {
        const pending = res?.pendingMessages;
        if (!Array.isArray(pending) || pending.length === 0) return;
        // pendingMessages 格式: "用户：xxx\n助手：yyy"
        const lastAssistant = [...pending].reverse().find((m: string) => m.startsWith('助手：'));
        if (!lastAssistant) return;
        const fullContent = lastAssistant.replace(/^助手：/, '').trim();
        if (!fullContent) return;
        setMessages((prev) => {
          const lastAi = [...prev].reverse().find((m) => m.sender === 'ai' && !m.isComplete);
          if (!lastAi) return prev;
          return prev.map((m) => m.id === lastAi.id ? { ...m, content: fullContent, isComplete: true } : m);
        });
      }).catch(() => {});
      return;
    }
    setMessages([{
      id: 'welcome',
      content: `你好！我是你的智能健身助手 Tatan 😊

我能帮你：
1. 制定训练/饮食计划（生成后记得点保存按钮）
2. 回答健身问题、陪你聊聊
3. 查看历史记录和AI健康复盘
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
    if (messages.length === 0) return;
    if (!initializedScrollRef.current) {
      initializedScrollRef.current = true;
      requestAnimationFrame(() => scrollToBottom(false));
      return;
    }
    scrollToBottom(true);
  }, [messages]);

  useEffect(() => {
    const t = setTimeout(() => { writeStoredMessages(messages); }, 300);
    return () => clearTimeout(t);
  }, [messages]);

  useEffect(() => {
    getModelList({ skipErrorHandler: true })
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

  useEffect(() => {
    if (!modelConfigOpen) return;
    getModelConfig({ skipErrorHandler: true })
      .then((res: any) => {
        if (res) setModelRoles({ purificationModel: res.purificationModel || '', chatModel: res.chatModel || '', whisperModel: res.whisperModel || '', visionModel: res.visionModel || '' });
      })
      .catch(() => {});
  }, [modelConfigOpen]);

  // ====== 语音输入 (MediaRecorder + Groq Whisper) ======
  const toggleRecording = async () => {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      antMessage.warning('您的浏览器不支持录音功能');
      return;
    }
    if (!window.MediaRecorder) {
      antMessage.warning('您的浏览器不支持 MediaRecorder');
      return;
    }

    if (isRecording) {
      mediaRecorderRef.current?.stop();
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      audioChunksRef.current = [];
      const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
        ? 'audio/webm;codecs=opus' : 'audio/webm';
      const recorder = new MediaRecorder(stream, { mimeType });
      mediaRecorderRef.current = recorder;

      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) audioChunksRef.current.push(e.data);
      };

      recorder.onstop = async () => {
        stream.getTracks().forEach(t => t.stop());
        setIsRecording(false);
        const blob = new Blob(audioChunksRef.current, { type: mimeType });
        if (blob.size === 0) { antMessage.info('录音为空'); return; }
        const hide = antMessage.loading('正在识别语音...', 0);
        try {
          const formData = new FormData();
          formData.append('file', blob, 'recording.webm');
          const text = await transcribeAudio(formData) as unknown as string;
          hide();
          if (text) {
            setInputValue(prev => prev ? prev + text : text);
          } else {
            antMessage.warning('语音识别失败，请重试');
          }
        } catch {
          hide();
          antMessage.error('语音识别请求失败');
        }
      };

      recorder.onerror = () => {
        stream.getTracks().forEach(t => t.stop());
        setIsRecording(false);
        antMessage.error('录音出错，请重试');
      };

      recorder.start();
      setIsRecording(true);
    } catch (err: any) {
      if (err?.name === 'NotAllowedError') {
        antMessage.error('麦克风权限被拒绝，请在浏览器设置中允许');
      } else if (err?.name === 'NotFoundError') {
        antMessage.error('未检测到麦克风设备');
      } else {
        antMessage.error('录音启动失败');
      }
    }
  };

  // ====== 图片压缩（手机拍照像素过高时缩小） ======
  const compressImage = (file: File, opts: { maxWidth: number; quality: number }) =>
    new Promise<{ file: File; dataUrl: string }>((resolve) => {
      const img = new Image();
      img.onload = () => {
        const { maxWidth, quality } = opts;
        let { width, height } = img;
        if (width <= maxWidth) {
          // 不需要压缩，直接转 base64
          const reader = new FileReader();
          reader.onload = () => resolve({ file, dataUrl: reader.result as string });
          reader.readAsDataURL(file);
          return;
        }
        height = Math.round(height * (maxWidth / width));
        width = maxWidth;
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d')!;
        ctx.drawImage(img, 0, 0, width, height);
        const dataUrl = canvas.toDataURL(file.type || 'image/jpeg', quality);
        canvas.toBlob((blob) => {
          const compressed = blob ? new File([blob], file.name, { type: file.type || 'image/jpeg' }) : file;
          resolve({ file: compressed, dataUrl });
        }, file.type || 'image/jpeg', quality);
      };
      img.src = URL.createObjectURL(file);
    });

  // ====== 图片识别 ======
  const handleFoodImageSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) { antMessage.warning('只支持图片格式'); return; }
    if (file.size > 20 * 1024 * 1024) { antMessage.warning('图片大小不能超过 20MB'); return; }

    const compressed = await compressImage(file, { maxWidth: 1280, quality: 0.8 });
    setPendingImageFile(compressed.file);
    setPendingImageDataUrl(compressed.dataUrl);
    setImagePanelOpen(true);
    e.target.value = '';
  };

  const closeImagePanel = () => {
    setImagePanelOpen(false);
    setPendingImageFile(null);
    setPendingImageDataUrl(null);
  };

  const handleImageOptionClick = async (option: typeof IMAGE_OPTIONS[number]) => {
    const file = pendingImageFile;
    const dataUrl = pendingImageDataUrl;
    if (!file || !dataUrl) return;
    closeImagePanel();

    const userMsgId = createMessageId('user');
    const aiMsgId = createMessageId('ai');
    const userMsg: Message = { id: userMsgId, content: option.label, sender: 'user', timestamp: new Date(), userImageUrl: dataUrl };
    const aiMsg: Message = { id: aiMsgId, content: '', sender: 'ai', timestamp: new Date(), recognitionLoading: true, recognitionStep: 0, recognitionType: option.key };
    appendStoredMessages([userMsg, aiMsg]);
    setMessages(prev => [...prev, userMsg, aiMsg]);

    if (option.key === 'food') {
      // 食物：全量同步（需要结构化营养数据）
      const timers: ReturnType<typeof setTimeout>[] = [];
      for (let i = 1; i < 3; i++) timers.push(setTimeout(() => patchAiMessage(aiMsgId, (msg) => ({ ...msg, recognitionStep: i })), i * 1200));
      try {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('type', 'food');
        const res: any = await recognizeFoodImage(formData, { timeout: 60000 });
        timers.forEach(t => clearTimeout(t));
        if (res) {
          const nutrition = res.nutritionPerUnit || res.nutritionPer100g || {};
          patchMessages([
            { id: userMsgId, patch: (msg) => ({ ...msg, userImageUrl: res.imageUrl || dataUrl }) },
            { id: aiMsgId, patch: (msg) => ({
              ...msg, recognitionLoading: false, isComplete: true, recognitionStep: 3,
              content: res.textReply || `识别结果：${res.foodName || '识别完成'}`,
              imageInfo: {
                imageUrl: res.imageUrl, recognitionType: 'food', foodName: res.foodName, confidence: res.confidence,
                unit: res.unit || 'g', suggestedAmount: res.suggestedAmount || res.perUnitAmount || 100,
                currentAmount: res.suggestedAmount || res.perUnitAmount || 100,
                nutritionPerUnit: { calories: nutrition.calories || 0, protein: nutrition.protein || 0, carbs: nutrition.carbs || 0, fat: nutrition.fat || 0, fiber: nutrition.fiber || 0 },
                perUnitAmount: res.perUnitAmount || 100, dataSource: res.dataSource, amountEstimated: res.amountEstimated,
              },
              recognitionType: 'food',
            }) },
          ]);
        } else {
          timers.forEach(t => clearTimeout(t));
          patchAiMessage(aiMsgId, (msg) => ({ ...msg, recognitionLoading: false, isComplete: true, content: '未识别到有效内容' }));
        }
      } catch {
        timers.forEach(t => clearTimeout(t));
        patchAiMessage(aiMsgId, (msg) => ({ ...msg, recognitionLoading: false, isComplete: true, content: '识别失败，请重试' }));
      }
    } else {
      // 非 food：Step1+2 同步，Step3 SSE 流式总结
      const timers: ReturnType<typeof setTimeout>[] = [];
      patchAiMessage(aiMsgId, (msg) => ({ ...msg, recognitionStep: 1 }));
      try {
        // Step 1+2：同步 POST，返回视觉结果和搜索数据
        const formData = new FormData();
        formData.append('file', file);
        formData.append('type', option.key);
        formData.append('deepThinking', String(deepThinking));
        const res: any = await recognizeFoodImage(formData, { timeout: 30000 });
        timers.forEach(t => clearTimeout(t));

        if (res?.error) {
          patchAiMessage(aiMsgId, (msg) => ({ ...msg, recognitionLoading: false, isComplete: true, content: res.error }));
          return;
        }

        // 更新用户图片 URL
        if (res?.imageUrl) {
          patchMessages([
            { id: aiMsgId, patch: (msg) => ({ ...msg }) },
            { id: userMsgId, patch: (msg) => ({ ...msg, userImageUrl: res.imageUrl }) },
          ]);
        }

        // Step 3：SSE 流式推送总结（与训练计划生成相同的 patchAiMessage 方式）
        patchAiMessage(aiMsgId, (msg) => ({ ...msg, recognitionStep: 2, recognitionLoading: false }));
        let isFirstChunk = true;
        await recognizeSummaryStream(
          { equipmentName: res?.equipmentName || '', rawData: res?.rawData || '', type: option.key, deepThinking },
          (chunk) => {
            patchAiMessage(aiMsgId, (msg) => {
              const base = isFirstChunk ? '' : msg.content;
              isFirstChunk = false;
              return { ...msg, content: base + chunk, statusText: undefined };
            });
            scrollToBottom();
          },
          (fullText) => {
            console.log('[SSE-ONDONE] 器械识别完整文本:', fullText);
            patchAiMessage(aiMsgId, (msg) => ({
              ...msg,
              isComplete: true,
              content: fullText,
              bilibiliUrl: res?.bilibiliUrl,
              douyinUrl: res?.douyinUrl,
              matchedExercises: res?.matchedExercises,
              equipmentName: res?.equipmentName,
            }));
          },
          (err) => {
            patchAiMessage(aiMsgId, (msg) => ({ ...msg, isComplete: true, content: '生成失败，请重试' }));
          },
        );
      } catch {
        timers.forEach(t => clearTimeout(t));
        patchAiMessage(aiMsgId, (msg) => ({ ...msg, recognitionLoading: false, isComplete: true, content: '识别失败，请重试' }));
      }
    }
  };

  const getCurrentMeal = () => {
    const h = new Date().getHours();
    if (h < 10) return '早餐';
    if (h < 14) return '午餐';
    if (h < 20) return '晚餐';
    return '加餐';
  };

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

  const handleModelRoleChange = (role: 'purificationModel' | 'chatModel' | 'whisperModel' | 'visionModel', value: string) => {
    setModelRoles(prev => ({ ...prev, [role]: value }));
    setModelConfigDirty(true);
  };

  const handleSaveModelConfig = async () => {
    try {
      setModelConfigLoading(true);
      await saveAllModelConfig(modelRoles);
      if (currentModel) {
        await switchModel(currentModel);
      }
      setModelConfigDirty(false);
      antMessage.success('模型配置已保存');
    } catch {
      antMessage.error('保存模型配置失败');
    } finally {
      setModelConfigLoading(false);
    }
  };

  const handleAddCustomModel = async () => {
    try {
      const values = await customForm.validateFields();
      setCustomModelLoading(true);
      if (editingModel) {
        await updateCustomModel({ name: editingModel, ...values });
        setModelOptions(prev => prev.map(m =>
          m.name === editingModel ? { ...m, label: values.label || values.name, type: values.type || 'text' } : m
        ));
        antMessage.success('自定义模型已更新');
      } else {
        await addCustomModel(values);
        setModelOptions(prev => [...prev, { name: values.name, label: values.label || values.name, type: values.type || 'text', isCustom: 'true' }]);
        antMessage.success('自定义模型已添加');
      }
      customForm.resetFields();
      setCustomModelFormOpen(false);
      setEditingModel(null);
    } catch (err: any) {
      if (err?.errorFields) return;
      antMessage.error(editingModel ? '更新失败' : '添加失败');
    } finally {
      setCustomModelLoading(false);
    }
  };

  const handleEditCustomModel = async (name: string) => {
    const model = modelOptions.find(m => m.name === name);
    if (!model || !(model as any).isCustom) return;
    setEditingModel(name);
    try {
      const detail = await getCustomModelDetail(name) as any;
      if (detail) {
        customForm.setFieldsValue({ name, label: detail.label, baseUrl: detail.baseUrl, apiKey: detail.apiKey, model: detail.model, type: detail.type });
      }
    } catch {
      customForm.setFieldsValue({ name, label: model.label, type: model.type });
    }
    setCustomModelFormOpen(true);
  };

  const handleDeleteCustomModel = async (name: string) => {
    try {
      await deleteCustomModel(name);
      setModelOptions(prev => prev.filter(m => m.name !== name));
      if (currentModel === name) setCurrentModel(modelOptions.find(m => m.name !== name)?.name || '');
      antMessage.success('已删除');
    } catch {
      antMessage.error('删除失败');
    }
  };

  const handleSend = useCallback(async () => {
    const text = inputValueRef.current.trim();
    if (!text) return;

    abortControllerRef.current?.abort();
    const abortController = new AbortController();
    abortControllerRef.current = abortController;

    const userMessage: Message = { id: createMessageId('user'), content: text, sender: 'user', timestamp: new Date() };
    const aiMessageId = createMessageId('ai');
    const aiMessage: Message = { id: aiMessageId, content: '', sender: 'ai', timestamp: new Date() };

    appendStoredMessages([userMessage, aiMessage]);
    setMessages(prev => [...prev, userMessage, aiMessage]);
    setInputValue('');
    setIsTyping(true);
    activeStreamMessageIdRef.current = aiMessageId;

    let isFirstChunk = true;

    try {
      await sendChatMessageStream(
        { message: text },
        (chunk) => {
          patchAiMessage(aiMessageId, (msg) => {
            const base = isFirstChunk ? '' : msg.content;
            isFirstChunk = false;
            return { ...msg, content: base + chunk, statusText: undefined };
          });
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
        abortController.signal,
        (status: string) => {
          patchAiMessage(aiMessageId, (msg) => ({ ...msg, statusText: status }));
        },
      );
    } catch {
      finalizeStreamMessage('不好意思，AI 调用失败，请联系工作人员或稍等片刻再试。');
    }
  }, [finalizeStreamMessage, patchAiMessage]);

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
    savePlan({ type, content: msg.content }, { skipErrorHandler: true })
      .then(() => {
        setMessages(prev => prev.map(m => m.id === msg.id ? { ...m, planSaved: true } : m));
        patchStoredMessage(msg.id, (m) => ({ ...m, planSaved: true }));
        emitStoredMessagesUpdated();
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
        antMessage.error(error?.message || '保存计划失败，请稍后重试');
      });
  };

  const handleClear = () => {
    abortControllerRef.current?.abort();
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
    // 流式输出中，内容还没开始时显示后端推送的状态
    if (msg.sender === 'ai' && !msg.content && !msg.recognitionLoading && !msg.isComplete && !msg.imageInfo) {
      return <span className="chat-thinking-status">{msg.statusText || '正在思考...'}</span>;
    }
    // 识别结果卡片
    if (msg.sender === 'ai' && msg.imageInfo) {
      return <FoodRecognitionCard message={msg} patchAiMessage={patchAiMessage} scrollToBottom={scrollToBottom} getCurrentMeal={getCurrentMeal} />;
    }
    // 识别加载中 — 步骤式提示（按类型区分）
    if (msg.sender === 'ai' && msg.recognitionLoading) {
      const step = msg.recognitionStep || 1;
      const stepMap: Record<string, string[]> = {
        food: ['正在识别食物...', '正在查询食物库...', '正在搜索营养数据...'],
        equipment: ['正在识别器械...', '正在联网搜索资料...', '正在整理生成回复...'],
        nutrition_label: ['正在识别标签...', '正在解析营养成分...'],
        form_check: ['正在分析动作姿势...', '正在生成评分建议...'],
        other: ['正在识别图片内容...', '正在分析处理...'],
      };
      const steps = stepMap[msg.recognitionType || 'other'] || stepMap.other;
      const done = steps.slice(0, step);
      const current = steps[step] || steps[steps.length - 1];
      return (
        <div className="chat-food-card">
          <div className="chat-recognition-steps">
            {done.map((s, i) => (
              <div key={i} className="chat-recognition-step chat-recognition-step-done">
                <span className="chat-recognition-step-icon">&#10003;</span>
                <span>{s}</span>
              </div>
            ))}
            <div className="chat-recognition-step chat-recognition-step-active">
              <span className="chat-recognition-step-icon"><Spin size="small" /></span>
              <span>{current}</span>
            </div>
          </div>
        </div>
      );
    }

    const isPlanMessage = msg.sender === 'ai' && (msg.replyType === 'training_plan' || msg.replyType === 'diet_plan');

    if (!isTodayExerciseRecordMessage(msg) && !isTodayDietRecordMessage(msg)) {
      if (msg.sender === 'ai' && msg.content) {
        return (
          <div className="chat-md-render">
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{msg.content || ''}</ReactMarkdown>
            {msg.matchedExercises && msg.matchedExercises.length > 0 && (
              <div style={{ marginTop: 12, padding: '8px 12px', background: 'rgba(22,74,65,0.08)', borderRadius: 8, border: '1px solid rgba(22,74,65,0.15)' }}>
                <div style={{ fontSize: 12, color: '#888', marginBottom: 6 }}>📌 动作库中可以用到该器械的动作</div>
                {msg.matchedExercises.slice(0, 3).map((ex: any) => (
                  <div key={ex.name} style={{ fontSize: 13, color: '#333', padding: '2px 0' }}>
                    · {ex.name}<span style={{ color: '#999', marginLeft: 6 }}>({MUSCLE_GROUP_LABELS[ex.muscleGroup] || ex.muscleGroup})</span>
                  </div>
                ))}
                {msg.matchedExercises.length > 3 && (
                  <div style={{ fontSize: 12, color: '#aaa', padding: '2px 0' }}>...等 {msg.matchedExercises.length} 个动作</div>
                )}
                {msg.equipmentName && (
                  <a
                    href={`/exercises?equipment=${encodeURIComponent(msg.equipmentName)}`}
                    style={{ display: 'inline-block', marginTop: 8, fontSize: 12, color: '#164A41', textDecoration: 'none' }}
                    onClick={(e) => {
                      e.preventDefault();
                      history.push(`/exercises?equipment=${encodeURIComponent(msg.equipmentName)}`);
                    }}
                  >
                    查看全部相关动作 →
                  </a>
                )}
              </div>
            )}
            {(msg.bilibiliUrl || msg.douyinUrl) && (
              <div style={{ marginTop: 12, display: 'flex', flexDirection: 'column', gap: 6 }}>
                {msg.bilibiliUrl && (
                  <a
                    href={msg.bilibiliUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8,
                      padding: '8px 14px',
                      borderRadius: 8,
                      background: 'rgba(251,114,153,0.08)',
                      border: '1px solid rgba(251,114,153,0.2)',
                      color: '#fb7299',
                      fontSize: 13,
                      textDecoration: 'none',
                      fontWeight: 500,
                      transition: 'background 0.2s',
                    }}
                    onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(251,114,153,0.15)'; }}
                    onMouseLeave={(e) => { e.currentTarget.style.background = 'rgba(251,114,153,0.08)'; }}
                  >
                    <BilibiliIcon size={18} />
                    <span>B站搜「{msg.equipmentName || '使用教程'}」</span>
                    <span style={{ marginLeft: 'auto', fontSize: 11, opacity: 0.5 }}>{new URL(msg.bilibiliUrl).hostname}</span>
                  </a>
                )}
                {msg.douyinUrl && (
                  <a
                    href={msg.douyinUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8,
                      padding: '8px 14px',
                      borderRadius: 8,
                      background: 'rgba(23,11,26,0.06)',
                      border: '1px solid rgba(23,11,26,0.12)',
                      color: '#170B1A',
                      fontSize: 13,
                      textDecoration: 'none',
                      fontWeight: 500,
                      transition: 'background 0.2s',
                    }}
                    onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(23,11,26,0.12)'; }}
                    onMouseLeave={(e) => { e.currentTarget.style.background = 'rgba(23,11,26,0.06)'; }}
                  >
                    <DouyinIcon size={18} />
                    <span>抖音搜「{msg.equipmentName || '使用教程'}」</span>
                    <span style={{ marginLeft: 'auto', fontSize: 11, opacity: 0.4 }}>{new URL(msg.douyinUrl).hostname}</span>
                  </a>
                )}
              </div>
            )}
          </div>
        );
      }
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

  const textModels = modelOptions.filter(m => (m.type || 'text') === 'text');
  const visionModels = modelOptions.filter(m => m.type === 'vision');
  const asrModels = modelOptions.filter(m => m.type === 'asr');

  const renderModelConfigPanel = () => (
    <div className="model-config-panel">
      <div className="model-config-item">
        <div className="model-config-label">
          <RobotOutlined style={{ marginRight: 6 }} />
          聊天模型
          <span className="model-config-hint">当前使用的对话模型</span>
        </div>
        <Select
          value={currentModel}
          onChange={(v) => { setCurrentModel(v); setModelConfigDirty(true); }}
          options={textModels.map(m => ({ value: m.name, label: m.label }))}
          style={{ width: '100%' }}
          loading={modelConfigLoading}
          popupClassName="model-config-popup"
        />
      </div>
      <div className="model-config-item">
        <div className="model-config-label">
          <SettingOutlined style={{ marginRight: 6 }} />
          提纯模型
          <span className="model-config-hint">工具决策 / 文本清洗</span>
        </div>
        <Select
          value={modelRoles.purificationModel || undefined}
          onChange={(v) => handleModelRoleChange('purificationModel', v)}
          options={textModels.map(m => ({ value: m.name, label: m.label }))}
          style={{ width: '100%' }}
          loading={modelConfigLoading}
          popupClassName="model-config-popup"
        />
      </div>
      <div className="model-config-item">
        <div className="model-config-label">
          <RobotOutlined style={{ marginRight: 6 }} />
          聪明模型
          <span className="model-config-hint">最终回复生成</span>
        </div>
        <Select
          value={modelRoles.chatModel || undefined}
          onChange={(v) => handleModelRoleChange('chatModel', v)}
          options={textModels.map(m => ({ value: m.name, label: m.label }))}
          style={{ width: '100%' }}
          loading={modelConfigLoading}
          popupClassName="model-config-popup"
        />
      </div>
      <div className="model-config-item">
        <div className="model-config-label">
          <CameraOutlined style={{ marginRight: 6 }} />
          视觉模型
          <span className="model-config-hint">图片理解 / 拍照识别</span>
        </div>
        <Select
          value={modelRoles.visionModel || undefined}
          onChange={(v) => handleModelRoleChange('visionModel', v)}
          options={visionModels.map(m => ({ value: m.name, label: m.label }))}
          style={{ width: '100%' }}
          loading={modelConfigLoading}
          popupClassName="model-config-popup"
        />
      </div>
      <div className="model-config-item">
        <div className="model-config-label">
          <AudioOutlined style={{ marginRight: 6 }} />
          语音识别模型
          <span className="model-config-hint">语音转文字</span>
        </div>
        <Select
          value={modelRoles.whisperModel || undefined}
          onChange={(v) => handleModelRoleChange('whisperModel', v)}
          options={asrModels.map(m => ({ value: m.name, label: m.label }))}
          style={{ width: '100%' }}
          loading={modelConfigLoading}
          popupClassName="model-config-popup"
        />
      </div>

      {/* 自定义模型 */}
      <div className="model-config-section">
        <div className="model-config-section-title">
          <span>我的模型</span>
          <Button type="link" size="small" icon={<PlusOutlined />} onClick={() => { customForm.resetFields(); setEditingModel(null); setCustomModelFormOpen(true); }}>添加</Button>
        </div>
        {modelOptions.filter(m => (m as any).isCustom).length === 0 && !customModelFormOpen && (
          <div className="model-config-empty">暂无自定义模型，点击上方添加</div>
        )}
        {modelOptions.filter(m => (m as any).isCustom).map(m => (
          <div className="model-config-custom-item" key={m.name}>
            <span className="model-config-custom-name">{m.label}<span className="model-config-custom-type">{m.type === 'asr' ? '语音' : m.type === 'vision' ? '视觉' : '文本'}</span></span>
            <span className="model-config-custom-actions">
              <Button type="text" size="small" icon={<EditOutlined />} onClick={() => handleEditCustomModel(m.name)} />
              <Button type="text" size="small" danger icon={<DeleteOutlined />} onClick={() => handleDeleteCustomModel(m.name)} />
            </span>
          </div>
        ))}
      </div>

      {customModelFormOpen && (
        <div className="model-config-custom-form">
          <Form form={customForm} layout="vertical" size="small">
            <div className="model-config-form-row">
              <Form.Item name="name" label="模型标识" rules={[{ required: true, message: '必填' }]}>
                <Input placeholder="如 my-gpt4" disabled={!!editingModel} />
              </Form.Item>
              <Form.Item name="label" label="显示名称">
                <Input placeholder="如 我的GPT-4" />
              </Form.Item>
            </div>
            <Form.Item name="type" label="模型类型" initialValue="text">
              <Select
                popupClassName="model-config-popup"
                options={[
                  { value: 'text', label: '文本对话' },
                  { value: 'vision', label: '视觉理解' },
                  { value: 'asr', label: '语音识别' },
                ]}
              />
            </Form.Item>
            <Form.Item name="baseUrl" label="API 地址" rules={[{ required: true, message: '必填' }]}>
              <Input placeholder="https://api.openai.com/v1" />
            </Form.Item>
            <div className="model-config-form-row">
              <Form.Item name="apiKey" label={editingModel ? 'API Key（留空则不修改）' : 'API Key'} rules={editingModel ? [] : [{ required: true, message: '必填' }]}>
                <Input.Password placeholder="sk-xxx" />
              </Form.Item>
              <Form.Item name="model" label="模型 ID" rules={[{ required: true, message: '必填' }]}>
                <Input placeholder="如 gpt-4o" />
              </Form.Item>
            </div>
            <div className="model-config-form-actions">
              <Button size="small" onClick={() => { setCustomModelFormOpen(false); setEditingModel(null); }}>取消</Button>
              <Button type="primary" size="small" loading={customModelLoading} onClick={handleAddCustomModel}>
                {editingModel ? '保存' : '添加'}
              </Button>
            </div>
          </Form>
        </div>
      )}

      <div className="model-config-desc">
        分层调用：提纯模型先分析意图并执行工具，聪明模型再生成最终回复，兼顾速度与质量。支持添加 OpenAI 兼容的自定义模型。
      </div>
    </div>
  );

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
              <span className="chat-header-box-btn" onClick={handleClear}>
                <ClearOutlined />
              </span>
            </Tooltip>
            <Tooltip title={deepThinking ? '关闭深度思考' : '开启深度思考'}>
              <span className={`chat-header-box-btn ${deepThinking ? 'chat-deep-active' : ''}`} onClick={() => {
                setDeepThinking(!deepThinking);
                setRecognitionTimeout(!deepThinking ? 120 : 60);
              }}>
                <span className="chat-deep-icon" />
                <span className="chat-deep-label">深度思考</span>
              </span>
            </Tooltip>
            <Tooltip title="模型配置">
              <span className="chat-header-box-btn" onClick={() => setModelConfigOpen(true)}>
                <span className="chat-model-icon" />
                <span className="chat-model-label">{modelOptions.find(m => m.name === currentModel)?.label || currentModel}</span>
              </span>
            </Tooltip>
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
                    {isUser && msg.userImageUrl && <img src={msg.userImageUrl} className="chat-user-img-preview" />}
                    {(isTraining(msg) || isDiet(msg)) && !msg.planSaved && (
                      <div style={{ marginTop: 12, display: 'flex', justifyContent: 'flex-end' }}>
                        <button
                          type="button"
                          onClick={(e) => handleSave(msg, e)}
                          style={{
                            display: 'flex', alignItems: 'center', gap: 8,
                            padding: '8px 14px', borderRadius: 8,
                            background: 'rgba(22,74,65,0.08)', border: '1px solid rgba(22,74,65,0.2)',
                            color: '#164A41', fontSize: 13, fontWeight: 500,
                            cursor: 'pointer', transition: 'background 0.2s',
                          }}
                          onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(22,74,65,0.15)'; }}
                          onMouseLeave={(e) => { e.currentTarget.style.background = 'rgba(22,74,65,0.08)'; }}
                        >
                          <SaveIcon size={16} />
                          <span>{isTraining(msg) ? '保存训练计划' : '保存饮食计划'}</span>
                        </button>
                      </div>
                    )}
                    {msg.planSaved && (
                      <div style={{ marginTop: 12, display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: 6, fontSize: 12, color: '#52c41a' }}>
                        <SaveIcon size={14} />
                        <span>已保存</span>
                      </div>
                    )}
                    {msg.showRecordButton && msg.recordType && !msg.recordSaved && (
                      <div style={{ marginTop: 12, display: 'flex', justifyContent: 'flex-end' }}>
                        <button
                          type="button"
                          disabled={msg.recordSaving}
                          onClick={() => {
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
                          }}
                          style={{
                            display: 'flex', alignItems: 'center', gap: 8,
                            padding: '8px 14px', borderRadius: 8,
                            background: msg.recordSaving ? 'rgba(22,74,65,0.04)' : 'rgba(22,74,65,0.08)',
                            border: '1px solid rgba(22,74,65,0.2)',
                            color: msg.recordSaving ? '#aaa' : '#164A41', fontSize: 13, fontWeight: 500,
                            cursor: msg.recordSaving ? 'not-allowed' : 'pointer',
                            transition: 'background 0.2s', opacity: msg.recordSaving ? 0.6 : 1,
                          }}
                          onMouseEnter={(e) => { if (!msg.recordSaving) e.currentTarget.style.background = 'rgba(22,74,65,0.15)'; }}
                          onMouseLeave={(e) => { e.currentTarget.style.background = 'rgba(22,74,65,0.08)'; }}
                        >
                          <SaveIcon size={16} />
                          <span>{msg.recordSaving ? '保存中...' : (msg.recordType === 'training' ? '一键记录训练' : '一键记录饮食')}</span>
                        </button>
                      </div>
                    )}
                    {msg.recordSaved && (
                      <div style={{ marginTop: 12, display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: 6, fontSize: 12, color: '#52c41a' }}>
                        <SaveIcon size={14} />
                        <span>已记录</span>
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
          {imagePanelOpen && (
            <div className="chat-image-quick-options">
              <img className="chat-image-quick-thumb" src={pendingImageDataUrl || ''} alt="" />
              <div className="chat-image-quick-chips">
                {IMAGE_OPTIONS.map(opt => (
                  <span key={opt.key} className="chat-image-quick-chip" onClick={() => handleImageOptionClick(opt)}>{opt.label}</span>
                ))}
              </div>
              <CloseOutlined className="chat-image-quick-close" onClick={closeImagePanel} />
            </div>
          )}
          {!imagePanelOpen && <div className="chat-float-btns">
            <span className="chat-float-btn" onClick={() => insertPrefix('帮我生成训练计划：')}>
              <ThunderboltOutlined />
              <span>生成训练计划</span>
            </span>
            <span className="chat-float-btn" onClick={() => insertPrefix('帮我生成饮食计划：')}>
              <FireOutlined />
              <span>生成饮食计划</span>
            </span>
            <span className="chat-float-btn" onClick={() => insertPrefix('帮我调整训练计划：')}>
              <EditOutlined />
              <span>调整训练计划</span>
            </span>
            <span className="chat-float-btn" onClick={() => insertPrefix('帮我调整饮食计划：')}>
              <EditOutlined />
              <span>调整饮食计划</span>
            </span>
            <span className="chat-float-btn" onClick={() => insertPrefix('记录一下我吃了：')}>
              <FormOutlined />
              <span>记录饮食</span>
            </span>
            <span className="chat-float-btn" onClick={() => insertPrefix('记录一下我练了：')}>
              <ThunderboltOutlined />
              <span>记录运动</span>
            </span>
            <span className="chat-float-btn" onClick={() => insertPrefix('查看我当前训练计划')}>
              <UnorderedListOutlined />
              <span>查看计划</span>
            </span>
            <span className="chat-float-btn" onClick={() => insertPrefix('看看我今天运动和饮食情况')}>
              <BarChartOutlined />
              <span>今日总结</span>
            </span>
          </div>}
          <div className="chat-input-row">
            <div className="chat-input-top">
              <span className="chat-toolbar-item chat-input-icon" onClick={() => foodFileInputRef.current?.click()}>
                <CameraOutlined />
              </span>
              <Input.TextArea
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onKeyDown={handleKeyPress}
                placeholder="问问 Tatan 今天的训练安排是什么..."
                autoSize={{ minRows: 1, maxRows: 3 }}
                style={{ flex: 1, resize: 'none' }}
                className="chat-input-field"
              />
              <span className={`chat-toolbar-item chat-input-icon ${isRecording ? 'chat-toolbar-active' : ''}`} onClick={toggleRecording}>
                <AudioOutlined />
              </span>
              <Button
                type="primary"
                icon={<SendOutlined />}
                onClick={handleSend}
                disabled={!inputValue.trim()}
                className="chat-send-btn"
              />
            </div>
          </div>
          <input ref={foodFileInputRef} type="file" accept="image/*" style={{ display: 'none' }} onChange={handleFoodImageSelect} />
          <div className="chat-quote">
            <span className="chat-quote-text">"{chatQuote}"</span>
            <span className="chat-quote-dash" />
          </div>
        </div>
      </div>

      {/* 模型配置 - 移动端 bottom sheet */}
      {isMobile && (modelConfigOpen || modelConfigSheet.mounted) && (
        <>
          <div className="chat-model-sheet-overlay" onClick={() => {
            if (modelConfigDirty) { antMessage.warning('有未保存的配置更改'); return; }
            modelConfigSheet.requestClose();
          }} />
          <div className="chat-model-sheet" style={modelConfigSheet.sheetStyle}>
            <div className="chat-model-sheet-handle" {...modelConfigSheet.dragHandleProps} />
            <div className="chat-model-sheet-head" {...modelConfigSheet.dragHandleProps}>
              <span className="chat-model-sheet-title">
                <RobotOutlined style={{ marginRight: 6 }} />
                模型配置
              </span>
              {modelConfigDirty && <span style={{ fontSize: 12, color: 'var(--ant-color-warning)' }}> *</span>}
            </div>
            <div className="chat-model-sheet-body">
              {renderModelConfigPanel()}
            </div>
            <div className="chat-model-sheet-footer">
              {modelConfigDirty && <span className="model-config-dirty-hint">有未保存的更改</span>}
              <Button onClick={() => { setModelRoles(DEFAULT_MODEL_ROLES); setModelConfigDirty(true); }} disabled={modelConfigLoading}>恢复默认</Button>
              <Button type="primary" icon={<SaveOutlined />} loading={modelConfigLoading} disabled={!modelConfigDirty} onClick={handleSaveModelConfig}>保存配置</Button>
            </div>
          </div>
        </>
      )}

      {/* 模型配置 - 桌面端右侧面板 */}
      {!isMobile && modelConfigOpen && (
        <>
          <div className="chat-model-config-overlay" onClick={() => { if (modelConfigDirty) { antMessage.warning('有未保存的配置更改'); return; } setModelConfigOpen(false); }} />
          <div className="chat-model-config-panel">
            <div className="chat-model-config-header">
              <span>模型配置{modelConfigDirty && <span style={{ fontSize: 12, color: 'var(--ant-color-warning)' }}> *</span>}</span>
              <Button type="text" icon={<CloseOutlined />} onClick={() => { if (modelConfigDirty) { antMessage.warning('有未保存的配置更改'); return; } setModelConfigOpen(false); }} />
            </div>
            <div className="chat-model-config-body">{renderModelConfigPanel()}</div>
            <div className="chat-model-config-footer">
              {modelConfigDirty && <span className="model-config-dirty-hint">有未保存的更改</span>}
              <Button onClick={() => { setModelRoles(DEFAULT_MODEL_ROLES); setModelConfigDirty(true); }} disabled={modelConfigLoading}>恢复默认</Button>
              <Button type="primary" icon={<SaveOutlined />} loading={modelConfigLoading} disabled={!modelConfigDirty} onClick={handleSaveModelConfig}>保存配置</Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default ChatPage;
