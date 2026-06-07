import { Avatar, Button, Form, Input, InputNumber, message, Select, Tooltip, Upload } from 'antd';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useModel, history } from '@umijs/max';
import { updateUserProfile, uploadAvatar, generateUserProfile, getFavoriteList, getProgressTrend, addExerciseRecord, addStructuredExerciseRecord, addDietRecord, deleteExerciseRecord, deleteDietRecord, searchFoods, updateDietRecord, updateExerciseRecord, getTodayDietRecords, getTodayExerciseRecords, getExerciseList, getExercisesByGroup } from '@/services/ant-design-pro/api';
import {
  UserOutlined,
  EditOutlined,
  CameraOutlined,
  FormOutlined,
  CalendarOutlined,
  ToolOutlined,
  HeartOutlined,
  FireOutlined,
  ClockCircleOutlined,
  CheckCircleFilled,
  PlusOutlined,
  CloseOutlined,
  SwapOutlined,
  ThunderboltOutlined,
  ExclamationCircleOutlined,
  RobotOutlined,
} from '@ant-design/icons';
import { MUSCLE_GROUP_LABELS } from '@/constants/exercise';
import WeightTrendChart from './components/WeightTrendChart';
import CalorieTrendCalendar from './components/CalorieTrendCalendar';
import { bmiStatus } from '@/utils/bmi';
import { getGreeting } from '@/utils/greeting';
import useBottomSheetGesture from '@/hooks/useBottomSheetGesture';
import './profile.less';

const PROFILE_ENCOURAGEMENTS = [
  '今天也在慢慢变强，继续保持现在的节奏就很好。',
  '你的每一次坚持，身体都会认真记住。',
  '不用着急变厉害，先把今天这一点点完成。',
  '状态有起伏很正常，愿意继续练就已经很棒了。',
  '你已经走在变好的路上，接下来只要继续。',
  '哪怕只是小进步，也值得被看见和肯定。',
  '今天的训练不需要完美，完成就已经有意义。',
  '把注意力放回自己，你正在一点点变得更稳。',
];

const fitnessGoals = [
  { value: 'muscle_gain', label: '增肌' },
  { value: 'fat_loss', label: '减脂' },
  { value: 'endurance', label: '耐力提升' },
  { value: 'flexibility', label: '柔韧性' },
  { value: 'general_fitness', label: '综合健身' },
];

const activityOptions = [
  { value: 'sedentary', label: '久坐少动', factor: 1.2 },
  { value: 'light', label: '轻度活动', factor: 1.375 },
  { value: 'moderate', label: '中度活动', factor: 1.55 },
  { value: 'active', label: '高频训练', factor: 1.725 },
  { value: 'very_active', label: '高强度体力', factor: 1.9 },
];

const experienceOptions = [
  { value: 'beginner', label: '新手' },
  { value: 'intermediate', label: '有经验' },
  { value: 'advanced', label: '高手' },
];

const equipmentOptions = [
  { value: '杠铃', label: '杠铃' },
  { value: '哑铃', label: '哑铃' },
  { value: '龙门架', label: '龙门架' },
  { value: '倒蹬机', label: '倒蹬机' },
  { value: '哈克深蹲机', label: '哈克深蹲机' },
  { value: '高位下拉器', label: '高位下拉器' },
  { value: '单杠', label: '单杠' },
  { value: '双杠', label: '双杠' },
  { value: '无器械', label: '无器械（自重）' },
  { value: '蝴蝶机', label: '蝴蝶机' },
  { value: '罗马椅', label: '罗马椅' },
  { value: '腿屈伸机', label: '腿屈伸机' },
  { value: '腿弯举机', label: '腿弯举机' },
  { value: '史密斯机', label: '史密斯机' },
  { value: '推肩机', label: '推肩机' },
];

const muscleGroupOptions = Object.entries(MUSCLE_GROUP_LABELS).map(([value, label]) => ({ value, label }));

const trainingDaysOptions = [
  { value: 1, label: '1 天/周' },
  { value: 2, label: '2 天/周' },
  { value: 3, label: '3 天/周' },
  { value: 4, label: '4 天/周' },
  { value: 5, label: '5 天/周' },
  { value: 6, label: '6 天/周' },
  { value: 7, label: '7 天/周' },
];

const durationOptions = [
  { value: 30, label: '30 分钟' },
  { value: 45, label: '45 分钟' },
  { value: 60, label: '60 分钟' },
  { value: 90, label: '90 分钟' },
  { value: 120, label: '120 分钟' },
];

const MEAL_OPTIONS = ['早餐', '午餐', '晚餐', '练后餐', '加餐'];
const kjToKcal = (value?: number) => (value || 0) / 4.184;
const formatFoodBase = (baseAmount?: number, unit?: string) => `${baseAmount || 100} ${unit || 'g'}`;
const EXERCISE_CALORIE_HINT = '推荐估算公式：有氧每分钟约消耗 2 kcal，无氧每分钟约消耗 5 kcal。具体消耗因人而异。';

const renderDietNutritionPreview = (preview: { calories: number; protein: number; carbs: number; fat: number; fiber: number }) => (
  <div className="profile-diet-edit-nutrition-preview">
    <div className="profile-diet-edit-nutrition-item">
      <span className="profile-diet-edit-nutrition-label">热量</span>
      <div className="profile-diet-edit-nutrition-metric">
        <strong className="profile-diet-edit-nutrition-value">{preview.calories.toFixed(0)}</strong>
        <span className="profile-diet-edit-nutrition-unit">kcal</span>
      </div>
    </div>
    <div className="profile-diet-edit-nutrition-item">
      <span className="profile-diet-edit-nutrition-label">蛋白质</span>
      <div className="profile-diet-edit-nutrition-metric">
        <strong className="profile-diet-edit-nutrition-value">{preview.protein.toFixed(1)}</strong>
        <span className="profile-diet-edit-nutrition-unit">g</span>
      </div>
    </div>
    <div className="profile-diet-edit-nutrition-item">
      <span className="profile-diet-edit-nutrition-label">碳水</span>
      <div className="profile-diet-edit-nutrition-metric">
        <strong className="profile-diet-edit-nutrition-value">{preview.carbs.toFixed(1)}</strong>
        <span className="profile-diet-edit-nutrition-unit">g</span>
      </div>
    </div>
    <div className="profile-diet-edit-nutrition-item">
      <span className="profile-diet-edit-nutrition-label">脂肪</span>
      <div className="profile-diet-edit-nutrition-metric">
        <strong className="profile-diet-edit-nutrition-value">{preview.fat.toFixed(1)}</strong>
        <span className="profile-diet-edit-nutrition-unit">g</span>
      </div>
    </div>
    <div className="profile-diet-edit-nutrition-item">
      <span className="profile-diet-edit-nutrition-label">纤维</span>
      <div className="profile-diet-edit-nutrition-metric">
        <strong className="profile-diet-edit-nutrition-value">{preview.fiber.toFixed(1)}</strong>
        <span className="profile-diet-edit-nutrition-unit">g</span>
      </div>
    </div>
  </div>
);

type SelectedFoodItem = {
  food: API.FoodItem;
  amount: number;
};

type EditableExerciseItem = {
  exerciseId?: number;
  name: string;
  muscleGroup?: string;
  completedSets?: number;
  durationSeconds?: number;
  caloriesBurned?: number;
  note?: string;
};

const personalityOptions = [
  { value: '自律型', label: '自律型 — 能严格按计划执行' },
  { value: '社交型', label: '社交型 — 喜欢团体训练氛围' },
  { value: '随意型', label: '随意型 — 看心情练，不想有压力' },
  { value: '竞技型', label: '竞技型 — 喜欢挑战和突破极限' },
  { value: '佛系型', label: '佛系型 — 保持健康就行，不追求成绩' },
];

const PROFILE_FORM_CACHE_KEY = 'profileFormData';
const currentWeekDayProgress = new Date().getDay() === 0 ? 7 : new Date().getDay();

const formatDuration = (seconds?: number) => {
  if (!seconds) return '-';
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return mins > 0 ? `${mins}分${secs > 0 ? secs + '秒' : ''}` : `${secs}秒`;
};

const formatCaloriesBurned = (value?: number) =>
  typeof value === 'number' && value > 0 ? `${value} kcal` : '';

function calcBmi(h: number, w: number) { return w / ((h / 100) ** 2); }

function normalizeFitnessGoal(goal?: string) {
  if (!goal) return undefined;
  const match = fitnessGoals.find(f => f.value === goal || f.label === goal);
  return match ? match.value : goal;
}

function getDietRecordImages(record: any): string[] {
  const directImages = [record?.imageUrl, record?.coverUrl, record?.foodImageUrl]
    .filter((url: unknown): url is string => typeof url === 'string' && !!url);
  if (directImages.length > 0) {
    return directImages.slice(0, 3);
  }

  const rawItems = Array.isArray(record?.items)
    ? record.items
    : typeof record?.items === 'string'
      ? (() => {
        try {
          const parsed = JSON.parse(record.items);
          return Array.isArray(parsed) ? parsed : [];
        } catch {
          return [];
        }
      })()
      : [];

  return rawItems
    .map((item: any) => item?.imageUrl || item?.foodImageUrl || item?.food?.imageUrl)
    .filter((url: unknown): url is string => typeof url === 'string' && !!url)
    .slice(0, 3);
}

function getExerciseSessionItems(record: API.StructuredExerciseSession | any): API.StructuredExerciseRecord[] {
  return Array.isArray(record?.items) ? record.items : [];
}

function getExerciseActionCount(records: API.StructuredExerciseSession[]) {
  return records.reduce((sum, record) => sum + getExerciseSessionItems(record).length, 0);
}

function parseCachedProfileForm() {
  try {
    const cached = localStorage.getItem(PROFILE_FORM_CACHE_KEY);
    return cached ? JSON.parse(cached) : undefined;
  } catch {
    return undefined;
  }
}

function splitCsv(value?: string) {
  return value?.split(',').filter(Boolean) || [];
}

function buildUserProfileText(values: Record<string, any>): string {
  const parts: string[] = [];
  if (values.occupation) parts.push(`职业:${values.occupation}`);
  if (values.personality) parts.push(`性格:${values.personality}`);
  if (values.weeklyTrainingDays) parts.push(`每周练${values.weeklyTrainingDays}天`);
  if (values.trainingDuration) parts.push(`每次${values.trainingDuration}分钟`);
  if (values.medicalHistory) parts.push(`伤病/病史:${values.medicalHistory}`);
  if (values.dietPreference) parts.push(`饮食:${values.dietPreference}`);
  if (values.additionalNotes) parts.push(`补充:${values.additionalNotes}`);
  return parts.join('；');
}

function getActivityLabel(activityLevel?: string) {
  return activityOptions.find((item) => item.value === activityLevel)?.label || '未设置';
}

function getMuscleGroupLabel(muscleGroup?: string) {
  return muscleGroup ? MUSCLE_GROUP_LABELS[muscleGroup] || muscleGroup : undefined;
}

function renderExerciseCaloriesLabel() {
  return (
    <span className="profile-inline-label">
      消耗热量
      <Tooltip title={EXERCISE_CALORIE_HINT}>
        <ExclamationCircleOutlined className="profile-inline-label-icon" />
      </Tooltip>
    </span>
  );
}

function getEffectiveDailyCalories(
  trendData?: API.UserProgressTrend | null,
  user?: API.CurrentUser,
) {
  return trendData?.customDailyCalories
    ?? user?.customDailyCalories
    ?? trendData?.dailyCalorieBurn
    ?? user?.dailyCalorieBurn
    ?? null;
}

function formatSignedNumber(value?: number, fractionDigits = 1) {
  if (value == null || Number.isNaN(value)) return '-';
  const fixed = value.toFixed(fractionDigits);
  return value > 0 ? `+${fixed}` : fixed;
}

function formatWeightChangeText(value?: number) {
  if (value == null || Number.isNaN(value)) return '暂无变化';
  if (value > 0) return `上升 ${value.toFixed(1)} kg`;
  if (value < 0) return `下降 ${Math.abs(value).toFixed(1)} kg`;
  return '无变化';
}

function getBalanceSummary(value?: number, scope = '今日') {
  if (value == null || Number.isNaN(value)) {
    return { label: '暂无热量数据', tone: 'neutral' as const, abs: 0 };
  }
  if (value > 0) {
    return { label: `${scope}还可吃 ${Math.round(value)} kcal`, tone: 'surplus' as const, abs: Math.abs(value) };
  }
  if (value < 0) {
    return { label: `${scope}吃超了 ${Math.round(Math.abs(value))} kcal`, tone: 'deficit' as const, abs: Math.abs(value) };
  }
  return { label: `${scope}热量刚好`, tone: 'neutral' as const, abs: 0 };
}

function parseUserProfile(text: string): Record<string, any> {
  const result: Record<string, any> = {};
  if (!text) return result;

  const matchField = (prefix: string) => {
    const m = text.match(new RegExp(`${prefix}[:：]([^；;]+)`));
    return m ? m[1].trim() : undefined;
  };

  result.occupation = matchField('职业');
  result.personality = matchField('性格');

  const daysMatch = text.match(/每周练(\d+)天/);
  if (daysMatch) result.weeklyTrainingDays = Number(daysMatch[1]);

  const durMatch = text.match(/每次(\d+)分钟/);
  if (durMatch) result.trainingDuration = Number(durMatch[1]);

  result.medicalHistory = matchField('伤病') || matchField('病史');
  result.dietPreference = matchField('饮食');

  const notesMatch = text.match(/补充[:：]([^；;]+)$/);
  result.additionalNotes = notesMatch ? notesMatch[1].trim() : undefined;

  return result;
}

const GenderButtons: React.FC<{ value?: number; onChange?: (v: number) => void }> = ({ value, onChange }) => (
  <div className="gender-btn-group">
    <div className={`gender-btn${value === 1 ? ' active' : ''}`} onClick={() => onChange?.(1)}>男</div>
    <div className={`gender-btn${value === 0 ? ' active' : ''}`} onClick={() => onChange?.(0)}>女</div>
  </div>
);

function getDaysSince(dateStr: string | Date | undefined): number {
  if (!dateStr) return 1;
  const tz = { timeZone: 'Asia/Shanghai' };
  const cStr = new Date(dateStr).toLocaleDateString('en-CA', tz);
  const nStr = new Date().toLocaleDateString('en-CA', tz);
  const diff = Math.round((new Date(nStr).getTime() - new Date(cStr).getTime()) / 86400000);
  return Math.max(1, diff + 1);
}

const Profile: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  const staticHeroMessage = useMemo(() => ({
    title: `${getGreeting()}，${initialState?.currentUser?.username?.trim() || '朋友'}`,
    sub: PROFILE_ENCOURAGEMENTS[0],
  }), [initialState?.currentUser?.username]);
  const [profileForm] = Form.useForm();
  const [profileOpen, setProfileOpen] = useState(false);
  const [profileLoading, setProfileLoading] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [editLoading, setEditLoading] = useState(false);
  const [editForm] = Form.useForm();
  const [trendData, setTrendData] = useState<API.UserProgressTrend | null>(null);
  const [favoriteCount, setFavoriteCount] = useState(0);
  const [recordDetailOpen, setRecordDetailOpen] = useState<'exercise' | 'diet' | null>(null);
  const [todayExerciseLoading, setTodayExerciseLoading] = useState(false);
  const [todayDietLoading, setTodayDietLoading] = useState(false);
  const [dietEditOpen, setDietEditOpen] = useState(false);
  const [exerciseEditOpen, setExerciseEditOpen] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<{ type: 'exercise' | 'diet'; index: number; label: string } | null>(null);
  const [returnToExerciseRecordSheet, setReturnToExerciseRecordSheet] = useState(false);
  const [returnToDietRecordSheet, setReturnToDietRecordSheet] = useState(false);
  const [dietEditingIndex, setDietEditingIndex] = useState<number | null>(null);
  const [exerciseEditingIndex, setExerciseEditingIndex] = useState<number | null>(null);
  const [exerciseEditTime, setExerciseEditTime] = useState('');
  const [exerciseEditName, setExerciseEditName] = useState('');
  const [exerciseEditDurationSeconds, setExerciseEditDurationSeconds] = useState<number | null>(null);
  const [exerciseEditCaloriesBurned, setExerciseEditCaloriesBurned] = useState<number | null>(null);
  const [exerciseEditNote, setExerciseEditNote] = useState('');
  const [exerciseEditItems, setExerciseEditItems] = useState<EditableExerciseItem[]>([]);
  const [exerciseLib, setExerciseLib] = useState<API.Exercise[]>([]);
  const [exerciseLibLoading, setExerciseLibLoading] = useState(false);
  const [exerciseEditSaving, setExerciseEditSaving] = useState(false);
  const [dietEditMealType, setDietEditMealType] = useState('早餐');
  const [dietEditNote, setDietEditNote] = useState('');
  const [dietEditFoods, setDietEditFoods] = useState<SelectedFoodItem[]>([]);
  const [dietEditKeyword, setDietEditKeyword] = useState('');
  const [dietEditOptions, setDietEditOptions] = useState<API.FoodItem[]>([]);
  const [dietEditLoading, setDietEditLoading] = useState(false);
  const [dietEditSaving, setDietEditSaving] = useState(false);
  const [isMobileProfile, setIsMobileProfile] = useState(
    () => typeof window !== 'undefined' && window.innerWidth <= 768,
  );
  const [weightTab, setWeightTab] = useState<'week' | 'month' | 'year'>('week');
  const [calorieTab, setCalorieTab] = useState<'week' | 'month' | 'year'>('week');
  const [trendLoading, setTrendLoading] = useState(false);
  const [calorieTrendLoading, setCalorieTrendLoading] = useState(false);
  const [calorieTrendData, setCalorieTrendData] = useState<API.UserProgressTrend | null>(null);
  const [weightDrillMonth, setWeightDrillMonth] = useState<{ year?: number; month?: number }>({});
  const [calorieDrillMonth, setCalorieDrillMonth] = useState<{ year?: number; month?: number }>({});
  const prevWeightTabRef = useRef(weightTab);
  const prevCalorieTabRef = useRef(calorieTab);

  const handleWeightTabChange = useCallback((tab: 'week' | 'month' | 'year') => {
    if (tab === prevWeightTabRef.current) return;
    prevWeightTabRef.current = tab;
    setWeightTab(tab);
    setTrendLoading(true);
  }, []);

  const handleCalorieTabChange = useCallback((tab: 'week' | 'month' | 'year') => {
    if (tab === prevCalorieTabRef.current) return;
    prevCalorieTabRef.current = tab;
    setCalorieTab(tab);
    setCalorieTrendLoading(true);
  }, []);
  const [todayExerciseList, setTodayExerciseList] = useState<API.StructuredExerciseSession[]>([]);
  const [todayDietList, setTodayDietList] = useState<any[]>([]);

  const fetchTodayExerciseRecords = useCallback(async () => {
    try {
      setTodayExerciseLoading(true);
      const res = await getTodayExerciseRecords({ skipErrorHandler: true });
      setTodayExerciseList(Array.isArray(res) ? res : []);
    } catch {
      setTodayExerciseList([]);
    } finally {
      setTodayExerciseLoading(false);
    }
  }, []);

  const fetchTodayDietRecords = useCallback(async () => {
    try {
      setTodayDietLoading(true);
      const res = await getTodayDietRecords({ skipErrorHandler: true });
      setTodayDietList(Array.isArray(res) ? res : []);
    } catch { setTodayDietList([]); }
    finally { setTodayDietLoading(false); }
  }, []);

  const fetchFavoriteCount = useCallback(async () => {
    try {
      const list = await getFavoriteList({ skipErrorHandler: true });
      setFavoriteCount(list?.length || 0);
    } catch { setFavoriteCount(0); }
  }, []);

  const fetchExerciseLib = useCallback(async (muscleGroup?: string) => {
    try {
      setExerciseLibLoading(true);
      const list = muscleGroup ? await getExercisesByGroup(muscleGroup) : await getExerciseList();
      setExerciseLib(Array.isArray(list) ? list : []);
    } catch { setExerciseLib([]); }
    finally { setExerciseLibLoading(false); }
  }, []);

  const fetchTodayRecords = useCallback(async () => {
    await Promise.all([fetchTodayExerciseRecords(), fetchTodayDietRecords()]);
  }, [fetchTodayDietRecords, fetchTodayExerciseRecords]);

  const openDietRecordDetail = useCallback(async () => {
    await fetchTodayDietRecords();
    setRecordDetailOpen('diet');
  }, [fetchTodayDietRecords]);

  const openExerciseRecordDetail = useCallback(async () => {
    await fetchTodayExerciseRecords();
    setRecordDetailOpen('exercise');
  }, [fetchTodayExerciseRecords]);

  const fetchTrend = useCallback(async (range: string = 'week') => {
    setTrendLoading(true);
    try {
      const res = await getProgressTrend({ params: { range }, skipErrorHandler: true });
      setTrendData(res || null);
    } catch {
      setTrendData(null);
    } finally {
      setTrendLoading(false);
    }
  }, []);

  const fetchCalorieTrend = useCallback(async (range: string = 'week') => {
    setCalorieTrendLoading(true);
    try {
      const res = await getProgressTrend({ params: { range }, skipErrorHandler: true });
      setCalorieTrendData(res || null);
    } catch {
      setCalorieTrendData(null);
    } finally {
      setCalorieTrendLoading(false);
    }
  }, []);

  const silentRefresh = useCallback(async () => {
    await Promise.all([fetchTodayRecords(), fetchFavoriteCount(), fetchTrend(), fetchCalorieTrend()]);
    initialState?.fetchUserInfo?.();
  }, [fetchTodayRecords, fetchFavoriteCount, fetchTrend, fetchCalorieTrend, initialState]);

  useEffect(() => { fetchFavoriteCount(); fetchTodayRecords(); }, [fetchFavoriteCount, fetchTodayRecords]);

  useEffect(() => { fetchTrend(weightTab); }, [weightTab, fetchTrend]);

  useEffect(() => { fetchCalorieTrend(calorieTab); }, [calorieTab, fetchCalorieTrend]);

  useEffect(() => {
    if (typeof window === 'undefined') return undefined;
    const media = window.matchMedia('(max-width: 768px)');
    const sync = () => setIsMobileProfile(media.matches);
    sync();
    if (media.addEventListener) {
      media.addEventListener('change', sync);
      return () => media.removeEventListener('change', sync);
    }
    media.addListener(sync);
    return () => media.removeListener(sync);
  }, []);

  useEffect(() => {
    if (!dietEditOpen) {
      setDietEditKeyword('');
      setDietEditOptions([]);
      return;
    }
    const timer = window.setTimeout(async () => {
      try {
        setDietEditLoading(true);
        const res = await searchFoods(dietEditKeyword.trim() || undefined, { skipErrorHandler: true });
        setDietEditOptions(Array.isArray(res) ? res : []);
      } catch {
        setDietEditOptions([]);
      } finally {
        setDietEditLoading(false);
      }
    }, 200);
    return () => window.clearTimeout(timer);
  }, [dietEditKeyword, dietEditOpen]);

  const profileSheet = useBottomSheetGesture(profileOpen && isMobileProfile, () => setProfileOpen(false));
  const recordSheet = useBottomSheetGesture(!!recordDetailOpen && isMobileProfile, () => setRecordDetailOpen(null));
  const editSheet = useBottomSheetGesture(editOpen && isMobileProfile, () => setEditOpen(false));
  const exerciseEditSheet = useBottomSheetGesture(exerciseEditOpen && isMobileProfile, () => {
    setExerciseEditOpen(false);
    if (returnToExerciseRecordSheet) {
      window.setTimeout(() => setRecordDetailOpen('exercise'), 180);
      setReturnToExerciseRecordSheet(false);
    }
  });
  const dietEditSheet = useBottomSheetGesture(dietEditOpen && isMobileProfile, () => {
    setDietEditOpen(false);
    if (returnToDietRecordSheet) {
      window.setTimeout(() => setRecordDetailOpen('diet'), 180);
      setReturnToDietRecordSheet(false);
    }
  });

  const overlayLocked = (!isMobileProfile && (!!recordDetailOpen || editOpen || exerciseEditOpen || dietEditOpen || !!pendingDelete || profileOpen))
    || profileSheet.mounted
    || recordSheet.mounted
    || editSheet.mounted
    || exerciseEditSheet.mounted
    || dietEditSheet.mounted;

  useEffect(() => {
    if (!overlayLocked) {
      document.documentElement.classList.remove('app-overlay-lock');
      document.body.classList.remove('app-overlay-lock');
      return;
    }
    document.documentElement.classList.add('app-overlay-lock');
    document.body.classList.add('app-overlay-lock');
    return () => {
      document.documentElement.classList.remove('app-overlay-lock');
      document.body.classList.remove('app-overlay-lock');
    };
  }, [overlayLocked]);

  const user = initialState?.currentUser;
  const effectiveDailyCalories = getEffectiveDailyCalories(calorieTrendData, user);
  const dailyCaloriesLabel = user?.customDailyCalories != null || calorieTrendData?.customDailyCalories != null
    ? '每日目标'
    : '预估日消耗';

  const handleEditSave = async () => {
    try {
      setEditLoading(true);
      const values = await editForm.validateFields();
      await updateUserProfile({
        ...values,
        customDailyCalories: values.customDailyCalories ?? -1,
        targetWeight: values.targetWeight ?? -1,
        gender: values.gender ?? user?.gender,
      });
      message.success('资料保存成功！');
      await silentRefresh();
    } catch (e: any) {
      message.error(e?.message || '保存失败');
    } finally {
      setEditLoading(false);
    }
  };

  const handleProfileSave = async () => {
    try {
      setProfileLoading(true);
      const values = await profileForm.validateFields();
      const profileFormData = buildUserProfileText(values);
      const aiProfile = await generateUserProfile({ profileData: profileFormData }) || profileFormData;
      const data = {
        username: user?.username,
        gender: user?.gender,
        height: user?.height,
        weight: user?.weight,
        age: user?.age,
        activityLevel: user?.activityLevel,
        customDailyCalories: user?.customDailyCalories,
        fitnessGoal: user?.fitnessGoal,
        experienceLevel: values.experienceLevel,
        preferredEquipment: values.preferredEquipment?.join(',') || null,
        userProfile: aiProfile,
        weeklyTrainingDays: values.weeklyTrainingDays,
        trainingDuration: values.trainingDuration,
        occupation: values.occupation,
        personality: values.personality,
        medicalHistory: values.medicalHistory,
        dietPreference: values.dietPreference,
        trainingPreference: values.additionalNotes,
      };
      await updateUserProfile(data);
      localStorage.setItem(PROFILE_FORM_CACHE_KEY, JSON.stringify(values));
      message.success('画像保存成功');
      if (isMobileProfile) {
        profileSheet.requestClose();
      } else {
        setProfileOpen(false);
      }
      initialState?.fetchUserInfo?.();
      fetchTrend();
      fetchCalorieTrend();
    } catch (e: any) {
      message.error(e?.message || '保存失败');
    } finally {
      setProfileLoading(false);
    }
  };

  const bmi = (user?.height && user?.weight) ? calcBmi(user.height, user.weight) : null;
  const status = bmi !== null ? bmiStatus(bmi) : null;
  const bmiRatio = bmi !== null ? Math.min(Math.max((bmi - 15) / 25, 0), 1) : 0;
  const weeklyPercent = Math.round((currentWeekDayProgress / 7) * 100);
  const latestTrendPoint = useMemo(() => {
    const points = calorieTrendData?.points || [];
    for (let i = points.length - 1; i >= 0; i -= 1) {
      const point = points[i];
      if (point && (point.calorieBalance != null || point.targetCalories != null || point.intakeCalories != null)) {
        return point;
      }
    }
    return null;
  }, [calorieTrendData?.points]);
  const latestCalorieBalance = useMemo(() => {
    const points = calorieTrendData?.points || [];
    for (let i = points.length - 1; i >= 0; i -= 1) {
      const value = points[i]?.calorieBalance;
      if (typeof value === 'number' && !Number.isNaN(value)) {
        return value;
      }
    }
    return null;
  }, [calorieTrendData?.points]);
  const todayExerciseCalories = useMemo(() => {
    const points = calorieTrendData?.points || [];
    const todayStr = new Date().toISOString().slice(0, 10);
    for (let i = points.length - 1; i >= 0; i -= 1) {
      const point = points[i];
      if (point?.date === todayStr && point.exerciseCalories != null) {
        return point.exerciseCalories;
      }
    }
    return 0;
  }, [calorieTrendData?.points]);

  const weightChange = useMemo(() => {
    if (weightTab === 'week') {
      return trendData?.weeklyWeightChange ?? null;
    }
    const points = trendData?.points || [];
    let first: number | null = null;
    let last: number | null = null;
    for (const p of points) {
      if (p.weight != null) {
        if (first == null) first = p.weight;
        last = p.weight;
      }
    }
    if (first != null && last != null) return last - first;
    return null;
  }, [weightTab, trendData?.weeklyWeightChange, trendData?.points]);

  const weightChangeLabel = weightTab === 'year' ? '年变化' : weightTab === 'month' ? '月变化' : '7天变化';

  const latestRemainingCalories = useMemo(() => {
    const target = effectiveDailyCalories;
    if (target == null) return null;
    const intake = latestTrendPoint?.intakeCalories ?? 0;
    return target - intake;
  }, [latestTrendPoint?.intakeCalories, effectiveDailyCalories]);

  const periodCalorieBalance = useMemo(() => {
    if (calorieTab === 'week') return latestRemainingCalories ?? null;
    const points = calorieTrendData?.points || [];
    let total = 0;
    let hasAny = false;
    for (const p of points) {
      const target = p.targetCalories;
      const intake = p.intakeCalories;
      if (target != null && intake != null) {
        total += (target - intake);
        hasAny = true;
      } else if (p.calorieBalance != null) {
        total += p.calorieBalance;
        hasAny = true;
      }
    }
    return hasAny ? total : null;
  }, [calorieTab, calorieTrendData?.points, latestRemainingCalories]);

  const periodCalorieLabel = calorieTab === 'year' ? '年盈余热量' : calorieTab === 'month' ? '月盈余热量' : '日盈余热量';
  const todayBalanceSummary = getBalanceSummary(latestRemainingCalories ?? undefined, '今日');
  const periodCalorieDisplay = useMemo(() => {
    if (periodCalorieBalance == null) {
      return { value: '未记录', label: calorieTab === 'week' ? '剩余热量' : periodCalorieLabel, color: undefined as string | undefined };
    }
    if (calorieTab === 'week') {
      if (periodCalorieBalance > 0) {
        return { value: `剩余 ${Math.round(periodCalorieBalance)} kcal`, label: '剩余热量', color: '#2a8a6d' };
      }
      if (periodCalorieBalance < 0) {
        return { value: `超出 ${Math.round(Math.abs(periodCalorieBalance))} kcal`, label: '剩余热量', color: '#e04848' };
      }
      return { value: '刚好 0 kcal', label: '剩余热量', color: undefined };
    }
    return {
      value: `${periodCalorieBalance > 0 ? '-' : '+'}${Math.round(Math.abs(periodCalorieBalance))} kcal`,
      label: periodCalorieLabel,
      color: periodCalorieBalance > 0 ? '#2a8a6d' : periodCalorieBalance < 0 ? '#e04848' : undefined,
    };
  }, [calorieTab, periodCalorieBalance, periodCalorieLabel]);
  const latestTargetCalories = effectiveDailyCalories ?? latestTrendPoint?.targetCalories;
  const latestIntakeCalories = latestTrendPoint?.intakeCalories ?? 0;

  const todayNutrition = useMemo(() => {
    const totals = { protein: 0, carbs: 0, fat: 0, fiber: 0 };
    for (const record of todayDietList) {
      totals.protein += record.protein || 0;
      totals.carbs += record.carbs || 0;
      totals.fat += record.fat || 0;
      totals.fiber += record.fiber || 0;
    }
    return totals;
  }, [todayDietList]);
  const balanceGaugeRatio = latestTargetCalories && latestRemainingCalories != null
    ? latestRemainingCalories / latestTargetCalories
    : null;
  const gaugeIsOver = balanceGaugeRatio != null && balanceGaugeRatio < 0;
  const gaugeIsNormal = balanceGaugeRatio != null && balanceGaugeRatio >= 0;
  const gaugeProgressRatio = balanceGaugeRatio != null
    ? Math.min(Math.abs(balanceGaugeRatio), 1)
    : 0;

  const handleDeleteRecord = async (type: 'exercise' | 'diet', index: number) => {
    const label = type === 'exercise' ? '训练记录' : '饮食记录';
    if (isMobileProfile) {
      if (!window.confirm(`确定删除这条${label}吗？`)) {
        return;
      }
      if (type === 'exercise') {
        await deleteExerciseRecord(index);
      } else {
        await deleteDietRecord(index);
      }
      message.success(`${label}已删除`);
      silentRefresh();
      return;
    }
    setPendingDelete({ type, index, label });
  };

  const confirmDeleteRecord = async () => {
    if (!pendingDelete) {
      return;
    }
    const { type, index, label } = pendingDelete;
    if (type === 'exercise') {
      await deleteExerciseRecord(index);
    } else {
      await deleteDietRecord(index);
    }
    setPendingDelete(null);
    message.success(`${label}已删除`);
    silentRefresh();
  };

  const handleOpenDietEdit = (record: any, index: number) => {
    const items = Array.isArray(record?.items) ? record.items : [];
    if (isMobileProfile && recordDetailOpen === 'diet') {
      setReturnToDietRecordSheet(true);
      setRecordDetailOpen(null);
    } else {
      setReturnToDietRecordSheet(false);
    }
    setDietEditingIndex(index);
    setDietEditMealType(record?.mealType || '早餐');
    setDietEditNote(record?.note || '');
    setDietEditFoods(items.map((item: any) => ({
      food: {
        id: item.foodItemId,
        name: item.name,
        imageUrl: item.imageUrl,
        unit: item.unit,
        baseAmount: item.baseAmount || 100,
        calories: typeof item.calories === 'number' ? item.calories * 4.184 : 0,
        protein: item.protein,
        carbs: item.carbs,
        fat: item.fat,
        fiber: item.fiber,
      },
      amount: Number(item.amount) || 0,
    })));
    setDietEditKeyword('');
    setDietEditOptions([]);
    setDietEditOpen(true);
  };

  const handleOpenExerciseEdit = (record: API.StructuredExerciseSession, index: number) => {
    fetchExerciseLib();
    if (isMobileProfile && recordDetailOpen === 'exercise') {
      setReturnToExerciseRecordSheet(true);
      setRecordDetailOpen(null);
    } else {
      setReturnToExerciseRecordSheet(false);
    }
    setExerciseEditingIndex(index);
    setExerciseEditTime(record.time || '');
    setExerciseEditName(record.name || '');
    setExerciseEditDurationSeconds(record.durationSeconds ?? null);
    setExerciseEditCaloriesBurned(record.caloriesBurned ?? null);
    setExerciseEditNote(record.note || '');
    setExerciseEditItems(
      getExerciseSessionItems(record).map((item) => ({
        exerciseId: item.exerciseId,
        name: item.name || '',
        muscleGroup: item.muscleGroup,
        completedSets: item.completedSets,
        totalSets: undefined,
        durationSeconds: item.durationSeconds,
        note: item.note,
      })),
    );
    setExerciseEditOpen(true);
  };

  const handleOpenDietCreate = () => {
    if (isMobileProfile && recordDetailOpen === 'diet') {
      setReturnToDietRecordSheet(true);
      setRecordDetailOpen(null);
    } else {
      setReturnToDietRecordSheet(false);
    }
    setDietEditingIndex(null);
    setDietEditMealType('早餐');
    setDietEditNote('');
    setDietEditFoods([]);
    setDietEditKeyword('');
    setDietEditOptions([]);
    setDietEditOpen(true);
  };

  const handleOpenExerciseCreate = () => {
    fetchExerciseLib();
    if (isMobileProfile && recordDetailOpen === 'exercise') {
      setReturnToExerciseRecordSheet(true);
      setRecordDetailOpen(null);
    } else {
      setReturnToExerciseRecordSheet(false);
    }
    setExerciseEditingIndex(null);
    setExerciseEditTime('');
    setExerciseEditName('');
    setExerciseEditDurationSeconds(null);
    setExerciseEditCaloriesBurned(null);
    setExerciseEditNote('');
    setExerciseEditItems([{ name: '', muscleGroup: undefined, completedSets: 1, durationSeconds: undefined, note: '' }]);
    setExerciseEditOpen(true);
  };

  const closeDietEdit = () => {
    if (isMobileProfile && dietEditSheet.mounted) {
      dietEditSheet.requestClose();
      return;
    }
    setDietEditOpen(false);
    if (returnToDietRecordSheet) {
      setRecordDetailOpen('diet');
      setReturnToDietRecordSheet(false);
    }
  };

  const closeExerciseEdit = () => {
    if (isMobileProfile && exerciseEditSheet.mounted) {
      exerciseEditSheet.requestClose();
      return;
    }
    setExerciseEditOpen(false);
    if (returnToExerciseRecordSheet) {
      setRecordDetailOpen('exercise');
      setReturnToExerciseRecordSheet(false);
    }
  };

  const handleExerciseItemChange = (itemIndex: number, patch: Partial<EditableExerciseItem>) => {
    setExerciseEditItems((prev) => prev.map((item, index) => (index === itemIndex ? { ...item, ...patch } : item)));
  };

  const handleAddExerciseItem = () => {
    setExerciseEditItems((prev) => [...prev, { name: '', muscleGroup: undefined, completedSets: 1, durationSeconds: undefined, note: '' }]);
  };

  const handleRemoveExerciseItem = (itemIndex: number) => {
    setExerciseEditItems((prev) => prev.filter((_, index) => index !== itemIndex));
  };

  const handleAddEditFood = (food: API.FoodItem) => {
    setDietEditFoods((prev) => {
      if (prev.some((item) => item.food.id === food.id)) {
        return prev;
      }
      return [...prev, { food, amount: food.baseAmount || 100 }];
    });
  };

  const handleEditFoodAmountChange = (foodId: number, amount: number | null) => {
    setDietEditFoods((prev) => prev.map((item) => (item.food.id === foodId ? { ...item, amount: amount || 0 } : item)));
  };

  const handleRemoveEditFood = (foodId: number) => {
    setDietEditFoods((prev) => prev.filter((item) => item.food.id !== foodId));
  };

  const dietEditPreview = useMemo(() => dietEditFoods.reduce((acc, item) => {
    const baseAmount = item.food.baseAmount || 100;
    const ratio = (item.amount || 0) / baseAmount;
    acc.calories += kjToKcal(item.food.calories) * ratio;
    acc.protein += (item.food.protein || 0) * ratio;
    acc.carbs += (item.food.carbs || 0) * ratio;
    acc.fat += (item.food.fat || 0) * ratio;
    acc.fiber += (item.food.fiber || 0) * ratio;
    return acc;
  }, { calories: 0, protein: 0, carbs: 0, fat: 0, fiber: 0 }), [dietEditFoods]);

  const handleSaveDietEdit = async () => {
    if (dietEditFoods.length === 0) {
      message.warning('请至少保留一个食物');
      return;
    }
    if (dietEditFoods.some((item) => !item.amount || item.amount <= 0)) {
      message.warning('食物摄入量必须大于 0');
      return;
    }
    try {
      setDietEditSaving(true);
      const payload = {
        mealType: dietEditMealType,
        note: dietEditNote.trim() || undefined,
        items: dietEditFoods.map((item) => ({
          foodItemId: item.food.id,
          amount: item.amount,
        })),
      };
      if (dietEditingIndex == null) {
        await addDietRecord(payload, { skipErrorHandler: true });
        message.success('饮食记录已添加');
      } else {
        await updateDietRecord(dietEditingIndex, payload, { skipErrorHandler: true });
        message.success('饮食记录已更新');
      }
      silentRefresh();
    } catch (e: any) {
      message.error(e?.message || '更新失败');
    } finally {
      setDietEditSaving(false);
    }
  };

  const handleSaveExerciseEdit = async () => {
    const cleanedItems = exerciseEditItems
      .map((item) => ({
        ...item,
        name: item.name.trim(),
        note: item.note?.trim() || undefined,
      }))
      .filter((item) => item.name);
    if (cleanedItems.length === 0) {
      message.warning('请至少保留一个训练动作');
      return;
    }
    try {
      setExerciseEditSaving(true);
      const payload = {
        time: exerciseEditTime || undefined,
        name: exerciseEditName.trim() || undefined,
        durationSeconds: exerciseEditDurationSeconds ?? undefined,
        caloriesBurned: exerciseEditCaloriesBurned ?? undefined,
        note: exerciseEditNote.trim() || undefined,
        source: 'manual',
        items: cleanedItems,
      };
      if (exerciseEditingIndex == null) {
        await addStructuredExerciseRecord(payload, { skipErrorHandler: true });
        message.success('训练记录已添加');
      } else {
        await updateExerciseRecord(exerciseEditingIndex, payload, { skipErrorHandler: true });
        message.success('训练记录已更新');
      }
      silentRefresh();
    } catch (e: any) {
      message.error(e?.message || '更新失败');
    } finally {
      setExerciseEditSaving(false);
    }
  };

  return (
    <div className="profile-page">
      <div className="profile-container">
        <div className="profile-content">
          {/* ===== 区域 B: 资料卡片 ===== */}
          <div className="profile-banner glass-card">
            <Upload
              showUploadList={false}
              beforeUpload={(file) => {
                const ok = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type);
                if (!ok) { message.error('只支持 jpg/png/gif/webp'); return Upload.LIST_IGNORE; }
                if (file.size > 5 * 1024 * 1024) { message.error('头像不超过5MB'); return Upload.LIST_IGNORE; }
                return true;
              }}
              customRequest={async ({ file }) => {
                const fd = new FormData(); fd.append('file', file);
                try { await uploadAvatar(fd); message.success('头像更新成功'); initialState?.fetchUserInfo?.(); }
                catch (e: any) { message.error(e?.message || '上传失败'); }
              }}
            >
              <div className="profile-avatar-ring">
                <div className="profile-avatar-click">
                  <Avatar
                    size={108}
                    src={user?.avatarUrl}
                    icon={!user?.avatarUrl ? <UserOutlined /> : undefined}
                    className="profile-avatar-img"
                    style={{
                      backgroundColor: !user?.avatarUrl ? 'oklch(0.93 0.015 230)' : undefined,
                      color: !user?.avatarUrl ? 'oklch(0.35 0.08 230)' : undefined,
                    }}
                  />
                  <div className="profile-avatar-overlay">
                    <CameraOutlined style={{ fontSize: 20, color: '#fff' }} />
                  </div>
                </div>
              </div>
            </Upload>

            <div className="profile-banner-info">
              <div className="profile-username">
                {user?.username}
                <EditOutlined className="profile-username-edit" onClick={() => {
                  editForm.setFieldsValue({
                    username: user?.username,
                    fitnessGoal: normalizeFitnessGoal(user?.fitnessGoal),
                    height: user?.height,
                    weight: user?.weight,
                    age: user?.age,
                    gender: user?.gender,
                    activityLevel: user?.activityLevel,
                    customDailyCalories: user?.customDailyCalories,
                    targetWeight: user?.targetWeight,
                    city: user?.city,
                    cityEn: user?.cityEn,
                  });
                  setEditOpen(true);
                }} />
              </div>
              <div className="profile-account">{user?.userAccount}</div>
            </div>

            <div className="profile-stats-row">
              <div className="profile-stat-item">
                <CalendarOutlined className="profile-stat-icon" />
                <div className="profile-stat-body">
                  <div className="profile-stat-value">{user?.createTime ? getDaysSince(user.createTime) : '-'}</div>
                  <div className="profile-stat-label">加入天数</div>
                </div>
              </div>
              <div className="profile-stat-divider" />
              <div className="profile-stat-item profile-stat-item--clickable" onClick={() => {
                const parsed = parseCachedProfileForm() || parseUserProfile(user?.userProfile || '');
                profileForm.setFieldsValue({
                  experienceLevel: user?.experienceLevel || undefined,
                  preferredEquipment: splitCsv(user?.preferredEquipment),
                  ...parsed,
                });
                setProfileOpen(true);
              }}>
                <ToolOutlined className="profile-stat-icon" />
                <div className="profile-stat-body">
                  <div className="profile-stat-value">{splitCsv(user?.preferredEquipment).length}</div>
                  <div className="profile-stat-label">可用器械</div>
                </div>
              </div>
              <div className="profile-stat-divider" />
              <div className="profile-stat-item profile-stat-item--clickable" onClick={() => history.push('/user/favorites')}>
                <HeartOutlined className="profile-stat-icon" />
                <div className="profile-stat-body">
                  <div className="profile-stat-value">{favoriteCount}</div>
                  <div className="profile-stat-label">收藏动作</div>
                </div>
              </div>
            </div>
          </div>

          {/* ===== 今日记录 ===== */}
          {(() => {
            const exList = todayExerciseList;
            const exerciseTotalDuration = exList.reduce((sum, r) => sum + (r.durationSeconds || 0), 0);
            const exerciseActionCount = getExerciseActionCount(exList);
            return (
              <div className="profile-card-today glass-card">
                <div className="today-card-title">
                  <span className="ai-card-section-title">今日记录</span>
                </div>
                <div className="today-card-body">
                  <div className="today-record-items">
                    <div
                      className="today-record-item today-record-item--clickable"
                      onClick={openExerciseRecordDetail}
                    >
                      <div className="today-record-item-icon"><FireOutlined /></div>
                      <div className="today-record-item-info">
                        <div className="today-record-item-label">运动记录</div>
                        <div className="today-record-item-desc">
                          {todayExerciseLoading
                            ? '加载中...'
                            : exList.length > 0
                            ? `已完成 ${exerciseActionCount} 项${exerciseTotalDuration > 0 ? ` · 共 ${formatDuration(exerciseTotalDuration)}` : ''}`
                            : '暂无记录'}
                        </div>
                      </div>
                      {exList.length > 0 && <CheckCircleFilled className="today-record-item-check" />}
                    </div>
                    <div className="today-record-item today-record-item--clickable" onClick={openDietRecordDetail}>
                      <div className="today-record-item-icon today-record-item-icon--diet"><FormOutlined /></div>
                      <div className="today-record-item-info">
                        <div className="today-record-item-label">饮食记录</div>
                        <div className="today-record-item-desc">
                          {todayDietLoading ? '加载中...' : todayDietList.length > 0 ? `已记录 ${todayDietList.length} 项` : '暂无记录'}
                        </div>
                      </div>
                      {todayDietList.length > 0 && <CheckCircleFilled className="today-record-item-check" />}
                    </div>
                  </div>
                  {user?.targetWeight != null && user?.weight != null && user.weight !== user.targetWeight && (
                    <div className="today-record-motivation">
                      当前目标：{user.targetWeight}kg，{user.weight > user.targetWeight ? '还需减掉' : '还需增重'}{Math.abs(user.weight - user.targetWeight).toFixed(1)}kg，加油！
                    </div>
                  )}
                  {user?.city && (
                    <div className="today-record-motivation">
                      📍 {user.city}
                    </div>
                  )}
                </div>
              </div>
            );
          })()}

          {/* ===== 训练进度 + AI 画像 ===== */}
          <div className="profile-card-ai glass-card">
            <div className="ai-card-body">
              <div className="ai-card-left">
                <div className="ai-card-section-title">训练进度</div>
                <div className="profile-bars">
                  {bmi !== null && status && (
                    <div className="profile-bar">
                      <div className="profile-bar-label">
                        <span>BMI 指数</span>
                        <span className="profile-bar-value" style={{ color: status.color }}>{bmi.toFixed(1)} · {status.text}</span>
                      </div>
                      <div className="profile-bar-track">
                        <div className="profile-bar-fill" style={{ width: `${Math.min(bmiRatio * 100, 100)}%`, background: status.color }} />
                      </div>
                    </div>
                  )}
                  <div className="profile-bar">
                    <div className="profile-bar-label">
                      <span>本周进度</span>
                      <span className="profile-bar-value">{currentWeekDayProgress}/7 天</span>
                    </div>
                    <div className="profile-bar-track">
                      <div className="profile-bar-fill" style={{ width: `${weeklyPercent}%` }} />
                    </div>
                  </div>
                </div>
              </div>
              <div className="ai-card-right">
                {isMobileProfile ? (
                  <RobotOutlined className="ai-profile-mobile-edit" onClick={() => {
                    const parsed = parseCachedProfileForm() || parseUserProfile(user?.userProfile || '');
                    profileForm.setFieldsValue({
                      experienceLevel: user?.experienceLevel || undefined,
                      preferredEquipment: splitCsv(user?.preferredEquipment),
                      ...parsed,
                    });
                    setProfileOpen(true);
                  }} />
                ) : (
                  <div className="ai-card-section-title">
                    AI 画像
                    <EditOutlined className="ai-profile-edit-icon" onClick={() => {
                      const parsed = parseCachedProfileForm() || parseUserProfile(user?.userProfile || '');
                      profileForm.setFieldsValue({
                        experienceLevel: user?.experienceLevel || undefined,
                        preferredEquipment: splitCsv(user?.preferredEquipment),
                        ...parsed,
                      });
                      setProfileOpen(true);
                    }} />
                  </div>
                )}
                {user?.userProfile ? (
                  <div className="ai-profile-full">{user.userProfile}</div>
                ) : (
                  <div className="ai-profile-empty">未设置，点击右上角编辑按钮补充你的训练背景和偏好。</div>
                )}
              </div>
            </div>
          </div>

          <div className="profile-trend-card glass-card">
            <div className="profile-trend-head">
              <div>
                <div className="ai-card-section-title">体重与热量趋势</div>
                <div className="profile-trend-sub">最近 7 天的记录，未称重日沿用前一日体重。</div>
              </div>
            </div>
            <div className="profile-trend-grid">
              <div className="profile-trend-panel">
                <div className="profile-trend-panel-header">
                  <div className="profile-trend-panel-title">体重曲线</div>
                  <div className="weight-chart-tabs">
                    {(['week', 'month', 'year'] as const).map((tab) => (
                      <button
                        key={tab}
                        type="button"
                        className={`weight-chart-tab${weightTab === tab ? ' active' : ''}`}
                        onClick={() => handleWeightTabChange(tab)}
                      >
                        {tab === 'week' ? '周' : tab === 'month' ? '月' : '年'}
                      </button>
                    ))}
                  </div>
                </div>
                <div className="profile-stats-row profile-trend-panel-stats">
                  <div className="profile-stat-item">
                    <CalendarOutlined className="profile-stat-icon" />
                    <div className="profile-stat-body">
                      <div className="profile-stat-value">{trendData?.latestWeight != null ? `${trendData.latestWeight.toFixed(2)} kg` : '未记录'}</div>
                      <div className="profile-stat-label">最新体重</div>
                    </div>
                  </div>
                  <div className="profile-stat-divider" />
                  <div className="profile-stat-item">
                    <SwapOutlined className="profile-stat-icon" />
                    <div className="profile-stat-body">
                      <div className="profile-stat-value" style={{ color: weightChange != null && weightChange < 0 ? '#2a8a6d' : weightChange != null && weightChange > 0 ? '#e04848' : undefined }}>
                        {formatWeightChangeText(weightChange ?? undefined)}
                      </div>
                      <div className="profile-stat-label">{weightChangeLabel}</div>
                    </div>
                  </div>
                  <div className="profile-stat-divider" />
                  <div className="profile-stat-item">
                    <FireOutlined className="profile-stat-icon" />
                    <div className="profile-stat-body">
                      <div className="profile-stat-value">{user?.targetWeight != null ? `${user.targetWeight} kg` : '未设置'}</div>
                      <div className="profile-stat-label">目标体重</div>
                    </div>
                  </div>
                </div>
                <WeightTrendChart
                  key={weightTab}
                  points={trendData?.points || []}
                  period={weightTab}
                  loading={trendLoading}
                  drillYear={weightDrillMonth.year}
                  drillMonth={weightDrillMonth.month}
                  onDrillDown={(y, m) => { setWeightDrillMonth({ year: y, month: m }); handleWeightTabChange('month'); }}
                />
              </div>
              <div className="profile-trend-panel">
                <div className="profile-trend-panel-header">
                  <div className="profile-trend-panel-title">今日热量盈亏</div>
                  <div className="weight-chart-tabs">
                    {(['week', 'month', 'year'] as const).map((tab) => (
                      <button
                        key={tab}
                        type="button"
                        className={`weight-chart-tab${calorieTab === tab ? ' active' : ''}`}
                        onClick={() => handleCalorieTabChange(tab)}
                      >
                        {tab === 'week' ? '周' : tab === 'month' ? '月' : '年'}
                      </button>
                    ))}
                  </div>
                </div>
                <div className="profile-stats-row profile-trend-panel-stats">
                  <div className="profile-stat-item">
                    <ThunderboltOutlined className="profile-stat-icon" />
                    <div className="profile-stat-body">
                      <div className="profile-stat-value">{effectiveDailyCalories != null ? Math.round(effectiveDailyCalories) : '-'}</div>
                      <div className="profile-stat-label">{dailyCaloriesLabel}</div>
                    </div>
                  </div>
                  <div className="profile-stat-divider" />
                  <div className="profile-stat-item">
                    <FireOutlined className="profile-stat-icon" />
                    <div className="profile-stat-body">
                      <div className="profile-stat-value" style={{ color: periodCalorieDisplay.color }}>
                        {periodCalorieDisplay.value}
                      </div>
                      <div className="profile-stat-label">{periodCalorieDisplay.label}</div>
                    </div>
                  </div>
                  <div className="profile-stat-divider" />
                  <div className="profile-stat-item">
                    <HeartOutlined className="profile-stat-icon" />
                    <div className="profile-stat-body">
                      <div className="profile-stat-value">{todayExerciseCalories != null ? `${todayExerciseCalories} kcal` : '-'}</div>
                      <div className="profile-stat-label">运动消耗</div>
                    </div>
                  </div>
                </div>
                {calorieTab === 'week' && <div className="profile-balance-gauge-wrap">
                  <div className={`profile-balance-gauge${gaugeIsOver ? ' is-over' : gaugeIsNormal ? ' is-normal' : ''}`}>
                    <svg className="profile-balance-gauge-svg" viewBox="0 0 220 110" preserveAspectRatio="none" aria-hidden="true">
                      <path
                        className="profile-balance-gauge-track"
                        d="M 28 100 A 82 82 0 0 1 192 100"
                        pathLength="100"
                      />
                      {balanceGaugeRatio != null && (
                        <path
                          className={`profile-balance-gauge-stroke${gaugeIsOver ? ' is-over' : ' is-normal'}`}
                          d="M 28 100 A 82 82 0 0 1 192 100"
                          pathLength="100"
                          strokeDasharray={`${gaugeProgressRatio * 100} 100`}
                        />
                      )}
                      <defs>
                        <linearGradient id="balanceGaugeNormalGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                          <stop offset="0%" stopColor="#8ee7c2" />
                          <stop offset="55%" stopColor="#5bbf95" />
                          <stop offset="100%" stopColor="#2a8a6d" />
                        </linearGradient>
                        <linearGradient id="balanceGaugeOverGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                          <stop offset="0%" stopColor="#fca5a5" />
                          <stop offset="55%" stopColor="#f07878" />
                          <stop offset="100%" stopColor="#e04848" />
                        </linearGradient>
                      </defs>
                    </svg>
                    <div className="profile-balance-gauge-inner">
                      <div className={`profile-balance-gauge-value${gaugeIsOver ? ' is-over' : gaugeIsNormal ? ' is-normal' : ''}`}>
                        {latestRemainingCalories != null
                          ? (gaugeIsOver ? `+${Math.round(Math.abs(latestRemainingCalories))}` : `${Math.round(latestRemainingCalories)}`) + ' kcal'
                          : '--'}
                      </div>
                      <div className={`profile-balance-gauge-text${gaugeIsOver ? ' is-over' : gaugeIsNormal ? ' is-normal' : ''}`}>{todayBalanceSummary.label}</div>
                    </div>
                  </div>
                </div>}
                <div className="profile-nutrition-row">
                  <div className="profile-nutrition-item">
                    <div className="profile-nutrition-top">
                      <span className="profile-nutrition-value">{todayNutrition.protein.toFixed(1)}</span>
                      <span className="profile-nutrition-unit">g</span>
                    </div>
                    <span className="profile-nutrition-label">蛋白质</span>
                  </div>
                  <div className="profile-nutrition-item">
                    <div className="profile-nutrition-top">
                      <span className="profile-nutrition-value">{todayNutrition.carbs.toFixed(1)}</span>
                      <span className="profile-nutrition-unit">g</span>
                    </div>
                    <span className="profile-nutrition-label">碳水</span>
                  </div>
                  <div className="profile-nutrition-item">
                    <div className="profile-nutrition-top">
                      <span className="profile-nutrition-value">{todayNutrition.fiber.toFixed(1)}</span>
                      <span className="profile-nutrition-unit">g</span>
                    </div>
                    <span className="profile-nutrition-label">膳食纤维</span>
                  </div>
                  <div className="profile-nutrition-item">
                    <div className="profile-nutrition-top">
                      <span className="profile-nutrition-value">{todayNutrition.fat.toFixed(1)}</span>
                      <span className="profile-nutrition-unit">g</span>
                    </div>
                    <span className="profile-nutrition-label">脂肪</span>
                  </div>
                </div>
                {calorieTab === 'week' ? (
                  <div className="profile-calorie-bars">
                    {(calorieTrendData?.points || []).map((point) => {
                      const target = point.targetCalories || effectiveDailyCalories || 1;
                      const intake = point.intakeCalories ?? (point.targetCalories != null ? 0 : null);
                      const remaining = intake != null ? target - intake : null;
                      const canStillEat = (remaining ?? 0) > 0;
                      const overAmount = remaining != null && remaining < 0 ? Math.abs(remaining) : 0;
                      const OVER_CAP = 500;

                      let barHeight = 0;
                      if (remaining == null) {
                        barHeight = 0;
                      } else if (canStillEat) {
                        barHeight = Math.max(Math.min(remaining / target, 1) * 100, 8);
                      } else {
                        barHeight = Math.max(Math.min(overAmount / OVER_CAP, 1) * 100, 8);
                      }

                      return (
                        <div key={point.date} className={`profile-calorie-bar-wrap ${canStillEat ? '' : 'is-over'}`}>
                          <span className={`profile-calorie-bar-number ${canStillEat ? 'is-good' : 'is-high'}`}>
                            {remaining == null ? '--' : Math.round(Math.abs(remaining))}
                          </span>
                          <div className="profile-calorie-bar-track">
                            {remaining != null && (
                              <div
                                className={`profile-calorie-bar ${canStillEat ? 'is-good' : 'is-high'}`}
                                style={{ height: `${barHeight}%` }}
                                title={canStillEat ? `还可吃 ${Math.round(remaining)} kcal` : `吃超了 ${Math.round(overAmount)} kcal`}
                              />
                            )}
                            {remaining == null && <div className="profile-calorie-bar-empty" />}
                          </div>
                          <span className="profile-calorie-bar-label">{point.label}</span>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <CalorieTrendCalendar
                    key={calorieTab}
                    points={calorieTrendData?.points || []}
                    period={calorieTab}
                    loading={calorieTrendLoading}
                    drillYear={calorieDrillMonth.year}
                    drillMonth={calorieDrillMonth.month}
                    onDrillDown={(y, m) => { setCalorieDrillMonth({ year: y, month: m }); handleCalorieTabChange('month'); }}
                  />
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 今日记录详情弹窗 / 底部面板 */}
      {!isMobileProfile && (
        !!recordDetailOpen && (
          <div className="profile-desktop-overlay" onClick={() => setRecordDetailOpen(null)}>
            <div className="profile-desktop-panel record-detail-panel" onClick={(e) => e.stopPropagation()}>
              <div className="record-detail-head">
                <span className="record-detail-head-title">
                  {recordDetailOpen === 'exercise' ? '运动记录详情' : '饮食记录详情'}
                </span>
                <div className="record-detail-head-actions">
                  <button
                    type="button"
                    className="record-detail-close"
                    onClick={() => setRecordDetailOpen(null)}
                    aria-label="关闭"
                  >
                    <CloseOutlined />
                  </button>
                </div>
              </div>
              <div className="record-detail-panel-body">
                {recordDetailOpen === 'exercise' && (() => {
                  const list = todayExerciseList;
                  if (todayExerciseLoading) {
                    return <div className="today-record-empty">加载中...</div>;
                  }
                  return list.length > 0 ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 12 }}>
                      {list.map((r, i) => (
                        <div key={i} className="record-detail-item">
                          <CheckCircleFilled style={{ color: '#52c41a', fontSize: 15, flexShrink: 0 }} />
                          <div style={{ flex: 1, minWidth: 0 }}>
                            <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text)' }}>{r.name || '训练记录'}</div>
                            <div style={{ fontSize: 12, color: 'var(--text-3)', marginTop: 2 }}>
                              {[r.durationSeconds ? formatDuration(r.durationSeconds) : '', formatCaloriesBurned(r.caloriesBurned)].filter(Boolean).join(' · ')}
                            </div>
                            {getExerciseSessionItems(r).length > 0 && (
                              <div style={{ fontSize: 12, color: 'var(--text-2)', marginTop: 6, display: 'flex', flexDirection: 'column', gap: 4 }}>
                                {getExerciseSessionItems(r).map((item, itemIndex) => (
                                  <div key={`${item.name || 'item'}-${itemIndex}`}>
                                    {[item.name || '训练动作', getMuscleGroupLabel(item.muscleGroup), item.completedSets != null ? `${item.completedSets} 组` : '', item.durationSeconds ? formatDuration(item.durationSeconds) : '']
                                      .filter(Boolean)
                                      .join(' · ')}
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>
                          <div className="record-detail-actions">
                            <button type="button" className="record-detail-edit" onClick={() => handleOpenExerciseEdit(r, i)}>编辑</button>
                            <button type="button" className="record-detail-delete" onClick={() => handleDeleteRecord('exercise', i)}>删除</button>
                          </div>
                        </div>
                      ))}
                    </div>
                ) : (
                  <div className="today-record-empty">
                    <button
                      type="button"
                      className="today-record-empty-circle"
                      onClick={handleOpenExerciseCreate}
                      aria-label="直接记录训练"
                    >
                      <PlusOutlined />
                    </button>
                  </div>
                );
              })()}
              {recordDetailOpen === 'diet' && (() => {
                const list = todayDietList;
                if (todayDietLoading) {
                  return <div className="today-record-empty">加载中...</div>;
                }
                return list.length > 0 ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 12 }}>
                      {list.map((r, i) => (
                        <div key={i} className="record-detail-item">
                          <div className="record-detail-food-media">
                            {getDietRecordImages(r).length > 0 ? (
                              <div className="record-detail-food-stack">
                                {getDietRecordImages(r).map((imageUrl, imageIndex) => (
                                  <img
                                    key={`${imageUrl}-${imageIndex}`}
                                    src={imageUrl}
                                    alt={r.name || '饮食'}
                                    className="record-detail-food-image"
                                  />
                                ))}
                              </div>
                            ) : (
                              <div className="record-detail-food-fallback">
                                <FormOutlined className="record-detail-food-icon" />
                              </div>
                            )}
                          </div>
                          <div className="record-detail-item-content">
                            <div className="record-detail-item-title">{r.name || '饮食'}</div>
                            <div className="record-detail-item-meta">
                              {[r.mealType, r.calories ? `${r.calories} kcal` : ''].filter(Boolean).join(' · ')}
                            </div>
                          </div>
                          <div className="record-detail-actions">
                            <button type="button" className="record-detail-edit" onClick={() => handleOpenDietEdit(r, i)}>编辑</button>
                            <button type="button" className="record-detail-delete" onClick={() => handleDeleteRecord('diet', i)}>删除</button>
                          </div>
                        </div>
                      ))}
                    </div>
                ) : (
                  <div className="today-record-empty">
                    <button
                      type="button"
                      className="today-record-empty-circle"
                      onClick={handleOpenDietCreate}
                      aria-label="直接记录饮食"
                    >
                      <PlusOutlined />
                    </button>
                  </div>
                );
              })()}
              </div>
              {(recordDetailOpen === 'exercise' && todayExerciseList.length > 0 || recordDetailOpen === 'diet' && todayDietList.length > 0) && (
                <div className="record-detail-footer">
                  <button type="button" className="profile-record-sheet-action" onClick={recordDetailOpen === 'exercise' ? handleOpenExerciseCreate : handleOpenDietCreate}>
                    <span className="record-footer-label">直接记录</span>
                  </button>
                  <button type="button" className="profile-record-sheet-action" onClick={() => history.push(recordDetailOpen === 'exercise' ? '/exercises?panel=training' : '/exercises?panel=diet')}>
                    <span className="record-footer-label">查看计划</span>
                  </button>
                </div>
              )}
            </div>
          </div>
        )
      )}

      {recordSheet.mounted && (
        <div className="profile-record-sheet" style={recordSheet.sheetStyle}>
          <div className="profile-record-sheet-handle" {...recordSheet.dragHandleProps} />
          <div className="profile-record-sheet-head" {...recordSheet.dragHandleProps}>
            <div className="profile-record-sheet-title">{recordDetailOpen === 'exercise' ? '运动记录详情' : '饮食记录详情'}</div>
          </div>
          <div className="profile-record-sheet-body">
            {recordDetailOpen === 'exercise' && (() => {
              const list = todayExerciseList;
              if (todayExerciseLoading) {
                return <div className="today-record-empty">加载中...</div>;
              }
              return list.length > 0 ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  {list.map((r, i) => (
                    <div key={i} className="record-detail-item">
                      <CheckCircleFilled style={{ color: '#52c41a', fontSize: 15, flexShrink: 0 }} />
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text)' }}>{r.name || '训练记录'}</div>
                        <div style={{ fontSize: 12, color: 'var(--text-3)', marginTop: 2 }}>
                          {[r.durationSeconds ? formatDuration(r.durationSeconds) : '', formatCaloriesBurned(r.caloriesBurned)].filter(Boolean).join(' · ')}
                        </div>
                        {getExerciseSessionItems(r).length > 0 && (
                          <div style={{ fontSize: 12, color: 'var(--text-2)', marginTop: 6, display: 'flex', flexDirection: 'column', gap: 4 }}>
                            {getExerciseSessionItems(r).map((item, itemIndex) => (
                              <div key={`${item.name || 'item'}-${itemIndex}`}>
                                {[item.name || '训练动作', getMuscleGroupLabel(item.muscleGroup), item.completedSets != null ? `${item.completedSets} 组` : '', item.durationSeconds ? formatDuration(item.durationSeconds) : '']
                                  .filter(Boolean)
                                  .join(' · ')}
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                      <div className="record-detail-actions">
                        <button type="button" className="record-detail-edit" onClick={() => handleOpenExerciseEdit(r, i)}>编辑</button>
                        <button type="button" className="record-detail-delete" onClick={() => handleDeleteRecord('exercise', i)}>删除</button>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="today-record-empty">
                  <button
                    type="button"
                    className="today-record-empty-circle"
                    onClick={handleOpenExerciseCreate}
                    aria-label="直接记录训练"
                  >
                    <PlusOutlined />
                  </button>
                </div>
              );
            })()}
            {recordDetailOpen === 'diet' && (() => {
              const list = todayDietList;
              if (todayDietLoading) {
                return <div className="today-record-empty">加载中...</div>;
              }
              return list.length > 0 ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  {list.map((r, i) => (
                    <div key={i} className="record-detail-item">
                      <div className="record-detail-food-media">
                        {getDietRecordImages(r).length > 0 ? (
                          <div className="record-detail-food-stack">
                            {getDietRecordImages(r).map((imageUrl, imageIndex) => (
                              <img
                                key={`${imageUrl}-${imageIndex}`}
                                src={imageUrl}
                                alt={r.name || '饮食'}
                                className="record-detail-food-image"
                              />
                            ))}
                          </div>
                        ) : (
                          <div className="record-detail-food-fallback">
                            <FormOutlined className="record-detail-food-icon" />
                          </div>
                        )}
                      </div>
                      <div className="record-detail-item-content">
                        <div className="record-detail-item-title">{r.name || '饮食'}</div>
                        <div className="record-detail-item-meta">
                          {[r.mealType, r.calories ? `${r.calories} kcal` : ''].filter(Boolean).join(' · ')}
                        </div>
                      </div>
                      <div className="record-detail-actions">
                        <button type="button" className="record-detail-edit" onClick={() => handleOpenDietEdit(r, i)}>编辑</button>
                        <button type="button" className="record-detail-delete" onClick={() => handleDeleteRecord('diet', i)}>删除</button>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="today-record-empty">
                  <button
                    type="button"
                    className="today-record-empty-circle"
                    onClick={handleOpenDietCreate}
                    aria-label="直接记录饮食"
                  >
                    <PlusOutlined />
                  </button>
                </div>
              );
            })()}
          </div>
          {(recordDetailOpen === 'exercise' && todayExerciseList.length > 0 || recordDetailOpen === 'diet' && todayDietList.length > 0) && (
            <div className="profile-record-sheet-footer">
              <button type="button" className="profile-record-sheet-action" onClick={recordDetailOpen === 'exercise' ? handleOpenExerciseCreate : handleOpenDietCreate}>
                <span className="record-footer-label">直接记录</span>
              </button>
              <button type="button" className="profile-record-sheet-action" onClick={() => history.push(recordDetailOpen === 'exercise' ? '/exercises?panel=training' : '/exercises?panel=diet')}>
                <span className="record-footer-label">查看计划</span>
              </button>
            </div>
          )}
        </div>
      )}

      {!isMobileProfile && editOpen && (
        <div className="profile-desktop-overlay" onClick={() => setEditOpen(false)}>
          <div className="profile-desktop-panel" onClick={(e) => e.stopPropagation()}>
            <div className="record-detail-head">
              <span className="record-detail-head-title">编辑个人信息</span>
              <div className="record-detail-head-actions">
                <button
                  type="button"
                  className="record-detail-close"
                  onClick={() => setEditOpen(false)}
                  aria-label="关闭"
                >
                  <CloseOutlined />
                </button>
              </div>
            </div>
            <div className="record-detail-panel-body">
              <Form form={editForm} layout="vertical" requiredMark={false}>
                <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }, { max: 20, message: '用户名不超过20个字符' }]}>
                  <Input placeholder="输入用户名" maxLength={20} />
                </Form.Item>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
                  <Form.Item name="fitnessGoal" label="健身目标" rules={[{ required: true }]}>
                    <Select placeholder="选择目标" options={fitnessGoals} />
                  </Form.Item>
                  <Form.Item name="age" label="年龄" rules={[{ required: true }, { type: 'number', min: 10, max: 100 }]}>
                    <InputNumber placeholder="22" min={10} max={100} addonAfter="岁" style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name="height" label="身高" rules={[{ required: true }, { type: 'number', min: 50, max: 250 }]}>
                    <InputNumber placeholder="175" min={50} max={250} step={0.1} addonAfter="cm" style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name="weight" label="体重" rules={[{ required: true }, { type: 'number', min: 20, max: 300 }]}>
                    <InputNumber placeholder="70" min={20} max={300} step={0.1} addonAfter="kg" style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name="targetWeight" label="目标体重">
                    <InputNumber placeholder="留空则不展示目标线" min={20} max={300} step={0.1} addonAfter="kg" style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name="city" label="所在城市">
                    <Input placeholder="如：广州" maxLength={64} />
                  </Form.Item>
                  <Form.Item name="cityEn" label="City (English)">
                    <Input placeholder="如：Guangzhou" maxLength={64} />
                  </Form.Item>
                  <Form.Item name="activityLevel" label="活动水平" rules={[{ required: true, message: '请选择活动水平' }]}>
                    <Select placeholder="选择活动水平" options={activityOptions.map(({ value, label, factor }) => ({ value, label: `${label} · x${factor}` }))} />
                  </Form.Item>
                  <Form.Item name="customDailyCalories" label="每日目标热量">
                    <InputNumber placeholder="留空则使用系统预估" min={0} max={10000} addonAfter="kcal" style={{ width: '100%' }} />
                  </Form.Item>
                </div>
                <Form.Item name="gender" label="性别" rules={[{ required: true }]}>
                  <GenderButtons />
                </Form.Item>
              </Form>
            </div>
            <div className="profile-desktop-panel-foot">
              <Button onClick={() => setEditOpen(false)}>取消</Button>
              <Button type="primary" loading={editLoading} onClick={handleEditSave}>
                保存
              </Button>
            </div>
          </div>
        </div>
      )}

      {editSheet.mounted && (
        <div className="profile-record-sheet" style={editSheet.sheetStyle}>
          <div className="profile-record-sheet-handle" {...editSheet.dragHandleProps} />
          <div className="profile-record-sheet-head" {...editSheet.dragHandleProps}>
            <div className="profile-record-sheet-title">编辑个人信息</div>
          </div>
          <div className="profile-record-sheet-body">
            <Form form={editForm} layout="vertical" requiredMark={false}>
              <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }, { max: 20, message: '用户名不超过20个字符' }]}>
                <Input placeholder="输入用户名" maxLength={20} />
              </Form.Item>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
                <Form.Item name="fitnessGoal" label="健身目标" rules={[{ required: true }]}>
                  <Select placeholder="选择目标" options={fitnessGoals} />
                </Form.Item>
                <Form.Item name="age" label="年龄" rules={[{ required: true }, { type: 'number', min: 10, max: 100 }]}>
                  <InputNumber placeholder="22" min={10} max={100} addonAfter="岁" style={{ width: '100%' }} />
                </Form.Item>
                <Form.Item name="height" label="身高" rules={[{ required: true }, { type: 'number', min: 50, max: 250 }]}>
                  <InputNumber placeholder="175" min={50} max={250} step={0.1} addonAfter="cm" style={{ width: '100%' }} />
                </Form.Item>
                <Form.Item name="weight" label="体重" rules={[{ required: true }, { type: 'number', min: 20, max: 300 }]}>
                  <InputNumber placeholder="70" min={20} max={300} step={0.1} addonAfter="kg" style={{ width: '100%' }} />
                </Form.Item>
                <Form.Item name="targetWeight" label="目标体重">
                  <InputNumber placeholder="留空则不展示目标线" min={20} max={300} step={0.1} addonAfter="kg" style={{ width: '100%' }} />
                </Form.Item>
                <Form.Item name="activityLevel" label="活动水平" rules={[{ required: true, message: '请选择活动水平' }]}>
                  <Select placeholder="选择活动水平" options={activityOptions.map(({ value, label, factor }) => ({ value, label: `${label} · x${factor}` }))} />
                </Form.Item>
                <Form.Item name="customDailyCalories" label="每日目标热量">
                  <InputNumber placeholder="留空则使用系统预估" min={0} max={10000} addonAfter="kcal" style={{ width: '100%' }} />
                </Form.Item>
              </div>
              <Form.Item name="gender" label="性别" rules={[{ required: true }]}>
                <GenderButtons />
              </Form.Item>
            </Form>
            <div style={{ padding: '12px 0', display: 'flex', gap: 12 }}>
              <Button block onClick={() => editSheet.requestClose()}>取消</Button>
              <Button block type="primary" loading={editLoading} onClick={() => { void handleEditSave(); }}>
                保存
              </Button>
            </div>
          </div>
        </div>
      )}

      {!isMobileProfile && (
        exerciseEditOpen && (
          <div className="profile-desktop-overlay" onClick={closeExerciseEdit}>
            <div className="profile-desktop-panel profile-desktop-panel--wide" onClick={(e) => e.stopPropagation()}>
              <div className="record-detail-head">
                <span className="record-detail-head-title">编辑训练记录</span>
                <div className="record-detail-head-actions">
                  <button type="button" className="record-detail-close" onClick={closeExerciseEdit} aria-label="关闭">
                    <CloseOutlined />
                  </button>
                </div>
              </div>
              <div className="record-detail-panel-body profile-exercise-edit-panel-body">
                <div className="profile-exercise-edit-form">
                  <div className="profile-exercise-edit-grid" style={{ marginBottom: 12 }}>
                    <div>
                      <label className="profile-diet-edit-label">总训练时长</label>
                      <InputNumber value={exerciseEditDurationSeconds != null ? Math.round(exerciseEditDurationSeconds / 60) : undefined} onChange={(value) => setExerciseEditDurationSeconds(typeof value === 'number' ? value * 60 : null)} min={0} addonAfter="分钟" placeholder="可不填" style={{ width: '100%' }} />
                    </div>
                    <div>
                      <label className="profile-diet-edit-label">总消耗卡路里</label>
                      <InputNumber value={exerciseEditCaloriesBurned ?? undefined} onChange={(value) => setExerciseEditCaloriesBurned(typeof value === 'number' ? value : null)} min={0} addonAfter="kcal" placeholder="可不填" style={{ width: '100%' }} />
                    </div>
                  </div>
                  <div className="profile-exercise-edit-list">
                    {exerciseEditItems.map((item, index) => (
                      <div key={`exercise-edit-${index}`} className="profile-exercise-edit-item">
                        <div className="profile-exercise-edit-grid">
                          <div>
                            <label className="profile-diet-edit-label">动作</label>
                            <Select
                              showSearch
                              value={item.name || undefined}
                              onSearch={(val) => { if (val) fetchExerciseLib(); }}
                              onChange={(val: string) => {
                                const ex = exerciseLib.find((e) => e.name === val);
                                handleExerciseItemChange(index, {
                                  name: val,
                                  exerciseId: ex?.id,
                                  muscleGroup: ex?.muscleGroup || item.muscleGroup,
                                });
                              }}
                              filterOption={false}
                              loading={exerciseLibLoading}
                              placeholder="搜索动作"
                              style={{ width: '100%' }}
                              options={exerciseLib.map((e) => ({ value: e.name, label: e.name }))}
                            />
                          </div>
                          <div>
                            <label className="profile-diet-edit-label">训练部位</label>
                            <Select
                              value={item.muscleGroup}
                              onChange={(value) => handleExerciseItemChange(index, { muscleGroup: value })}
                              allowClear
                              options={muscleGroupOptions}
                              placeholder="选择部位"
                              style={{ width: '100%' }}
                            />
                          </div>
                          <div>
                            <label className="profile-diet-edit-label">完成组数</label>
                            <InputNumber value={item.completedSets ?? undefined} onChange={(value) => handleExerciseItemChange(index, { completedSets: typeof value === 'number' ? value : undefined })} min={0} style={{ width: '100%' }} />
                          </div>
                          <div>
                            <label className="profile-diet-edit-label">该动作时长（分钟）</label>
                            <InputNumber value={item.durationSeconds != null ? Math.round(item.durationSeconds / 60) : undefined} onChange={(value) => handleExerciseItemChange(index, { durationSeconds: typeof value === 'number' ? value * 60 : undefined })} min={0} placeholder="可不填" style={{ width: '100%' }} />
                          </div>
                        </div>
                        <div className="profile-exercise-edit-item-foot">
                          <Button type="text" danger onClick={() => handleRemoveExerciseItem(index)}>删除</Button>
                        </div>
                      </div>
                    ))}
                    {exerciseEditItems.length === 0 && <div className="profile-diet-edit-empty">还没添加训练动作</div>}
                  </div>
                  <Button className="profile-exercise-edit-add" onClick={handleAddExerciseItem}>新增动作</Button>
                </div>
              </div>
              <div className="profile-desktop-panel-foot">
                <Button onClick={closeExerciseEdit}>取消</Button>
                <Button type="primary" loading={exerciseEditSaving} onClick={handleSaveExerciseEdit}>
                  保存
                </Button>
              </div>
            </div>
          </div>
        )
      )}

      {!isMobileProfile && (
        dietEditOpen && (
          <div className="profile-desktop-overlay" onClick={closeDietEdit}>
            <div className="profile-desktop-panel profile-desktop-panel--wide" onClick={(e) => e.stopPropagation()}>
              <div className="record-detail-head">
                <span className="record-detail-head-title">编辑饮食记录</span>
                <div className="record-detail-head-actions">
                  <button
                    type="button"
                    className="record-detail-close"
                    onClick={closeDietEdit}
                    aria-label="关闭"
                  >
                    <CloseOutlined />
                  </button>
                </div>
              </div>
              <div className="record-detail-panel-body profile-diet-edit-panel-body">
                <div className="profile-diet-edit-form">
                  <label className="profile-diet-edit-label">餐次</label>
                  <Select
                    value={dietEditMealType}
                    onChange={setDietEditMealType}
                    options={MEAL_OPTIONS.map((meal) => ({ value: meal, label: meal }))}
                    style={{ width: '100%', marginBottom: 16 }}
                  />
                  <label className="profile-diet-edit-label">搜索食物</label>
                  <Input
                    value={dietEditKeyword}
                    onChange={(e) => setDietEditKeyword(e.target.value)}
                    placeholder="输入食物名搜索"
                    maxLength={60}
                  />
                  <div className="profile-diet-edit-search-results">
                    {dietEditLoading ? <div className="profile-diet-edit-empty">搜索中...</div> : dietEditOptions.map((food) => (
                      <button type="button" key={food.id} className="profile-diet-edit-option" onClick={() => handleAddEditFood(food)}>
                        <div className="profile-diet-edit-option-main">
                          {food.imageUrl ? <img src={food.imageUrl} alt={food.name} className="profile-diet-edit-thumb" /> : <div className="profile-diet-edit-thumb profile-diet-edit-thumb--placeholder">{food.name?.slice(0, 1) || '食'}</div>}
                          <div>
                            <div className="profile-diet-edit-option-name">{food.name}</div>
                            <div className="profile-diet-edit-option-meta">
                              {food.category || '未分类'} · 每 {formatFoodBase(food.baseAmount, food.unit)} · {(food.calories || 0).toFixed(0)} kJ ≈ {kjToKcal(food.calories).toFixed(0)} kcal
                            </div>
                          </div>
                        </div>
                        <span className="profile-diet-edit-option-action">添加</span>
                      </button>
                    ))}
                    {!dietEditLoading && dietEditOptions.length === 0 && (
                      <div className="profile-diet-edit-empty">没搜到食物，先换个关键词试试</div>
                    )}
                  </div>
                  <label className="profile-diet-edit-label">已选食物</label>
                  <div className="profile-diet-edit-selected-list">
                    {dietEditFoods.map((item) => (
                      <div key={item.food.id} className="profile-diet-edit-selected-item">
                        <div className="profile-diet-edit-selected-main">
                          <div className="profile-diet-edit-selected-name">{item.food.name}</div>
                          <div className="profile-diet-edit-selected-meta">
                            每 {formatFoodBase(item.food.baseAmount, item.food.unit)}: {(item.food.calories || 0).toFixed(0)} kJ ≈ {kjToKcal(item.food.calories).toFixed(0)} kcal / 蛋白 {(item.food.protein || 0).toFixed(1)}g
                          </div>
                        </div>
                        <InputNumber
                          value={item.amount}
                          onChange={(value) => handleEditFoodAmountChange(item.food.id, typeof value === 'number' ? value : null)}
                          min={1}
                          addonAfter={item.food.unit || 'g'}
                          style={{ width: 132 }}
                        />
                        <Button type="text" danger onClick={() => handleRemoveEditFood(item.food.id)}>删除</Button>
                      </div>
                    ))}
                    {dietEditFoods.length === 0 && <div className="profile-diet-edit-empty">还没添加食物</div>}
                  </div>
                  {renderDietNutritionPreview(dietEditPreview)}
                  <label className="profile-diet-edit-label">备注（可选）</label>
                  <Input.TextArea
                    value={dietEditNote}
                    onChange={(e) => setDietEditNote(e.target.value)}
                    rows={3}
                    placeholder="比如：训练后补充、少糖、七分饱"
                    maxLength={120}
                    showCount
                    styles={{ textarea: { position: 'relative' }, count: { position: 'absolute', bottom: 6, right: 10, color: 'var(--text-3)', fontSize: 12 } }}
                  />
                </div>
              </div>
              <div className="profile-desktop-panel-foot">
                <Button onClick={closeDietEdit}>取消</Button>
                <Button type="primary" loading={dietEditSaving} onClick={handleSaveDietEdit}>
                  保存
                </Button>
              </div>
            </div>
          </div>
        )
      )}

      {!isMobileProfile && pendingDelete && (
        <div className="profile-desktop-overlay" onClick={() => setPendingDelete(null)}>
          <div className="profile-desktop-panel profile-desktop-panel--confirm" onClick={(e) => e.stopPropagation()}>
            <div className="record-detail-head">
              <span className="record-detail-head-title">删除{pendingDelete.label}</span>
              <div className="record-detail-head-actions">
                <button
                  type="button"
                  className="record-detail-close"
                  onClick={() => setPendingDelete(null)}
                  aria-label="关闭"
                >
                  <CloseOutlined />
                </button>
              </div>
            </div>
            <div className="profile-delete-confirm-text">确定删除这条{pendingDelete.label}吗？</div>
            <div className="profile-desktop-panel-foot">
              <Button onClick={() => setPendingDelete(null)}>取消</Button>
              <Button danger type="primary" onClick={confirmDeleteRecord}>删除</Button>
            </div>
          </div>
        </div>
      )}

      {dietEditSheet.mounted && (
        <div className="profile-record-sheet" style={dietEditSheet.sheetStyle}>
          <div className="profile-record-sheet-handle" {...dietEditSheet.dragHandleProps} />
          <div className="profile-record-sheet-head" {...dietEditSheet.dragHandleProps}>
            <div className="profile-record-sheet-title">编辑饮食记录</div>
            <button
              type="button"
              className="profile-record-sheet-action"
              onClick={closeDietEdit}
            >
              返回
            </button>
          </div>
          <div className="profile-record-sheet-body">
            <div className="profile-diet-edit-form">
              <label className="profile-diet-edit-label">餐次</label>
              <Select
                value={dietEditMealType}
                onChange={setDietEditMealType}
                options={MEAL_OPTIONS.map((meal) => ({ value: meal, label: meal }))}
                style={{ width: '100%', marginBottom: 16 }}
              />
              <label className="profile-diet-edit-label">搜索食物</label>
              <Input
                value={dietEditKeyword}
                onChange={(e) => setDietEditKeyword(e.target.value)}
                placeholder="输入食物名搜索"
                maxLength={60}
              />
              <div className="profile-diet-edit-search-results">
                {dietEditLoading ? <div className="profile-diet-edit-empty">搜索中...</div> : dietEditOptions.map((food) => (
                  <button type="button" key={food.id} className="profile-diet-edit-option" onClick={() => handleAddEditFood(food)}>
                    <div className="profile-diet-edit-option-main">
                      {food.imageUrl ? <img src={food.imageUrl} alt={food.name} className="profile-diet-edit-thumb" /> : <div className="profile-diet-edit-thumb profile-diet-edit-thumb--placeholder">{food.name?.slice(0, 1) || '食'}</div>}
                      <div>
                        <div className="profile-diet-edit-option-name">{food.name}</div>
                        <div className="profile-diet-edit-option-meta">
                          {food.category || '未分类'} · 每 {formatFoodBase(food.baseAmount, food.unit)} · {(food.calories || 0).toFixed(0)} kJ ≈ {kjToKcal(food.calories).toFixed(0)} kcal
                        </div>
                      </div>
                    </div>
                    <span className="profile-diet-edit-option-action">添加</span>
                  </button>
                ))}
                {!dietEditLoading && dietEditOptions.length === 0 && (
                  <div className="profile-diet-edit-empty">没搜到食物，先换个关键词试试</div>
                )}
              </div>
              <label className="profile-diet-edit-label">已选食物</label>
              <div className="profile-diet-edit-selected-list">
                {dietEditFoods.map((item) => (
                  <div key={item.food.id} className="profile-diet-edit-selected-item">
                    <div className="profile-diet-edit-selected-main">
                      <div className="profile-diet-edit-selected-name">{item.food.name}</div>
                      <div className="profile-diet-edit-selected-meta">
                        每 {formatFoodBase(item.food.baseAmount, item.food.unit)}: {(item.food.calories || 0).toFixed(0)} kJ ≈ {kjToKcal(item.food.calories).toFixed(0)} kcal / 蛋白 {(item.food.protein || 0).toFixed(1)}g
                      </div>
                    </div>
                    <InputNumber
                      value={item.amount}
                      onChange={(value) => handleEditFoodAmountChange(item.food.id, typeof value === 'number' ? value : null)}
                      min={1}
                      addonAfter={item.food.unit || 'g'}
                      style={{ width: 132 }}
                    />
                    <Button type="text" danger onClick={() => handleRemoveEditFood(item.food.id)}>删除</Button>
                  </div>
                ))}
                {dietEditFoods.length === 0 && <div className="profile-diet-edit-empty">还没添加食物</div>}
              </div>
              {renderDietNutritionPreview(dietEditPreview)}
              <label className="profile-diet-edit-label">备注（可选）</label>
              <Input.TextArea
                value={dietEditNote}
                onChange={(e) => setDietEditNote(e.target.value)}
                rows={4}
                placeholder="比如：训练后补充、少糖、七分饱"
                maxLength={120}
                showCount
                styles={{ textarea: { position: 'relative' }, count: { position: 'absolute', bottom: 6, right: 10, color: 'var(--text-3)', fontSize: 12 } }}
              />
            </div>
          </div>
          <div className="profile-ai-sheet-foot">
            <Button onClick={closeDietEdit}>取消</Button>
            <Button type="primary" loading={dietEditSaving} onClick={handleSaveDietEdit}>
              保存
            </Button>
          </div>
        </div>
      )}

      {exerciseEditSheet.mounted && (
        <div className="profile-record-sheet" style={exerciseEditSheet.sheetStyle}>
          <div className="profile-record-sheet-handle" {...exerciseEditSheet.dragHandleProps} />
          <div className="profile-record-sheet-head" {...exerciseEditSheet.dragHandleProps}>
            <div className="profile-record-sheet-title">编辑训练记录</div>
            <button type="button" className="profile-record-sheet-action" onClick={closeExerciseEdit}>
              返回
            </button>
          </div>
          <div className="profile-record-sheet-body">
            <div className="profile-exercise-edit-form">
              <div className="profile-exercise-edit-grid" style={{ marginBottom: 12 }}>
                <div>
                  <label className="profile-diet-edit-label">训练时长</label>
                  <InputNumber value={exerciseEditDurationSeconds != null ? Math.round(exerciseEditDurationSeconds / 60) : undefined} onChange={(value) => setExerciseEditDurationSeconds(typeof value === 'number' ? value * 60 : null)} min={0} addonAfter="分钟" placeholder="可不填" style={{ width: '100%' }} />
                </div>
                <div>
                  <label className="profile-diet-edit-label">消耗卡路里</label>
                  <InputNumber value={exerciseEditCaloriesBurned ?? undefined} onChange={(value) => setExerciseEditCaloriesBurned(typeof value === 'number' ? value : null)} min={0} addonAfter="kcal" placeholder="可不填" style={{ width: '100%' }} />
                </div>
              </div>
              <div className="profile-exercise-edit-list">
                {exerciseEditItems.map((item, index) => (
                  <div key={`exercise-mobile-${index}`} className="profile-exercise-edit-item">
                    <div className="profile-exercise-edit-grid">
                      <div>
                        <label className="profile-diet-edit-label">动作</label>
                        <Select
                          showSearch
                          value={item.name || undefined}
                          onSearch={(val) => { if (val) fetchExerciseLib(); }}
                          onChange={(val: string) => {
                            const ex = exerciseLib.find((e) => e.name === val);
                            handleExerciseItemChange(index, {
                              name: val,
                              exerciseId: ex?.id,
                              muscleGroup: ex?.muscleGroup || item.muscleGroup,
                            });
                          }}
                          filterOption={false}
                          loading={exerciseLibLoading}
                          placeholder="搜索动作"
                          style={{ width: '100%' }}
                          options={exerciseLib.map((e) => ({ value: e.name, label: e.name }))}
                        />
                      </div>
                      <div>
                        <label className="profile-diet-edit-label">训练部位</label>
                        <Select
                          value={item.muscleGroup}
                          onChange={(value) => handleExerciseItemChange(index, { muscleGroup: value })}
                          allowClear
                          options={muscleGroupOptions}
                          placeholder="选择部位"
                          style={{ width: '100%' }}
                        />
                      </div>
                      <div>
                        <label className="profile-diet-edit-label">完成组数</label>
                        <InputNumber value={item.completedSets ?? undefined} onChange={(value) => handleExerciseItemChange(index, { completedSets: typeof value === 'number' ? value : undefined })} min={0} style={{ width: '100%' }} />
                      </div>
                      <div>
                        <label className="profile-diet-edit-label">时长（分钟）</label>
                        <InputNumber value={item.durationSeconds != null ? Math.round(item.durationSeconds / 60) : undefined} onChange={(value) => handleExerciseItemChange(index, { durationSeconds: typeof value === 'number' ? value * 60 : undefined })} min={0} placeholder="可不填" style={{ width: '100%' }} />
                      </div>
                      <div>
                        <label className="profile-diet-edit-label">卡路里（kcal）</label>
                        <InputNumber value={item.caloriesBurned ?? undefined} onChange={(value) => handleExerciseItemChange(index, { caloriesBurned: typeof value === 'number' ? value : undefined })} min={0} placeholder="可不填" style={{ width: '100%' }} />
                      </div>
                    </div>
                    <div className="profile-exercise-edit-item-foot">
                      <Button type="text" danger onClick={() => handleRemoveExerciseItem(index)}>删除</Button>
                    </div>
                  </div>
                ))}
                {exerciseEditItems.length === 0 && <div className="profile-diet-edit-empty">还没添加训练动作</div>}
              </div>
              <Button className="profile-exercise-edit-add" onClick={handleAddExerciseItem}>新增动作</Button>
              <div className="profile-record-sheet-footer-actions">
                <Button onClick={closeExerciseEdit}>取消</Button>
                <Button type="primary" loading={exerciseEditSaving} onClick={handleSaveExerciseEdit}>
                  保存
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {!isMobileProfile && profileOpen && (
        <div className="profile-desktop-overlay" onClick={() => setProfileOpen(false)}>
          <div className="profile-desktop-panel" onClick={(e) => e.stopPropagation()}>
            <div className="record-detail-head">
              <span className="record-detail-head-title">编辑 AI 画像</span>
              <div className="record-detail-head-actions">
                <button
                  type="button"
                  className="record-detail-close"
                  onClick={() => setProfileOpen(false)}
                  aria-label="关闭"
                >
                  <CloseOutlined />
                </button>
              </div>
            </div>
            <div className="record-detail-panel-body">
              <Form form={profileForm} layout="vertical" requiredMark={false}>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
                  <Form.Item name="experienceLevel" label="训练水平" rules={[{ required: true, message: '请选择训练水平' }]}>
                    <Select placeholder="选择" options={experienceOptions} />
                  </Form.Item>
                  <Form.Item name="occupation" label="职业">
                    <Input placeholder="如：程序员、学生" maxLength={20} />
                  </Form.Item>
                  <Form.Item name="weeklyTrainingDays" label="每周训练天数" rules={[{ required: true, message: '请选择' }]}>
                    <Select placeholder="选择" options={trainingDaysOptions} />
                  </Form.Item>
                  <Form.Item name="trainingDuration" label="每次训练时长">
                    <Select placeholder="选择" options={durationOptions} allowClear />
                  </Form.Item>
                </div>
                <Form.Item name="preferredEquipment" label="可用器械" rules={[{ required: true, message: '请选择可用器械' }]}>
                  <Select mode="multiple" placeholder="选择你有的器械" options={equipmentOptions} />
                </Form.Item>
                <Form.Item name="personality" label="性格特征">
                  <Select placeholder="选择最接近的" options={personalityOptions} allowClear />
                </Form.Item>
                <Form.Item name="medicalHistory" label="伤病 / 疾病史">
                  <Input.TextArea rows={2} placeholder="如：左肩有旧伤、腰椎间盘突出" maxLength={200} showCount />
                </Form.Item>
                <Form.Item name="dietPreference" label="饮食偏好 / 忌口">
                  <Input.TextArea rows={2} placeholder="如：不吃海鲜、乳糖不耐受、素食" maxLength={200} showCount />
                </Form.Item>
                <Form.Item name="additionalNotes" label="补充信息">
                  <Input.TextArea rows={2} placeholder="其他想让 AI 了解的信息" maxLength={300} showCount />
                </Form.Item>
              </Form>
            </div>
            <div className="profile-desktop-panel-foot">
              <Button onClick={() => setProfileOpen(false)}>取消</Button>
              <Button type="primary" loading={profileLoading} onClick={handleProfileSave}>
                {profileLoading ? 'AI 生成中...' : '保存'}
              </Button>
            </div>
          </div>
        </div>
      )}

      {profileSheet.mounted && (
        <div className="profile-ai-sheet" style={profileSheet.sheetStyle}>
          <div className="profile-ai-sheet-handle" {...profileSheet.dragHandleProps} />
          <div className="profile-ai-sheet-head" {...profileSheet.dragHandleProps}>
            <div className="profile-ai-sheet-title">编辑 AI 画像</div>
          </div>
          <div className="profile-ai-sheet-body">
            <Form form={profileForm} layout="vertical" requiredMark={false}>
              <div className="profile-ai-sheet-grid">
                <Form.Item name="experienceLevel" label="训练水平" rules={[{ required: true, message: '请选择训练水平' }]}>
                  <Select placeholder="选择" options={experienceOptions} />
                </Form.Item>
                <Form.Item name="occupation" label="职业">
                  <Input placeholder="如：程序员、学生" maxLength={20} />
                </Form.Item>
                <Form.Item name="weeklyTrainingDays" label="每周训练天数" rules={[{ required: true, message: '请选择' }]}>
                  <Select placeholder="选择" options={trainingDaysOptions} />
                </Form.Item>
                <Form.Item name="trainingDuration" label="每次训练时长">
                  <Select placeholder="选择" options={durationOptions} allowClear />
                </Form.Item>
              </div>
              <Form.Item name="preferredEquipment" label="可用器械" rules={[{ required: true, message: '请选择可用器械' }]}>
                <Select mode="multiple" placeholder="选择你有的器械" options={equipmentOptions} />
              </Form.Item>
              <Form.Item name="personality" label="性格特征">
                <Select placeholder="选择最接近的" options={personalityOptions} allowClear />
              </Form.Item>
              <Form.Item name="medicalHistory" label="伤病 / 疾病史">
                <Input.TextArea rows={2} placeholder="如：左肩有旧伤、腰椎间盘突出" maxLength={200} showCount />
              </Form.Item>
              <Form.Item name="dietPreference" label="饮食偏好 / 忌口">
                <Input.TextArea rows={2} placeholder="如：不吃海鲜、乳糖不耐受、素食" maxLength={200} showCount />
              </Form.Item>
              <Form.Item name="additionalNotes" label="补充信息">
                <Input.TextArea rows={2} placeholder="其他想让 AI 了解的信息" maxLength={300} showCount />
              </Form.Item>
            </Form>
          </div>
          <div className="profile-ai-sheet-foot">
            <Button onClick={() => profileSheet.requestClose()}>取消</Button>
            <Button type="primary" loading={profileLoading} onClick={handleProfileSave}>
              {profileLoading ? 'AI 生成中...' : '保存'}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};

export default Profile;
