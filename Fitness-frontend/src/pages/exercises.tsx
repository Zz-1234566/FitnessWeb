import { CalendarOutlined, CameraOutlined, FireOutlined, HeartFilled, HeartOutlined, PlusOutlined, ReadOutlined, ReloadOutlined, RightOutlined, SettingOutlined, UploadOutlined } from '@ant-design/icons';
import { useModel, useSearchParams, history } from '@umijs/max';
import { AutoComplete, Button, Checkbox, Input, InputNumber, Modal, Select, Spin, Upload, message } from 'antd';
import Model, { type IExerciseData, type Muscle } from 'react-body-highlighter';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import ExerciseCard from '@/components/ExerciseCard';
import ExerciseExplorerFilters, { type ExerciseExplorerFilterRow } from '@/components/ExerciseExplorerFilters';
import TrainingTimer from '@/components/TrainingTimer';
import {
  MUSCLE_GROUP_COLORS,
  MUSCLE_GROUP_FILTERS,
  MUSCLE_GROUP_LABELS,
  DIFFICULTY_OPTIONS,
  getEquipmentOptions,
  parseEquipment,
  type MuscleGroup,
  normalizeExerciseDifficulty,
  normalizeExerciseMuscleGroup,
} from '@/constants/exercise';
import useEnsureCurrentUser from '@/hooks/useEnsureCurrentUser';
import { useExerciseInteractions } from '@/hooks/useExerciseInteractions';
import {
  activateDietCycle,
  activateTrainingCycle,
  addDietRecord,
  deleteDietCycle,
  deleteDietDayTemplate,
  deleteDietTemplate,
  deleteTrainingCycle,
  deleteTrainingTemplate,
  getActiveDietCycle,
  getActiveTrainingCycle,
  addStructuredExerciseRecord,
  getExerciseList,
  getTodayDietRecords,
  getExercisesByGroup,
  listDietCycles,
  listDietDayTemplates,
  listDietTemplates,
  listMyFoods,
  listTrainingCycles,
  listTrainingTemplates,
  recognizeFoodImage,
  saveDietCycle,
  saveDietDayTemplate,
  saveDietTemplate,
  saveFoodItem,
  saveTrainingCycle,
  saveTrainingTemplate,
  searchFoods,
  uploadFoodImage,
} from '@/services/ant-design-pro/api';
import { getGreeting } from '@/utils/greeting';
import useBottomSheetGesture from '@/hooks/useBottomSheetGesture';
import './exercises.less';

const getExerciseBatchSize = () => {
  if (typeof window === 'undefined') return 8;
  if (window.innerWidth <= 480) return 4;
  if (window.innerWidth <= 768) return 6;
  if (window.innerWidth <= 1100) return 8;
  return 10;
};

const WEEKDAY_NAMES = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
const WEEKDAY_FULL = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
const todayDayIndex = new Date().getDay();
const todayFullName = WEEKDAY_FULL[todayDayIndex];

type MealType = '早餐' | '午餐' | '加餐' | '晚餐' | '练后餐';

const MEAL_OPTIONS: MealType[] = ['早餐', '练后餐', '午餐', '加餐', '晚餐'];
const FOOD_CATEGORY_OPTIONS = ['主食', '蛋白质', '蔬菜', '水果', '乳制品', '坚果', '饮品', '补剂', '即食']
  .map((value) => ({ value }));

const getMealByTime = (): MealType => {
  const h = new Date().getHours();
  if (h < 11) return '早餐';
  if (h < 13) return '练后餐';
  if (h < 15) return '午餐';
  if (h < 17) return '加餐';
  return '晚餐';
};

const getNowTime = () =>
  new Date().toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });

const kjToKcal = (value?: number) => (value || 0) / 4.184;
const formatFoodBase = (baseAmount?: number, unit?: string) => `${baseAmount || 100} ${unit || 'g'}`;
const getRequestErrorMessage = (error: any, fallback: string) =>
  error?.info?.message || error?.data?.message || error?.message || fallback;
const RECENT_DIET_FOODS_STORAGE_KEY = 'recent_diet_foods';

type SelectedFoodItem = {
  food: API.FoodItem;
  amount: number;
};

type DietHistoryRecord = {
  time?: string;
  name?: string;
  mealType?: string;
  calories?: number;
  note?: string;
  source?: string;
  items?: Array<{
    foodItemId?: number;
    foodName?: string;
    name?: string;
    amount?: number;
    unit?: string;
    imageUrl?: string;
  }>;
};

type PlanTrainingDraft = {
  sectionType: 'warmup' | 'main' | 'stretch';
  sortOrder: number;
  exerciseId?: number;
  note: string;
};

type PlanDietDraft = {
  sortOrder: number;
  foodItemId?: number;
  amount: number;
  unit: string;
  note: string;
};

type PlanDraftState = {
  id?: number;
  name: string;
  mealType?: MealType;
  trainingItems: PlanTrainingDraft[];
  dietItems: PlanDietDraft[];
};

type PlanScheduleDraft = {
  id?: number;
  name: string;
  dayCount: number;
  startDate: string;
  items: {
    dayIndex: number;
    mealType?: MealType;
    planId?: number;
    templateId?: number;
    dayTemplateId?: number;
  }[];
};

type FoodEditorState = {
  id?: number;
  name: string;
  imageUrl: string;
  category: string;
  unit: string;
  baseAmount: number;
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
  fiber: number;
};

const createDefaultFoodEditor = (): FoodEditorState => ({
  name: '',
  imageUrl: '',
  category: '',
  unit: 'g',
  baseAmount: 100,
  calories: 0,
  protein: 0,
  carbs: 0,
  fat: 0,
  fiber: 0,
});

const createDefaultPlanDraft = (): PlanDraftState => ({
  id: undefined,
  name: '我的计划',
  mealType: '早餐',
  trainingItems: [],
  dietItems: [],
});

const buildPlanDraftFromTemplate = (
  template: API.UserTrainingTemplate | API.UserDietTemplate | undefined,
  mode: 'training' | 'diet',
): PlanDraftState => {
  const items = template?.items || [];
  return {
    id: template?.id,
    name: template?.name || '我的计划',
    mealType: ((template as API.UserDietTemplate)?.mealType as MealType) || '早餐',
    trainingItems: mode === 'training'
      ? items.map((item: any, index: number) => ({
        sectionType: (item.sectionType as PlanTrainingDraft['sectionType']) || 'main',
        sortOrder: item.sortOrder ?? index,
        exerciseId: item.exerciseId,
        note: item.note || '',
      }))
      : [],
    dietItems: mode === 'diet'
      ? items.map((item: any, index: number) => ({
        sortOrder: item.sortOrder ?? index,
        foodItemId: item.foodItemId,
        amount: item.amount || item.baseAmount || 100,
        unit: item.unit || 'g',
        note: item.note || '',
      }))
      : [],
  };
};

const createDefaultPlanScheduleDraft = (): PlanScheduleDraft => ({
  name: '',
  dayCount: 7,
  startDate: new Date().toLocaleDateString('en-CA', { timeZone: 'Asia/Shanghai' }),
  items: Array.from({ length: 7 }, (_, index) => ({ dayIndex: index + 1 })),
});

const PLAN_SECTION_OPTIONS: { value: PlanTrainingDraft['sectionType']; label: string }[] = [
  { value: 'warmup', label: '热身' },
  { value: 'main', label: '正式训练' },
  { value: 'stretch', label: '拉伸' },
];

const PLAN_SECTION_LABELS: Record<PlanTrainingDraft['sectionType'], string> = {
  warmup: '热身',
  main: '正式训练',
  stretch: '拉伸',
};

const generateNextName = (
  existingNames: string[],
  prefix: string,
  aiPrefix = '',
): string => {
  const numSet = new Set<number>();
  existingNames.forEach((n) => {
    const fullPrefix = aiPrefix ? `${aiPrefix}${prefix}` : prefix;
    if (n.startsWith(fullPrefix)) {
      const tail = n.slice(fullPrefix.length);
      const num = parseInt(tail, 10);
      if (num > 0 && String(num) === tail) numSet.add(num);
    }
  });
  let next = 1;
  while (numSet.has(next)) next++;
  return `${aiPrefix}${prefix}${next}`;
};

/* ===== 肌肉映射 ===== */
const muscleToGroupId: Record<string, string> = {
  'chest': 'chest', 'upper-back': 'back', 'lower-back': 'back', 'trapezius': 'back',
  'front-deltoids': 'shoulders', 'back-deltoids': 'shoulders',
  'biceps': 'arms', 'triceps': 'arms', 'forearm': 'arms',
  'quadriceps': 'legs', 'hamstring': 'legs', 'calves': 'legs', 'gluteal': 'legs',
  'abs': 'core', 'obliques': 'core',
};

const getGroupMuscles = (groupId: string): Muscle[] =>
  Object.keys(muscleToGroupId).filter(m => muscleToGroupId[m] === groupId) as Muscle[];

const HL_COLORS = ['rgba(255, 80, 80, 0.8)', 'rgba(255, 80, 80, 0.8)'];

/* ===== 推荐动作 ===== */
const daySeed = () => { const d = new Date(); return d.getFullYear() * 10000 + (d.getMonth() + 1) * 100 + d.getDate(); };
const seededIndex = (seed: number, len: number) => ((seed * 9301 + 49297) % 233280) % len;
const DIFF_LABELS: Record<string, string> = { easy: '简单', medium: '中等', hard: '困难', 简单: '简单', 中等: '中等', 困难: '困难' };

const calcUserDay = (createTime?: string | number | Date) => {
  const tz = { timeZone: 'Asia/Shanghai' };
  const c = new Date(createTime || Date.now()).toLocaleDateString('en-CA', tz);
  const n = new Date().toLocaleDateString('en-CA', tz);
  return Math.max(1, Math.round((new Date(n).getTime() - new Date(c).getTime()) / 86400000) + 1);
};

const buildExerciseDetailUrl = (exercise: API.Exercise) => {
  const params = new URLSearchParams();
  if (exercise.muscleGroup) params.set('group', exercise.muscleGroup);
  if (exercise.name) params.set('keyword', exercise.name);
  return `/exercises?${params.toString()}`;
};

const EXERCISE_GREET_LINES = [
  '先挑一个顺手的动作开始，状态会慢慢上来。',
  '今天练一点就够了，关键是把节奏接上。',
  '先从最想练的肌群下手，进入状态会更快。',
  '动作不用一次看太多，先把今天这一组练扎实。',
  '你现在需要的不是完美安排，是先动起来。',
  '从这里开始挑动作，今天的训练就算正式开场了。',
];

const Exercises: React.FC = () => {
  useEnsureCurrentUser();
  const { initialState } = useModel('@@initialState');
  const currentUser = initialState?.currentUser;
  const staticHeroMessage = useMemo(() => ({
    title: `${getGreeting()}，${currentUser?.username?.trim() || '朋友'}`,
    sub: EXERCISE_GREET_LINES[0],
  }), [currentUser?.username]);
  const { favoriteIds, videoUrl, setVideoUrl, handleToggleFavorite } = useExerciseInteractions();
  const [selectedGroup, setSelectedGroup] = useState<MuscleGroup>('all');
  const [selectedDifficulty, setSelectedDifficulty] = useState('all');
  const [selectedEquipment, setSelectedEquipment] = useState('all');
  const [searchText, setSearchText] = useState('');
  const [exercises, setExercises] = useState<API.Exercise[]>([]);
  const [loading, setLoading] = useState(false);
  const [visibleCount, setVisibleCount] = useState(() => getExerciseBatchSize());
  const [timerExercise, setTimerExercise] = useState<API.Exercise | null>(null);
  const [searchParams] = useSearchParams();
  const loadMoreRef = useRef<HTMLDivElement | null>(null);
  const [todayPlan, setTodayPlan] = useState<API.UserTrainingCycle | null>(null);
  const [trainingTemplates, setTrainingTemplates] = useState<API.UserTrainingTemplate[]>([]);
  const [dietTemplates, setDietTemplates] = useState<API.UserDietTemplate[]>([]);
  const [trainingSchedule, setTrainingSchedule] = useState<API.UserTrainingCycle | null>(null);
  const [dietSchedule, setDietSchedule] = useState<API.UserDietCycle | null>(null);
  const [planLoading, setPlanLoading] = useState(false);
  const [activePanel, setActivePanel] = useState<'training' | 'diet' | null>(null);
  const [isMobilePanel, setIsMobilePanel] = useState(
    () => typeof window !== 'undefined' && window.innerWidth <= 768,
  );
  const [allExercises, setAllExercises] = useState<API.Exercise[]>([]);
  const [planFoodLibrary, setPlanFoodLibrary] = useState<API.FoodItem[]>([]);
  const [planFoodKeyword, setPlanFoodKeyword] = useState('');
  const [recIdx, setRecIdx] = useState(0);
  const [planEditorOpen, setPlanEditorOpen] = useState(false);
  const [planScheduleOpen, setPlanScheduleOpen] = useState(false);
  const [planEditorSaving, setPlanEditorSaving] = useState(false);
  const [dietPanelShowAllMeals, setDietPanelShowAllMeals] = useState(false);
  const [dietPanelMealType, setDietPanelMealType] = useState<MealType>(getMealByTime());
  const [trainingPanelShowAllDays, setTrainingPanelShowAllDays] = useState(false);
  const [trainingPanelDayIndex, setTrainingPanelDayIndex] = useState<number>(1);
  const [planEditorMode, setPlanEditorMode] = useState<'training' | 'diet'>('training');
  const [planDraft, setPlanDraft] = useState<PlanDraftState>(createDefaultPlanDraft);
  const [planScheduleDraft, setPlanScheduleDraft] = useState<PlanScheduleDraft>(createDefaultPlanScheduleDraft);
  const [trainingCycles, setTrainingCycles] = useState<API.UserTrainingCycle[]>([]);
  const [dietCycles, setDietCycles] = useState<API.UserDietCycle[]>([]);
  const [dayTemplates, setDayTemplates] = useState<API.UserDietDayTemplate[]>([]);
  const [dayTemplateEditorOpen, setDayTemplateEditorOpen] = useState(false);
  const [dayTemplateSaving, setDayTemplateSaving] = useState(false);
  const [dayTemplateDraft, setDayTemplateDraft] = useState<{
    id?: number;
    name: string;
    mealConfig: Record<string, number>;
  }>({
    name: '',
    mealConfig: {},
  });
  const [managerOpen, setManagerOpen] = useState(false);
  const [managerMode, setManagerMode] = useState<'training' | 'diet'>('training');
  const [managerTab, setManagerTab] = useState('');
  const [managerSelectedIds, setManagerSelectedIds] = useState<Set<number>>(new Set());
  const [batchMode, setBatchMode] = useState(false);
  const [dietRecordOpen, setDietRecordOpen] = useState(false);
  const [dietRecordMealType, setDietRecordMealType] = useState<MealType>(getMealByTime());
  const [dietFoodKeyword, setDietFoodKeyword] = useState('');
  const [dietFoodOptions, setDietFoodOptions] = useState<API.FoodItem[]>([]);
  const [dietFoodLoading, setDietFoodLoading] = useState(false);
  const [dietSelectedFoods, setDietSelectedFoods] = useState<SelectedFoodItem[]>([]);
  const [dietRecentRecords, setDietRecentRecords] = useState<DietHistoryRecord[]>([]);
  const [dietRecentFoods, setDietRecentFoods] = useState<API.FoodItem[]>([]);
  const [myFoods, setMyFoods] = useState<API.FoodItem[]>([]);
  const [dietRecordNote, setDietRecordNote] = useState('');
  const [dietRecordSaving, setDietRecordSaving] = useState(false);
  const [foodEditorOpen, setFoodEditorOpen] = useState(false);
  const [foodEditorSaving, setFoodEditorSaving] = useState(false);
  const [foodRecognizing, setFoodRecognizing] = useState(false);
  const foodRecognizeFileRef = useRef<HTMLInputElement>(null);
  const [foodEditor, setFoodEditor] = useState<FoodEditorState>(createDefaultFoodEditor);
  const [returnToDietAfterFoodEditor, setReturnToDietAfterFoodEditor] = useState(false);

  const todaySheet = useBottomSheetGesture(!!activePanel && isMobilePanel, () => setActivePanel(null));
  const planEditorSheet = useBottomSheetGesture(planEditorOpen && isMobilePanel, () => setPlanEditorOpen(false));
  const planScheduleSheet = useBottomSheetGesture(planScheduleOpen && isMobilePanel, () => setPlanScheduleOpen(false));
  const dayTemplateSheet = useBottomSheetGesture(dayTemplateEditorOpen && isMobilePanel, () => setDayTemplateEditorOpen(false));
  const dietRecordSheet = useBottomSheetGesture(dietRecordOpen && isMobilePanel, () => setDietRecordOpen(false));
  const foodEditorSheet = useBottomSheetGesture(foodEditorOpen && isMobilePanel, () => {
    setFoodEditorOpen(false);
    if (returnToDietAfterFoodEditor) {
      window.setTimeout(() => setDietRecordOpen(true), 180);
      setReturnToDietAfterFoodEditor(false);
    }
  });

  const managerSheet = useBottomSheetGesture(managerOpen && isMobilePanel, () => setManagerOpen(false));

  const overlayLocked = !!activePanel
    || planEditorOpen
    || planScheduleOpen
    || dayTemplateEditorOpen
    || managerOpen
    || (!!dietRecordOpen && !isMobilePanel)
    || (!!foodEditorOpen && !isMobilePanel)
    || todaySheet.mounted
    || planEditorSheet.mounted
    || planScheduleSheet.mounted
    || dayTemplateSheet.mounted
    || dietRecordSheet.mounted
    || foodEditorSheet.mounted
    || managerSheet.mounted;

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

  const fetchMyFoods = async () => {
    try {
      const res = await listMyFoods({ skipErrorHandler: true });
      setMyFoods(Array.isArray(res) ? res : []);
    } catch {
      setMyFoods([]);
    }
  };

  const loadRecentDietFoodsFromStorage = () => {
    if (typeof window === 'undefined') return [];
    try {
      const raw = window.localStorage.getItem(RECENT_DIET_FOODS_STORAGE_KEY);
      const parsed = raw ? JSON.parse(raw) : [];
      return Array.isArray(parsed) ? parsed as API.FoodItem[] : [];
    } catch {
      return [];
    }
  };

  const persistRecentDietFoods = (foods: API.FoodItem[]) => {
    setDietRecentFoods(foods);
    if (typeof window === 'undefined') return;
    try {
      window.localStorage.setItem(RECENT_DIET_FOODS_STORAGE_KEY, JSON.stringify(foods.slice(0, 12)));
    } catch {}
  };

  const pushRecentDietFoods = (foods: API.FoodItem[]) => {
    if (foods.length === 0) return;
    const nextMap = new Map<number, API.FoodItem>();
    foods.forEach((food) => {
      if (food?.id) nextMap.set(food.id, food);
    });
    dietRecentFoods.forEach((food) => {
      if (food?.id && !nextMap.has(food.id)) nextMap.set(food.id, food);
    });
    persistRecentDietFoods(Array.from(nextMap.values()).slice(0, 12));
  };

  useEffect(() => {
    persistRecentDietFoods(loadRecentDietFoodsFromStorage());
  }, []);

  useEffect(() => {
    if (typeof window === 'undefined') return undefined;
    const media = window.matchMedia('(max-width: 768px)');
    const sync = () => setIsMobilePanel(media.matches);
    sync();
    if (media.addEventListener) {
      media.addEventListener('change', sync);
      return () => media.removeEventListener('change', sync);
    }
    media.addListener(sync);
    return () => media.removeListener(sync);
  }, []);

  useEffect(() => {
    if (!dietRecordOpen) {
      return;
    }
    void fetchMyFoods();
    void getTodayDietRecords({ skipErrorHandler: true })
      .then((res) => setDietRecentRecords(Array.isArray(res) ? res : []))
      .catch(() => setDietRecentRecords([]));
  }, [dietRecordOpen]);

  useEffect(() => {
    if (!dietRecordOpen) {
      return;
    }
    const timer = window.setTimeout(async () => {
      try {
        setDietFoodLoading(true);
        const res = await searchFoods(dietFoodKeyword.trim() || undefined, { skipErrorHandler: true });
        setDietFoodOptions(Array.isArray(res) ? res : []);
      } catch {
        setDietFoodOptions([]);
      } finally {
        setDietFoodLoading(false);
      }
    }, 200);
    return () => window.clearTimeout(timer);
  }, [dietFoodKeyword, dietRecordOpen]);

  const fetchPlanData = useCallback(async () => {
    setPlanLoading(true);
    const safe = async <T,>(fn: () => Promise<T>, fallback: T): Promise<T> => {
      try { return await fn(); } catch (e) { console.warn('[fetchPlanData]', e); return fallback; }
    };
    const [activeTrainingCycleRes, trainingTemplateRes, trainingCyclesRes, activeDietCycleRes, dietTemplateRes, dayTemplateRes, dietCyclesRes] = await Promise.all([
      safe(() => getActiveTrainingCycle({ skipErrorHandler: true }), null),
      safe(() => listTrainingTemplates({ skipErrorHandler: true }), []),
      safe(() => listTrainingCycles({ skipErrorHandler: true }), []),
      safe(() => getActiveDietCycle({ skipErrorHandler: true }), null),
      safe(() => listDietTemplates({ skipErrorHandler: true }), []),
      safe(() => listDietDayTemplates({ skipErrorHandler: true }), []),
      safe(() => listDietCycles({ skipErrorHandler: true }), []),
    ]);
    setTodayPlan(activeTrainingCycleRes || null);
    setTrainingTemplates(Array.isArray(trainingTemplateRes) ? trainingTemplateRes : []);
    setTrainingSchedule(activeTrainingCycleRes || null);
    setTrainingCycles(Array.isArray(trainingCyclesRes) ? trainingCyclesRes : []);
    setDietSchedule(activeDietCycleRes || null);
    setDietTemplates(Array.isArray(dietTemplateRes) ? dietTemplateRes : []);
    setDayTemplates(Array.isArray(dayTemplateRes) ? dayTemplateRes : []);
    setDietCycles(Array.isArray(dietCyclesRes) ? dietCyclesRes : []);
    setPlanLoading(false);
  }, []);

  const fetchExercises = useCallback(async (group: string | null) => {
    setLoading(true);
    try {
      if (group && group !== 'all') {
        const res = await getExercisesByGroup(group);
        setExercises(res || []);
      } else {
        const res = await getExerciseList();
        const list = res || [];
        setExercises(list);
        setAllExercises(list);
        if (list.length > 0) {
          const saved = localStorage.getItem('wc_rec_idx');
          if (saved === null) setRecIdx(seededIndex(daySeed(), list.length));
        }
      }
    } catch {
      setExercises([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const group = searchParams.get('group') as MuscleGroup | null;
    const keyword = searchParams.get('keyword') || '';
    const equipment = searchParams.get('equipment') || '';
    const panel = searchParams.get('panel');
    setSearchText(keyword);

    if (group && MUSCLE_GROUP_FILTERS.some((item) => item.key === group)) {
      setSelectedGroup(group);
      fetchExercises(group);
      getExerciseList().then((res: any) => {
        const list: API.Exercise[] = res?.data || res || [];
        if (Array.isArray(list) && list.length > 0) setAllExercises(list);
      }).catch(() => {});
    } else {
      setSelectedGroup('all');
      fetchExercises('all');
    }

    if (equipment) {
      setSelectedEquipment(equipment);
    }

    if (panel === 'training' || panel === 'diet') {
      setActivePanel(panel);
    }
  }, [fetchExercises, searchParams]);

  useEffect(() => {
    void fetchPlanData();
  }, []);

  useEffect(() => {
    if (planEditorOpen && planEditorMode === 'diet' && !planFoodLibrary.length) {
      searchFoods(undefined, { skipErrorHandler: true })
        .then((res) => setPlanFoodLibrary(Array.isArray(res) ? res : []))
        .catch(() => setPlanFoodLibrary([]));
    }
  }, [planEditorOpen, planEditorMode, planFoodLibrary.length]);

  useEffect(() => {
    const keyword = planFoodKeyword.trim();
    if (!keyword) return;
    const timer = window.setTimeout(async () => {
      try {
        const res = await searchFoods(keyword, { skipErrorHandler: true });
        setPlanFoodLibrary(Array.isArray(res) ? res : []);
      } catch {
        setPlanFoodLibrary([]);
      }
    }, 200);
    return () => window.clearTimeout(timer);
  }, [planFoodKeyword]);

  const recPool = useMemo(() => {
    const level = currentUser?.experienceLevel;
    if (level !== 'beginner' && level !== '新手') return allExercises;
    return allExercises.filter((e) => e.difficulty !== 'hard' && e.difficulty !== '困难');
  }, [allExercises, currentUser?.experienceLevel]);
  const recExercise = recPool[recIdx % (recPool.length || 1)];
  const recHighlight = useMemo((): IExerciseData[] => {
    if (!recExercise?.muscleGroup) return [];
    const muscles = getGroupMuscles(recExercise.muscleGroup);
    return muscles.length > 0 ? [{ name: 'hl', muscles, frequency: 2 }] : [];
  }, [recExercise?.muscleGroup]);
  const recIsPosterior = recExercise?.muscleGroup === 'back' || recExercise?.muscleGroup === 'glutes';
  const handleRefreshRec = () => {
    const len = recPool.length || 1;
    let next: number;
    do { next = Math.floor(Math.random() * len); } while (next === recIdx && len > 1);
    setRecIdx(next);
    localStorage.setItem('wc_rec_idx', String(next));
  };

  const handleFilterChange = useCallback((group: MuscleGroup) => {
    setSelectedGroup(group);
    fetchExercises(group);
  }, [fetchExercises]);

  const equipmentOptions = useMemo(() => getEquipmentOptions(exercises), [exercises]);

  const filterRows = useMemo<ExerciseExplorerFilterRow[]>(
    () => [
      {
        label: '肌群',
        activeKey: selectedGroup,
        onChange: (key) => handleFilterChange(key as MuscleGroup),
        options: MUSCLE_GROUP_FILTERS.map((group) => ({
          key: group.key,
          label: group.label,
          color: group.key !== 'all' ? MUSCLE_GROUP_COLORS[group.key] : '#164A41',
          showDot: group.key !== 'all',
        })),
      },
      {
        label: '难度',
        activeKey: selectedDifficulty,
        onChange: setSelectedDifficulty,
        options: DIFFICULTY_OPTIONS,
      },
      {
        label: '器械',
        activeKey: selectedEquipment,
        onChange: setSelectedEquipment,
        options: equipmentOptions,
      },
    ],
    [equipmentOptions, handleFilterChange, selectedDifficulty, selectedEquipment, selectedGroup],
  );

  const displayedExercises = useMemo(() => {
    let result = exercises;
    if (selectedGroup !== 'all') {
      result = result.filter((item) => normalizeExerciseMuscleGroup(item.muscleGroup) === selectedGroup);
    }
    if (selectedDifficulty !== 'all') {
      result = result.filter((item) => normalizeExerciseDifficulty(item.difficulty) === selectedDifficulty);
    }
    if (selectedEquipment !== 'all') {
      result = result.filter((item) => parseEquipment(item.equipment || '').includes(selectedEquipment));
    }
    if (searchText.trim()) {
      const keyword = searchText.trim().toLowerCase();
      result = result.filter((item) =>
        item.name?.toLowerCase().includes(keyword) ||
        item.equipment?.toLowerCase().includes(keyword) ||
        item.difficulty?.toLowerCase().includes(keyword),
      );
    }
    return result;
  }, [exercises, searchText, selectedDifficulty, selectedEquipment, selectedGroup]);
  const hasActiveExerciseFilter = selectedGroup !== 'all'
    || selectedDifficulty !== 'all'
    || !!searchText.trim();

  const visibleExercises = useMemo(
    () => displayedExercises.slice(0, visibleCount),
    [displayedExercises, visibleCount],
  );
  const hasMoreExercises = visibleCount < displayedExercises.length;
  const currentMeal = useMemo(() => getMealByTime(), []);
  const todayPlanIndex = todayPlan?.todayIndex || 1;
  const activePlanDayIndex = trainingPanelShowAllDays ? trainingPanelDayIndex : todayPlanIndex;
  const allTrainingItemsByDay = useMemo(() => {
    const map: Record<number, { items: any[]; templateName: string | null }> = {};
    if (!todayPlan?.days?.length || !trainingTemplates.length) return map;
    for (const day of todayPlan.days) {
      if (!day.templateId) { map[day.dayIndex] = { items: [], templateName: null }; continue; }
      const tpl = trainingTemplates.find((t) => t.id === day.templateId);
      map[day.dayIndex] = {
        items: tpl?.items?.length ? tpl.items.map((item: any) => ({ ...item, sectionType: item.sectionType || 'main' })) : [],
        templateName: tpl?.name || null,
      };
    }
    return map;
  }, [todayPlan?.days, trainingTemplates]);
  const activeTrainingData = allTrainingItemsByDay[activePlanDayIndex] || { items: [], templateName: null };
  const todayTrainingItems = activeTrainingData.items;
  const todayTrainingTemplateName = activeTrainingData.templateName;
  const todayTrainingSections = useMemo(() => {
    const grouped: Record<string, any[]> = { warmup: [], main: [], stretch: [] };
    todayTrainingItems.forEach((item) => {
      const key = item.sectionType || 'main';
      if (!grouped[key]) grouped[key] = [];
      grouped[key].push(item);
    });
    return grouped;
  }, [todayTrainingItems]);
  const todayDietMeals: any[] = useMemo(() => {
    if (!dietSchedule?.days?.length || !dayTemplates.length || !dietTemplates.length) return [];
    const todayDay = dietSchedule.days.find((d) => d.dayIndex === (dietSchedule.todayIndex || 1));
    if (!todayDay?.dayTemplateId) return [];
    const dayTpl = dayTemplates.find((t) => t.id === todayDay.dayTemplateId);
    if (!dayTpl?.mealSlots?.length) return [];
    const meals: any[] = [];
    for (const slot of dayTpl.mealSlots) {
      if (!slot.templateId) continue;
      const mealTpl = dietTemplates.find((t) => t.id === slot.templateId);
      if (!mealTpl?.items?.length) continue;
      meals.push({
        key: `${slot.mealType}-${slot.templateId}`,
        mealType: slot.mealType as MealType,
        planId: slot.templateId,
        planName: mealTpl.name,
        items: mealTpl.items.sort((a: any, b: any) => (a.sortOrder || 0) - (b.sortOrder || 0)),
      });
    }
    return meals.sort((a, b) => MEAL_OPTIONS.indexOf(a.mealType as MealType) - MEAL_OPTIONS.indexOf(b.mealType as MealType));
  }, [dietSchedule, dayTemplates, dietTemplates]);
  const todayDietDayTemplateName = useMemo(() => {
    if (!dietSchedule?.days?.length || !dayTemplates.length) return null;
    const todayDay = dietSchedule.days.find((d) => d.dayIndex === (dietSchedule.todayIndex || 1));
    if (!todayDay?.dayTemplateId) return null;
    const dayTpl = dayTemplates.find((t) => t.id === todayDay.dayTemplateId);
    return dayTpl?.name || null;
  }, [dietSchedule, dayTemplates]);
  const todayDietMealLabel = useMemo(() => {
    const current = todayDietMeals.find((m) => m.mealType === currentMeal);
    return current?.mealType || '';
  }, [todayDietMeals, currentMeal]);
  const availableDietMealTypes = useMemo(
    () => todayDietMeals.map((item) => item.mealType).filter((meal, index, arr) => arr.indexOf(meal) === index),
    [todayDietMeals],
  );
  const activeDietMealType = useMemo(() => {
    if (!dietPanelShowAllMeals) {
      return currentMeal;
    }
    if (availableDietMealTypes.includes(dietPanelMealType)) {
      return dietPanelMealType;
    }
    return availableDietMealTypes[0] || currentMeal;
  }, [availableDietMealTypes, currentMeal, dietPanelMealType, dietPanelShowAllMeals]);
  const currentMealOptions = useMemo(
    () => todayDietMeals.filter((item) => item.mealType === currentMeal),
    [currentMeal, todayDietMeals],
  );
  const visibleMealOptions = useMemo(
    () => todayDietMeals.filter((item) => item.mealType === activeDietMealType),
    [activeDietMealType, todayDietMeals],
  );
  const latestDietRecord = useMemo(
    () => (dietRecentRecords.length > 0 ? dietRecentRecords[dietRecentRecords.length - 1] : null),
    [dietRecentRecords],
  );
  const quickRecentFoods = useMemo(() => {
    const selectedIds = new Set(dietSelectedFoods.map((item) => item.food.id));
    return dietRecentFoods.filter((food) => !selectedIds.has(food.id)).slice(0, 6);
  }, [dietRecentFoods, dietSelectedFoods]);
  useEffect(() => {
    if (activePanel !== 'diet') {
      setDietPanelShowAllMeals(false);
      setDietPanelMealType(currentMeal);
      return;
    }
    setDietPanelMealType(currentMeal);
  }, [activePanel, currentMeal]);
  useEffect(() => {
    if (activePanel !== 'training') {
      setTrainingPanelShowAllDays(false);
      return;
    }
    if (trainingPanelShowAllDays) return;
    setTrainingPanelDayIndex(todayPlanIndex);
  }, [activePanel, todayPlanIndex, trainingPanelShowAllDays]);
  useEffect(() => {
    setVisibleCount(getExerciseBatchSize());
  }, [displayedExercises]);

  useEffect(() => {
    const node = loadMoreRef.current;
    if (!node || !hasMoreExercises) return undefined;

    const observer = new IntersectionObserver(
      (entries) => {
        if (!entries[0]?.isIntersecting) return;
        setVisibleCount((prev) => Math.min(prev + getExerciseBatchSize(), displayedExercises.length));
      },
      { rootMargin: '320px 0px' },
    );

    observer.observe(node);
    return () => observer.disconnect();
  }, [displayedExercises.length, hasMoreExercises]);

  const handleQuickSaveCurrentMeal = async () => {
    const option = visibleMealOptions[0];
    if (!option?.items?.length) {
      message.warning('当前没有可记录的饮食建议');
      return;
    }
    try {
      setDietRecordSaving(true);
      await addDietRecord({
        time: getNowTime(),
        mealType: activeDietMealType,
        source: 'recommendation',
        note: option.planName || undefined,
        items: option.items.map((item) => ({
          foodItemId: item.foodItemId,
          amount: item.amount,
        })),
      }, { skipErrorHandler: true });
      message.success('已记录这一餐');
    } catch {
      message.error('记录失败，请稍后重试');
    } finally {
      setDietRecordSaving(false);
    }
  };

  const handleQuickSaveCurrentTraining = async () => {
    if (!todayTrainingItems.length) {
      message.warning('当前没有可记录的训练安排');
      return;
    }
    try {
      setPlanLoading(true);
      await addStructuredExerciseRecord({
        time: getNowTime(),
        name: todayTrainingTemplateName || trainingSchedule?.name || '训练计划',
        note: `${todayFullName} · Day ${todayPlanIndex}`,
        source: 'recommendation',
        items: todayTrainingItems.map((item) => ({
          exerciseId: item.exerciseId,
          name: item.exerciseName,
          muscleGroup: item.muscleGroup,
          completedSets: item.recommendedSets,
          totalSets: item.recommendedSets,
          durationSeconds: undefined,
          note: item.note,
          source: 'recommendation',
        })),
      }, { skipErrorHandler: true });
      message.success('已记录当前训练');
    } catch (error: any) {
      message.error(getRequestErrorMessage(error, '记录训练失败，请稍后重试'));
    } finally {
      setPlanLoading(false);
    }
  };

  const renderTrainingPlanContent = () => {
    const availableDayIndices = (todayPlan?.days || []).map((d) => d.dayIndex);
    const startDate = todayPlan?.startDate ? new Date(todayPlan.startDate) : null;
    if (!todayTrainingItems.length) {
      if (todayTrainingTemplateName) {
        return (
          <div className="ex-plan-section-list">
            {trainingPanelShowAllDays && availableDayIndices.length > 0 ? (
              <div className="ex-plan-meal-tabs">
                {availableDayIndices.map((dayIdx) => {
                  let weekdayName = `Day ${dayIdx}`;
                  if (startDate) {
                    const dd = new Date(startDate);
                    dd.setDate(dd.getDate() + (dayIdx - 1));
                    weekdayName = WEEKDAY_NAMES[dd.getDay()];
                  }
                  return (
                    <button
                      key={dayIdx}
                      type="button"
                      className={`ex-plan-meal-tab${dayIdx === trainingPanelDayIndex ? ' active' : ''}${dayIdx === todayPlanIndex ? ' is-current' : ''}`}
                      onClick={() => setTrainingPanelDayIndex(dayIdx)}
                    >
                      {weekdayName}
                      {dayIdx === todayPlanIndex ? <span className="ex-plan-meal-tab-badge">今天</span> : null}
                    </button>
                  );
                })}
              </div>
            ) : null}
            <div className="ex-plan-empty">今天是{todayTrainingTemplateName}，休息一下，放松肌肉</div>
          </div>
        );
      }
      return <div className="ex-plan-empty">今天还没有可用的训练安排</div>;
    }
    return (
      <div className="ex-plan-section-list">
        {trainingPanelShowAllDays && availableDayIndices.length > 0 ? (
          <div className="ex-plan-meal-tabs">
            {availableDayIndices.map((dayIdx) => {
              let weekdayName = `Day ${dayIdx}`;
              if (startDate) {
                const dd = new Date(startDate);
                dd.setDate(dd.getDate() + (dayIdx - 1));
                weekdayName = WEEKDAY_NAMES[dd.getDay()];
              }
              return (
                <button
                  key={dayIdx}
                  type="button"
                  className={`ex-plan-meal-tab${dayIdx === trainingPanelDayIndex ? ' active' : ''}${dayIdx === todayPlanIndex ? ' is-current' : ''}`}
                  onClick={() => setTrainingPanelDayIndex(dayIdx)}
                >
                  {weekdayName}
                  {dayIdx === todayPlanIndex ? <span className="ex-plan-meal-tab-badge">今天</span> : null}
                </button>
              );
            })}
          </div>
        ) : null}
        {PLAN_SECTION_OPTIONS
          .filter((option) => (todayTrainingSections[option.value] || []).length > 0)
          .map((option) => (
            <div key={option.value} className="ex-plan-section">
              <div className="ex-plan-section-title">{option.label}</div>
              <div className="ex-plan-chip-list">
                {(todayTrainingSections[option.value] || []).map((item) => (
                  <div key={item.id || `${option.value}-${item.exerciseId}-${item.sortOrder}`} className="ex-plan-chip">
                    <div className="ex-plan-chip-name">{item.exerciseName || `动作 #${item.exerciseId}`}</div>
                    <div className="ex-plan-chip-meta">
                      {[
                        item.muscleGroup ? (MUSCLE_GROUP_LABELS[item.muscleGroup as MuscleGroup] || item.muscleGroup) : '',
                        item.recommendedSets ? `${item.recommendedSets} 组` : '',
                        item.recommendedReps || '',
                      ].filter(Boolean).join(' · ')}
                    </div>
                    {item.note ? <div className="ex-plan-chip-note">{item.note}</div> : null}
                  </div>
                ))}
              </div>
            </div>
          ))}
      </div>
    );
  };

  const renderDietPlanContent = () => {
    if (!visibleMealOptions.length) {
      return <div className="ex-plan-empty">当前时段暂无饮食建议</div>;
    }
    return (
      <div className="ex-plan-section-list">
        {dietPanelShowAllMeals && availableDietMealTypes.length > 1 ? (
          <div className="ex-plan-meal-tabs">
            {availableDietMealTypes.map((mealType) => (
              <button
                key={mealType}
                type="button"
                className={`ex-plan-meal-tab${mealType === activeDietMealType ? ' active' : ''}${mealType === currentMeal ? ' is-current' : ''}`}
                onClick={() => setDietPanelMealType(mealType)}
              >
                {mealType}
                {mealType === currentMeal ? <span className="ex-plan-meal-tab-badge">当前</span> : null}
              </button>
            ))}
          </div>
        ) : null}
        {visibleMealOptions.map((group) => (
          <div key={group.key} className="ex-plan-section ex-plan-meal-section ex-plan-meal-section--current">
            <div className="ex-plan-section-title">
              {group.mealType}
              {group.planName ? ` · ${group.planName}` : ''}
              {group.mealType === currentMeal ? <span className="ex-plan-meal-now">当前时段</span> : null}
            </div>
            <div className="ex-plan-chip-list">
              {group.items.map((item) => (
                <div key={item.id || `${group.key}-${item.foodItemId}-${item.sortOrder}`} className="ex-plan-chip">
                  <div className="ex-plan-chip-name">{item.foodName || `食物 #${item.foodItemId}`}</div>
                  <div className="ex-plan-chip-meta">
                    {`${item.amount}${item.unit}`}{item.calories ? ` · ${kjToKcal(item.calories).toFixed(0)} kcal` : ''}
                  </div>
                  {item.note ? <div className="ex-plan-chip-note">{item.note}</div> : null}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    );
  };

  const handleOpenPlanEditor = (mode: 'training' | 'diet') => {
    setPlanEditorMode(mode);
    const templates = mode === 'training' ? trainingTemplates : dietTemplates;
    setPlanDraft(buildPlanDraftFromTemplate(templates[0], mode));
    setPlanEditorOpen(true);
  };

  const handleSelectPlanTemplate = (templateId?: number) => {
    const templates = planEditorMode === 'training' ? trainingTemplates : dietTemplates;
    const selected = templates.find((item) => item.id === templateId);
    setPlanDraft(buildPlanDraftFromTemplate(selected, planEditorMode));
  };

  const handleCreateNewPlanTemplate = () => {
    setPlanDraft(createDefaultPlanDraft());
  };

  const handleSavePlanDraft = async () => {
    if (!planDraft.name.trim()) {
      message.warning('请先填写计划名称');
      return;
    }
    const templates = planEditorMode === 'training' ? trainingTemplates : dietTemplates;
    if (!checkNameUnique(planDraft.name, templates, planDraft.id)) {
      message.warning('该名称已存在，请使用其他名称');
      return;
    }
    const nextTrainingItems = planDraft.trainingItems
      .filter((item) => item.exerciseId)
      .map((item) => ({
        sectionType: item.sectionType,
        sortOrder: item.sortOrder,
        exerciseId: item.exerciseId!,
        note: item.note.trim() || undefined,
      }));
    const nextDietItems = planDraft.dietItems
      .filter((item) => item.foodItemId && item.amount > 0 && item.unit.trim())
      .map((item) => ({
        sortOrder: item.sortOrder,
        foodItemId: item.foodItemId!,
        amount: item.amount,
        unit: item.unit.trim(),
        note: item.note.trim() || undefined,
      }));
    if (planEditorMode === 'training' && nextTrainingItems.length === 0) {
      message.warning('训练模板至少保留一个动作');
      return;
    }
    if (planEditorMode === 'diet' && !nextDietItems.length) {
      message.warning('饮食计划至少保留一个食物');
      return;
    }
    try {
      setPlanEditorSaving(true);
      if (planEditorMode === 'training') {
        await saveTrainingTemplate({
          id: planDraft.id,
          name: planDraft.name.trim(),
          items: nextTrainingItems.map((item, index) => ({
            sectionType: item.sectionType,
            sortOrder: index,
            exerciseId: item.exerciseId!,
            note: item.note,
          })),
        }, { skipErrorHandler: true });
      } else {
        await saveDietTemplate({
          id: planDraft.id,
          name: planDraft.name.trim(),
          mealType: planDraft.mealType,
          items: nextDietItems.map((item, index) => ({
            sortOrder: index,
            foodItemId: item.foodItemId!,
            amount: item.amount,
            unit: item.unit.trim(),
            note: item.note?.trim() || undefined,
          })),
        }, { skipErrorHandler: true });
      }
      message.success('计划已保存');
      setPlanEditorOpen(false);
      await fetchPlanData();
    } catch (error: any) {
      message.error(getRequestErrorMessage(error, '保存计划失败，请稍后重试'));
    } finally {
      setPlanEditorSaving(false);
    }
  };

  const handleDeletePlanDraft = async () => {
    try {
      setPlanEditorSaving(true);
      if (planDraft.id) {
        if (planEditorMode === 'training') {
          await deleteTrainingTemplate(planDraft.id);
        } else {
          await deleteDietTemplate(planDraft.id);
        }
      }
      await fetchPlanData();
      setPlanEditorOpen(false);
      message.success(planEditorMode === 'training' ? '训练模板已删除' : '饮食模板已删除');
    } catch (error: any) {
      message.error(getRequestErrorMessage(error, '删除计划失败，请稍后重试'));
    } finally {
      setPlanEditorSaving(false);
    }
  };

  const handleOpenPlanSchedule = (mode: 'training' | 'diet') => {
    setPlanEditorMode(mode);
    const schedule = mode === 'training' ? trainingSchedule : dietSchedule;
    if (mode === 'training' && !trainingTemplates.length) {
      message.warning('请先创建训练模板');
      return;
    }
    if (mode === 'diet' && !dayTemplates.length) {
      message.warning('请先创建天模板');
      return;
    }
    let dayCount: number;
    let items: PlanScheduleDraft['items'];
    let name = '';
    let id: number | undefined;
    let startDate = createDefaultPlanScheduleDraft().startDate;
    if (mode === 'training') {
      const cycle = schedule as API.UserTrainingCycle | null;
      const rawDays = cycle?.days || [];
      dayCount = cycle?.dayCount || 7;
      name = cycle?.name || '';
      id = cycle?.id;
      startDate = cycle?.startDate || startDate;
      items = Array.from({ length: dayCount }, (_, index) => {
        const existing = rawDays.find((d) => d.dayIndex === index + 1);
        return { dayIndex: index + 1, templateId: existing?.templateId };
      });
    } else {
      const cycle = schedule as API.UserDietCycle | null;
      const rawDays = cycle?.days || [];
      dayCount = cycle?.dayCount || 7;
      name = cycle?.name || '';
      id = cycle?.id;
      startDate = cycle?.startDate || startDate;
      items = Array.from({ length: dayCount }, (_, index) => {
        const existing = rawDays.find((d) => d.dayIndex === index + 1);
        return { dayIndex: index + 1, dayTemplateId: existing?.dayTemplateId };
      });
    }
    setPlanScheduleDraft({
      id,
      name,
      dayCount,
      startDate,
      items,
    });
    setPlanScheduleOpen(true);
  };

  const handlePlanScheduleDayCountChange = (dayCount: number) => {
    setPlanScheduleDraft((prev) => {
      const safeDayCount = Math.max(1, dayCount || 1);
      const items = Array.from({ length: safeDayCount }, (_, index) => {
        const existing = prev.items.find((item) => item.dayIndex === index + 1);
        return existing || { dayIndex: index + 1 };
      });
      return { ...prev, dayCount: safeDayCount, items };
    });
  };

  const handleSavePlanSchedule = async () => {
    const name = planScheduleDraft.name.trim();
    if (!name) {
      message.warning('请填写循环名称');
      return;
    }
    if (planEditorMode === 'training') {
      if (!checkNameUnique(name, trainingCycles, planScheduleDraft.id)) {
        message.warning('该名称已存在，请使用其他名称');
        return;
      }
      const validDays = planScheduleDraft.items
        .filter((item) => item.templateId)
        .map((item) => ({
          dayIndex: item.dayIndex,
          templateId: item.templateId!,
        }));
      if (!validDays.length) {
        message.warning('至少安排一天');
        return;
      }
      try {
        setPlanEditorSaving(true);
        await saveTrainingCycle({
          id: planScheduleDraft.id,
          name: planScheduleDraft.name.trim(),
          dayCount: planScheduleDraft.dayCount,
          startDate: planScheduleDraft.startDate || undefined,
          activate: true,
          days: validDays,
        }, { skipErrorHandler: true });
        setPlanScheduleOpen(false);
        message.success('训练循环已保存');
        await fetchPlanData();
      } catch (error: any) {
        message.error(getRequestErrorMessage(error, '保存训练循环失败，请稍后重试'));
      } finally {
        setPlanEditorSaving(false);
      }
    } else {
      if (!checkNameUnique(name, dietCycles, planScheduleDraft.id)) {
        message.warning('该名称已存在，请使用其他名称');
        return;
      }
      const validDays = planScheduleDraft.items
        .filter((item) => item.dayTemplateId)
        .map((item) => ({
          dayIndex: item.dayIndex,
          dayTemplateId: item.dayTemplateId!,
        }));
      if (!validDays.length) {
        message.warning('至少安排一天');
        return;
      }
      try {
        setPlanEditorSaving(true);
        await saveDietCycle({
          id: planScheduleDraft.id,
          name: planScheduleDraft.name.trim(),
          dayCount: planScheduleDraft.dayCount,
          startDate: planScheduleDraft.startDate || undefined,
          activate: true,
          days: validDays,
        }, { skipErrorHandler: true });
        setPlanScheduleOpen(false);
        message.success('饮食循环已保存');
        await fetchPlanData();
      } catch (error: any) {
        message.error(getRequestErrorMessage(error, '保存饮食循环失败，请稍后重试'));
      } finally {
        setPlanEditorSaving(false);
      }
    }
  };

  // ===================== 天模板 CRUD =====================

  const handleOpenDayTemplateEditor = (existing?: API.UserDietDayTemplate) => {
    if (existing) {
      const mealConfig: Record<string, number> = {};
      existing.mealSlots?.forEach((slot) => {
        if (slot.templateId) mealConfig[slot.mealType] = slot.templateId;
      });
      setDayTemplateDraft({
        id: existing.id,
        name: existing.name,
        mealConfig,
      });
    } else {
      const name = generateNextName(dayTemplates.map((t) => t.name), '天模板');
      setDayTemplateDraft({ name, mealConfig: {} });
    }
    setDayTemplateEditorOpen(true);
  };

  const handleSaveDayTemplate = async () => {
    if (!dayTemplateDraft.name.trim()) {
      message.warning('请填写天模板名称');
      return;
    }
    if (!checkNameUnique(dayTemplateDraft.name, dayTemplates, dayTemplateDraft.id)) {
      message.warning('该名称已存在，请使用其他名称');
      return;
    }
    const validEntries = Object.entries(dayTemplateDraft.mealConfig).filter(([, v]) => v > 0);
    if (!validEntries.length) {
      message.warning('至少配置一餐');
      return;
    }
    try {
      setDayTemplateSaving(true);
      const mealConfig: Record<string, number> = {};
      validEntries.forEach(([k, v]) => { mealConfig[k] = v; });
      await saveDietDayTemplate({
        id: dayTemplateDraft.id,
        name: dayTemplateDraft.name.trim(),
        mealConfig,
      }, { skipErrorHandler: true });
      setDayTemplateEditorOpen(false);
      message.success('天模板已保存');
      await fetchPlanData();
    } catch (error: any) {
      message.error(getRequestErrorMessage(error, '保存失败'));
    } finally {
      setDayTemplateSaving(false);
    }
  };

  const handleDeleteDayTemplate = async (id: number) => {
    try {
      await deleteDietDayTemplate(id);
      message.success('天模板已删除');
      await fetchPlanData();
    } catch (error: any) {
      message.error(getRequestErrorMessage(error, '删除失败'));
    }
  };

  // ===================== 名称查重 =====================

  const checkNameUnique = (name: string, existingItems: { id?: number; name: string }[], currentId?: number): boolean => {
    const trimmed = name.trim();
    if (!trimmed) return false;
    return !existingItems.some((item) => item.id !== currentId && item.name === trimmed);
  };

  // ===================== 模板管理面板 =====================

  const openManager = (mode: 'training' | 'diet') => {
    setManagerMode(mode);
    setManagerTab(mode === 'training' ? 'training-tpl' : 'diet-tpl');
    setManagerSelectedIds(new Set());
    setManagerOpen(true);
  };

  const handleManagerEditTemplate = (mode: 'training' | 'diet', templateId?: number) => {
    setManagerOpen(false);
    setPlanEditorMode(mode);
    const templates = mode === 'training' ? trainingTemplates : dietTemplates;
    if (templateId) {
      const selected = templates.find((t) => t.id === templateId);
      setPlanDraft(buildPlanDraftFromTemplate(selected, mode));
    } else {
      const draft = createDefaultPlanDraft();
      if (mode === 'training') {
        const names = trainingTemplates.map((t) => t.name);
        draft.name = generateNextName(names, '训练模板');
      } else {
        const mealNames = dietTemplates.filter((t) => t.mealType === draft.mealType).map((t) => t.name);
        draft.name = generateNextName(mealNames, `${draft.mealType}饮食模板`);
      }
      setPlanDraft(draft);
    }
    setTimeout(() => setPlanEditorOpen(true), 100);
  };

  const handleManagerEditCycle = (mode: 'training' | 'diet', cycleId: number) => {
    if (mode === 'training' && !trainingTemplates.length) { message.warning('请先创建训练模板'); return; }
    if (mode === 'diet' && !dayTemplates.length) { message.warning('请先创建天模板'); return; }
    setManagerOpen(false);
    setPlanEditorMode(mode);
    const cycles = mode === 'training' ? trainingCycles : dietCycles;
    const cycle = cycles.find((c) => c.id === cycleId);
    if (!cycle) return;
    const dayCount = cycle.dayCount || 7;
    const rawDays = cycle.days || [];
    const items = Array.from({ length: dayCount }, (_, index) => {
      const existing = rawDays.find((d) => d.dayIndex === index + 1);
      return {
        dayIndex: index + 1,
        [mode === 'training' ? 'templateId' : 'dayTemplateId']: mode === 'training' ? (existing as any)?.templateId : (existing as any)?.dayTemplateId,
      };
    });
    setPlanScheduleDraft({ id: cycle.id, name: cycle.name || '', dayCount, startDate: cycle.startDate || createDefaultPlanScheduleDraft().startDate, items });
    setTimeout(() => setPlanScheduleOpen(true), 100);
  };

  const handleManagerNewCycle = (mode: 'training' | 'diet') => {
    if (mode === 'training' && !trainingTemplates.length) { message.warning('请先创建训练模板'); return; }
    if (mode === 'diet' && !dayTemplates.length) { message.warning('请先创建天模板'); return; }
    setManagerOpen(false);
    setPlanEditorMode(mode);
    const draft = createDefaultPlanScheduleDraft();
    if (mode === 'training') {
      draft.name = generateNextName(trainingCycles.map((c) => c.name), '训练循环');
    } else {
      draft.name = generateNextName(dietCycles.map((c) => c.name), '饮食循环');
    }
    setPlanScheduleDraft(draft);
    setTimeout(() => setPlanScheduleOpen(true), 100);
  };

  const handleManagerEditDayTemplate = (id?: number) => {
    setManagerOpen(false);
    const existing = id ? dayTemplates.find((t) => t.id === id) : undefined;
    handleOpenDayTemplateEditor(existing);
  };

  const toggleManagerSelect = (id: number) => {
    setManagerSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const handleBatchDelete = async () => {
    if (!managerSelectedIds.size) return;
    const ids = Array.from(managerSelectedIds);
    try {
      if (managerTab === 'training-tpl') {
        await Promise.all(ids.map((id) => deleteTrainingTemplate(id)));
      } else if (managerTab === 'diet-tpl') {
        await Promise.all(ids.map((id) => deleteDietTemplate(id)));
      } else if (managerTab === 'day-tpl') {
        await Promise.all(ids.map((id) => deleteDietDayTemplate(id)));
      } else if (managerTab === 'training-cycle') {
        await Promise.all(ids.map((id) => deleteTrainingCycle(id)));
      } else if (managerTab === 'diet-cycle') {
        await Promise.all(ids.map((id) => deleteDietCycle(id)));
      }
      message.success(`已删除 ${ids.length} 项`);
      setManagerSelectedIds(new Set());
      await fetchPlanData();
    } catch (error: any) {
      message.error(getRequestErrorMessage(error, '部分删除失败，已刷新列表'));
      setManagerSelectedIds(new Set());
      await fetchPlanData();
    }
  };

  const handleManagerTabChange = (tab: string) => {
    setManagerTab(tab);
    setManagerSelectedIds(new Set());
  };

  const renderManagerBody = () => {
    const isTraining = managerMode === 'training';
    const trainingTabClass = managerTab === 'training-tpl' ? 'ex-manager-tab--active' : '';
    const trainingCycleClass = managerTab === 'training-cycle' ? 'ex-manager-tab--active' : '';
    const dietTplClass = managerTab === 'diet-tpl' ? 'ex-manager-tab--active' : '';
    const dayTplClass = managerTab === 'day-tpl' ? 'ex-manager-tab--active' : '';
    const dietCycleClass = managerTab === 'diet-cycle' ? 'ex-manager-tab--active' : '';
    const hasSelection = batchMode && managerSelectedIds.size > 0;

    const renderCheckbox = (id: number) => batchMode ? (
      <Checkbox
        checked={managerSelectedIds.has(id)}
        onChange={() => toggleManagerSelect(id)}
        onClick={(e) => e.stopPropagation()}
      />
    ) : null;

    let listContent: React.ReactNode = null;
    if (managerTab === 'training-tpl') {
      listContent = (
        <div className="ex-manager-list">
          {trainingTemplates.length === 0 ? <div className="ex-plan-empty">还没有训练模板</div> : trainingTemplates.map((t) => (
            <div key={t.id} className="ex-day-template-card">
              <div className="ex-day-template-card-head">
                {renderCheckbox(t.id)}
                <span className="ex-day-template-card-name">{t.name}</span>
                <div className="ex-day-template-card-actions">
                  <Button type="text" size="small" onClick={() => handleManagerEditTemplate('training', t.id)}>编辑</Button>
                  <Button type="text" size="small" danger onClick={async () => {
                    try {
                      await deleteTrainingTemplate(t.id, { skipErrorHandler: true });
                      message.success('已删除');
                      await fetchPlanData();
                    } catch { message.error('删除失败'); }
                  }}>删除</Button>
                </div>
              </div>
              <div className="ex-day-template-card-meals">
                <span className="ex-day-template-meal-tag">{t.items.length} 个动作</span>
              </div>
            </div>
          ))}
          <Button type="dashed" size="small" icon={<PlusOutlined />} block style={{ marginTop: 12 }} onClick={() => handleManagerEditTemplate('training')}>新建训练模板</Button>
        </div>
      );
    } else if (managerTab === 'training-cycle') {
      listContent = (
        <div className="ex-manager-list">
          {trainingCycles.length === 0 ? <div className="ex-plan-empty">还没有训练循环</div> : trainingCycles.map((c) => (
            <div key={c.id} className="ex-day-template-card" style={c.id === trainingSchedule?.id ? { borderColor: 'var(--primary-color)' } : undefined}>
              <div className="ex-day-template-card-head">
                {renderCheckbox(c.id)}
                <span className="ex-day-template-card-name">{c.name}{c.id === trainingSchedule?.id ? <span style={{ color: 'var(--primary-color)', marginLeft: 8, fontSize: 12 }}>当前</span> : null}</span>
                <div className="ex-day-template-card-actions">
                  {c.id !== trainingSchedule?.id && <Button type="text" size="small" onClick={() => void handleActivateCycle('training', c.id)}>激活</Button>}
                  <Button type="text" size="small" onClick={() => handleManagerEditCycle('training', c.id)}>编辑</Button>
                </div>
              </div>
              <div style={{ fontSize: 12, color: 'var(--text-3)' }}>{c.dayCount}天 · {c.startDate || '未设置日期'}</div>
            </div>
          ))}
          <Button type="dashed" size="small" icon={<PlusOutlined />} block style={{ marginTop: 12 }} onClick={() => handleManagerNewCycle('training')}>新建训练循环</Button>
        </div>
      );
    } else if (managerTab === 'diet-tpl') {
      listContent = (
        <div className="ex-manager-list">
          {dietTemplates.length === 0 ? <div className="ex-plan-empty">还没有饮食模板</div> : dietTemplates.map((t) => (
            <div key={t.id} className="ex-day-template-card">
              <div className="ex-day-template-card-head">
                {renderCheckbox(t.id)}
                <span className="ex-day-template-card-name">{t.mealType || ''} · {t.name}</span>
                <div className="ex-day-template-card-actions">
                  <Button type="text" size="small" onClick={() => handleManagerEditTemplate('diet', t.id)}>编辑</Button>
                  <Button type="text" size="small" danger onClick={async () => {
                    try {
                      await deleteDietTemplate(t.id, { skipErrorHandler: true });
                      message.success('已删除');
                      await fetchPlanData();
                    } catch { message.error('删除失败'); }
                  }}>删除</Button>
                </div>
              </div>
              <div className="ex-day-template-card-meals">
                <span className="ex-day-template-meal-tag">{t.items.length} 种食物</span>
              </div>
            </div>
          ))}
          <Button type="dashed" size="small" icon={<PlusOutlined />} block style={{ marginTop: 12 }} onClick={() => handleManagerEditTemplate('diet')}>新建饮食模板</Button>
        </div>
      );
    } else if (managerTab === 'day-tpl') {
      listContent = (
        <div className="ex-manager-list">
          {dayTemplates.length === 0 ? <div className="ex-plan-empty">还没有天模板</div> : dayTemplates.map((t) => (
            <div key={t.id} className="ex-day-template-card">
              <div className="ex-day-template-card-head">
                {renderCheckbox(t.id)}
                <span className="ex-day-template-card-name">{t.name}</span>
                <div className="ex-day-template-card-actions">
                  <Button type="text" size="small" onClick={() => handleManagerEditDayTemplate(t.id)}>编辑</Button>
                </div>
              </div>
              <div className="ex-day-template-card-meals">
                {t.mealSlots?.map((m) => (
                  <span key={m.mealType} className="ex-day-template-meal-tag">{m.mealType} · {m.templateName || '已删除'}</span>
                ))}
              </div>
            </div>
          ))}
          <Button type="dashed" size="small" icon={<PlusOutlined />} block style={{ marginTop: 12 }} onClick={() => handleManagerEditDayTemplate()}>新建天模板</Button>
        </div>
      );
    } else if (managerTab === 'diet-cycle') {
      listContent = (
        <div className="ex-manager-list">
          {dietCycles.length === 0 ? <div className="ex-plan-empty">还没有饮食循环</div> : dietCycles.map((c) => (
            <div key={c.id} className="ex-day-template-card" style={c.id === dietSchedule?.id ? { borderColor: '#E8A838' } : undefined}>
              <div className="ex-day-template-card-head">
                {renderCheckbox(c.id)}
                <span className="ex-day-template-card-name">{c.name}{c.id === dietSchedule?.id ? <span style={{ color: '#E8A838', marginLeft: 8, fontSize: 12 }}>当前</span> : null}</span>
                <div className="ex-day-template-card-actions">
                  {c.id !== dietSchedule?.id && <Button type="text" size="small" onClick={() => void handleActivateCycle('diet', c.id)}>激活</Button>}
                  <Button type="text" size="small" onClick={() => handleManagerEditCycle('diet', c.id)}>编辑</Button>
                </div>
              </div>
              <div style={{ fontSize: 12, color: 'var(--text-3)' }}>{c.dayCount}天 · {c.startDate || '未设置日期'}</div>
            </div>
          ))}
          <Button type="dashed" size="small" icon={<PlusOutlined />} block style={{ marginTop: 12 }} onClick={() => handleManagerNewCycle('diet')}>新建饮食循环</Button>
        </div>
      );
    }

    return (
      <>
        <div className="ex-manager-tabs">
          {isTraining ? (
            <>
              <div className={`ex-manager-tab ${trainingTabClass}`} onClick={() => handleManagerTabChange('training-tpl')}>训练模板</div>
              <div className={`ex-manager-tab ${trainingCycleClass}`} onClick={() => handleManagerTabChange('training-cycle')}>训练循环</div>
            </>
          ) : (
            <>
              <div className={`ex-manager-tab ${dietTplClass}`} onClick={() => handleManagerTabChange('diet-tpl')}>饮食模板</div>
              <div className={`ex-manager-tab ${dayTplClass}`} onClick={() => handleManagerTabChange('day-tpl')}>天模板</div>
              <div className={`ex-manager-tab ${dietCycleClass}`} onClick={() => handleManagerTabChange('diet-cycle')}>饮食循环</div>
            </>
          )}
        </div>
        {batchMode && (
          <div className="ex-manager-batch-bar">
            <span>{hasSelection ? `已选 ${managerSelectedIds.size} 项` : '点击选择要操作的项'}</span>
            <div>
              <Button type="text" size="small" onClick={() => { setBatchMode(false); setManagerSelectedIds(new Set()); }}>取消</Button>
              {hasSelection && <Button type="text" size="small" danger onClick={() => void handleBatchDelete()}>删除选中</Button>}
            </div>
          </div>
        )}
        {!batchMode && (
          <div style={{ display: 'flex', justifyContent: 'flex-end', padding: '0 16px' }}>
            <Button type="text" size="small" onClick={() => { setBatchMode(true); setManagerSelectedIds(new Set()); }}>管理</Button>
          </div>
        )}
        {listContent}
      </>
    );
  };

  const renderDayTemplateEditor = () => (
    <div className="ex-plan-editor">
      <div className="ex-plan-editor-field ex-plan-editor-field--wide">
        <label className="ex-diet-form-label">天模板名称</label>
        <Input
          value={dayTemplateDraft.name}
          onChange={(e) => setDayTemplateDraft((prev) => ({ ...prev, name: e.target.value }))}
          placeholder="如：训练日、休息日"
          maxLength={40}
        />
      </div>
      <div className="ex-plan-editor-list">
        {MEAL_OPTIONS.map((mealType) => {
          const selectedTemplateId = dayTemplateDraft.mealConfig[mealType];
          const mealTemplates = dietTemplates.filter((t) => t.mealType === mealType);
          return (
            <div key={mealType} className="ex-plan-editor-row ex-plan-editor-row--schedule">
              <div className="ex-plan-schedule-day">{mealType}</div>
              <Select
                allowClear
                showSearch
                placeholder={`选择${mealType}模板`}
                value={selectedTemplateId || undefined}
                optionFilterProp="label"
                options={mealTemplates.map((t) => ({ value: t.id, label: t.name }))}
                onChange={(value) => setDayTemplateDraft((prev) => ({
                  ...prev,
                  mealConfig: { ...prev.mealConfig, [mealType]: value || 0 },
                }))}
              />
            </div>
          );
        })}
      </div>
    </div>
  );

  const renderDayTemplateList = () => (
    <div className="ex-day-template-section">
      <div className="ex-plan-editor-section-head">
        <div className="ex-plan-editor-section-title">天模板</div>
        <Button type="dashed" size="small" icon={<PlusOutlined />} onClick={() => handleOpenDayTemplateEditor()}>
          新建天模板
        </Button>
      </div>
      {dayTemplates.length === 0 ? (
        <div className="ex-plan-empty">还没有天模板，先创建一个</div>
      ) : (
        <div className="ex-day-template-list">
          {dayTemplates.map((tpl) => (
            <div key={tpl.id} className="ex-day-template-card">
              <div className="ex-day-template-card-head">
                <span className="ex-day-template-card-name">{tpl.name}</span>
                <div className="ex-day-template-card-actions">
                  <Button type="text" size="small" onClick={() => handleOpenDayTemplateEditor(tpl)}>编辑</Button>
                  <Button type="text" size="small" danger onClick={() => handleDeleteDayTemplate(tpl.id)}>删除</Button>
                </div>
              </div>
              <div className="ex-day-template-card-meals">
                {tpl.mealSlots?.map((m) => (
                  <span key={m.mealType} className="ex-day-template-meal-tag">
                    {m.mealType} · {m.templateName || '已删除'}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );

  const handleActivateCycle = async (mode: 'training' | 'diet', cycleId: number) => {
    try {
      if (mode === 'training') {
        await activateTrainingCycle(cycleId);
      } else {
        await activateDietCycle(cycleId);
      }
      message.success('已切换');
      await fetchPlanData();
    } catch (error: any) {
      message.error(getRequestErrorMessage(error, '切换失败'));
    }
  };

  const handleDeleteCycle = async (mode: 'training' | 'diet', cycleId: number) => {
    try {
      if (mode === 'training') {
        await deleteTrainingCycle(cycleId);
      } else {
        await deleteDietCycle(cycleId);
      }
      message.success('已删除');
      await fetchPlanData();
    } catch (error: any) {
      message.error(getRequestErrorMessage(error, '删除失败'));
    }
  };

  const renderCycleList = (mode: 'training' | 'diet') => {
    const cycles = mode === 'training' ? trainingCycles : dietCycles;
    const activeId = mode === 'training' ? trainingSchedule?.id : dietSchedule?.id;
    if (!cycles.length) return null;
    return (
      <div className="ex-day-template-section" style={{ marginBottom: 16 }}>
        <div className="ex-plan-editor-section-head">
          <div className="ex-plan-editor-section-title">已有循环</div>
        </div>
        <div className="ex-day-template-list">
          {cycles.map((c) => (
            <div key={c.id} className="ex-day-template-card" style={c.id === activeId ? { borderColor: 'var(--primary-color)' } : undefined}>
              <div className="ex-day-template-card-head">
                <span className="ex-day-template-card-name">
                  {c.name}
                  {c.id === activeId ? <span style={{ color: 'var(--primary-color)', marginLeft: 8, fontSize: 12 }}>当前</span> : null}
                </span>
                <div className="ex-day-template-card-actions">
                  {c.id !== activeId && (
                    <Button type="text" size="small" onClick={() => handleActivateCycle(mode, c.id)}>激活</Button>
                  )}
                  <Button type="text" size="small" onClick={() => {
                    setPlanScheduleDraft((prev) => ({
                      ...prev,
                      id: c.id,
                      name: c.name,
                      dayCount: c.dayCount,
                      startDate: c.startDate || prev.startDate,
                      items: (c.days || []).map((d) => ({
                        dayIndex: d.dayIndex,
                        [mode === 'training' ? 'templateId' : 'dayTemplateId']: mode === 'training' ? (d as any).templateId : (d as any).dayTemplateId,
                      })),
                    }));
                  }}>编辑</Button>
                  <Button type="text" size="small" danger onClick={() => handleDeleteCycle(mode, c.id)}>删除</Button>
                </div>
              </div>
              <div style={{ fontSize: 12, color: 'var(--text-3)' }}>
                {c.dayCount}天 · {c.startDate || '未设置日期'}
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  };

  const renderPlanScheduleBody = () => {
    const templates = planEditorMode === 'training' ? trainingTemplates : dietTemplates;
    const dayRows = Array.from({ length: planScheduleDraft.dayCount }, (_, index) => index + 1);
    return (
      <div className="ex-plan-editor">
        {renderCycleList(planEditorMode)}
        {planEditorMode === 'diet' && renderDayTemplateList()}
        {(planEditorMode === 'training' || planEditorMode === 'diet') && (
          <div className="ex-plan-editor-head">
            <div className="ex-plan-editor-field ex-plan-editor-field--wide">
              <label className="ex-diet-form-label">循环名称</label>
              <Input
                value={planScheduleDraft.name}
                onChange={(e) => setPlanScheduleDraft((prev) => ({ ...prev, name: e.target.value }))}
                placeholder={planEditorMode === 'training' ? '如：增肌期、减脂期' : '如：增肌饮食、减脂饮食'}
                maxLength={40}
              />
            </div>
          </div>
        )}
        <div className="ex-plan-editor-head">
          <div className="ex-plan-editor-field">
            <label className="ex-diet-form-label">循环天数</label>
            <InputNumber
              min={1}
              max={30}
              value={planScheduleDraft.dayCount}
              onChange={(value) => handlePlanScheduleDayCountChange(typeof value === 'number' ? value : 1)}
              style={{ width: '100%' }}
            />
          </div>
          <div className="ex-plan-editor-field">
            <label className="ex-diet-form-label">开始日期</label>
            <Input
              type="date"
              value={planScheduleDraft.startDate}
              onChange={(e) => setPlanScheduleDraft((prev) => ({ ...prev, startDate: e.target.value }))}
            />
          </div>
        </div>
        <div className="ex-plan-editor-list">
          {planEditorMode === 'training' ? planScheduleDraft.items.map((item, index) => (
            <div key={`schedule-${index}`} className="ex-plan-editor-row ex-plan-editor-row--schedule">
              <div className="ex-plan-schedule-day">Day {item.dayIndex}</div>
              <Select
                showSearch
                placeholder="选择训练模板"
                value={item.templateId}
                optionFilterProp="label"
                options={templates.map((template) => ({ value: template.id, label: template.name }))}
                onChange={(value) => setPlanScheduleDraft((prev) => ({
                  ...prev,
                  items: prev.items.map((row, rowIndex) => (rowIndex === index ? { ...row, templateId: value } : row)),
                }))}
              />
            </div>
          )) : dayRows.map((dayIndex) => {
            const item = planScheduleDraft.items.find((row) => row.dayIndex === dayIndex && row.dayTemplateId);
            return (
              <div key={`diet-schedule-${dayIndex}`} className="ex-plan-editor-row ex-plan-editor-row--schedule">
                <div className="ex-plan-schedule-day">Day {dayIndex}</div>
                <Select
                  allowClear
                  showSearch
                  placeholder="选择天模板"
                  value={item?.dayTemplateId}
                  optionFilterProp="label"
                  options={dayTemplates.map((tpl) => ({
                    value: tpl.id,
                    label: `${tpl.name}（${tpl.mealSlots?.map((m) => m.mealType).join('/') || '无餐次'}）`,
                  }))}
                  onChange={(value) => setPlanScheduleDraft((prev) => ({
                    ...prev,
                    items: prev.items.map((row) => (
                      row.dayIndex === dayIndex ? { ...row, dayTemplateId: value || undefined, planId: undefined, mealType: undefined } : row
                    )),
                  }))}
                />
              </div>
            );
          })}
        </div>
      </div>
    );
  };

  const renderTodayPanel = () => {
    if (!activePanel) return null;

    const isTraining = activePanel === 'training';
    const getWeekdayForDayIndex = (dayIdx: number) => {
      if (!todayPlan?.startDate) return `Day ${dayIdx}`;
      const d = new Date(todayPlan.startDate);
      d.setDate(d.getDate() + (dayIdx - 1));
      return WEEKDAY_NAMES[d.getDay()];
    };
    const title = isTraining
      ? (trainingPanelShowAllDays ? `训练计划 · ${getWeekdayForDayIndex(trainingPanelDayIndex)}` : '今日训练计划')
      : '今日饮食计划';
    const subtitle = isTraining
      ? `${trainingPanelShowAllDays ? getWeekdayForDayIndex(trainingPanelDayIndex) : `${todayFullName} · Day ${todayPlanIndex}`} · ${todayTrainingTemplateName || '暂无安排'}`
      : `${todayFullName} · Day ${todayPlanIndex}${todayDietDayTemplateName ? ` · ${todayDietDayTemplateName}` : ''}${todayDietMeals.length ? ` · ${todayDietMeals.length}餐` : ''}`;

    if (isMobilePanel) {
      return (
        <div className="ex-today-sheet" style={todaySheet.sheetStyle}>
          <div className="ex-today-sheet-handle" {...todaySheet.dragHandleProps} />
          <div className={`ex-today-sheet-head ex-today-sheet-head--${activePanel}`} {...todaySheet.dragHandleProps}>
            <div className="ex-today-sheet-title-row">
              <div className="ex-today-sheet-title">
                <span className="ex-today-sheet-icon">{isTraining ? <CalendarOutlined /> : <ReadOutlined />}</span>
                {title}
              </div>
            </div>
            <div className="ex-today-sheet-subline">
              <div className="ex-today-sheet-subtitle">{subtitle}</div>
            </div>
          </div>
          <div className="ex-today-sheet-body">
            <div className={`ex-today-sheet-card ex-today-sheet-card--${activePanel}`}>
              {isTraining ? renderTrainingPlanContent() : renderDietPlanContent()}
            </div>
          </div>
          <div className="ex-today-sheet-footer">
            <Button
              className="ex-dialog-footer-btn"
              onClick={() => {
                if (isTraining) {
                  if (trainingPanelShowAllDays) {
                    setTrainingPanelShowAllDays(false);
                    setTrainingPanelDayIndex(todayPlanIndex);
                  } else {
                    setTrainingPanelShowAllDays(true);
                    setTrainingPanelDayIndex(todayPlanIndex);
                  }
                  return;
                }
                if (dietPanelShowAllMeals) {
                  setDietPanelShowAllMeals(false);
                  setDietPanelMealType(currentMeal);
                } else {
                  setDietPanelShowAllMeals(true);
                }
              }}
            >
              {isTraining
                ? (trainingPanelShowAllDays ? '今天' : '其他训练日')
                : (dietPanelShowAllMeals ? '当前时段' : '其余餐次')}
            </Button>
            {isTraining ? (
              <Button
                type="primary"
                className="ex-dialog-footer-btn ex-dialog-footer-btn--primary"
                loading={planLoading}
                onClick={() => void handleQuickSaveCurrentTraining()}
              >
                记录当前训练
              </Button>
            ) : (
              <Button
                type="primary"
                className="ex-dialog-footer-btn ex-dialog-footer-btn--primary"
                loading={dietRecordSaving}
                onClick={() => {
                  if (visibleMealOptions.length > 0) {
                    void handleQuickSaveCurrentMeal();
                    return;
                  }
                  setDietRecordOpen(true);
                }}
              >
                {visibleMealOptions.length > 0 ? `记录当前${activeDietMealType}` : '手动记录'}
              </Button>
            )}
          </div>
        </div>
      );
    }

    return (
      <div className="ex-today-modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) setActivePanel(null); }}>
        <div className="ex-today-modal" onClick={(e) => e.stopPropagation()}>
          <div className={`ex-today-modal-header ex-today-modal-header--${activePanel}`}>
            <div className="ex-today-modal-title-row">
              <div className="ex-today-modal-title">
                <span className="ex-today-modal-icon">{isTraining ? <CalendarOutlined /> : <ReadOutlined />}</span>
                {title}
              </div>
              <div className="ex-today-modal-title-actions">
                <button className="ex-today-modal-close" onClick={() => setActivePanel(null)} type="button">✕</button>
              </div>
            </div>
            <div className="ex-today-modal-subline">
              <div className="ex-today-modal-subtitle">{subtitle}</div>
            </div>
          </div>
          <div className="ex-today-modal-body">
            <div className={`ex-today-modal-card ex-today-modal-card--${activePanel}`}>
              {isTraining ? renderTrainingPlanContent() : renderDietPlanContent()}
            </div>
          </div>
          <div className="ex-today-modal-footer">
            <Button
              className="ex-dialog-footer-btn"
              onClick={() => {
                if (isTraining) {
                  if (trainingPanelShowAllDays) {
                    setTrainingPanelShowAllDays(false);
                    setTrainingPanelDayIndex(todayPlanIndex);
                  } else {
                    setTrainingPanelShowAllDays(true);
                    setTrainingPanelDayIndex(todayPlanIndex);
                  }
                  return;
                }
                if (dietPanelShowAllMeals) {
                  setDietPanelShowAllMeals(false);
                  setDietPanelMealType(currentMeal);
                } else {
                  setDietPanelShowAllMeals(true);
                }
              }}
            >
              {isTraining
                ? (trainingPanelShowAllDays ? '今天' : '其他训练日')
                : (dietPanelShowAllMeals ? '当前时段' : '其余餐次')}
            </Button>
            {isTraining ? (
              <Button
                type="primary"
                className="ex-dialog-footer-btn ex-dialog-footer-btn--primary"
                loading={planLoading}
                onClick={() => void handleQuickSaveCurrentTraining()}
              >
                记录当前训练
              </Button>
            ) : (
              <Button
                type="primary"
                className="ex-dialog-footer-btn ex-dialog-footer-btn--primary"
                loading={dietRecordSaving}
                onClick={() => {
                  if (visibleMealOptions.length > 0) {
                    void handleQuickSaveCurrentMeal();
                    return;
                  }
                  setDietRecordOpen(true);
                }}
              >
                {visibleMealOptions.length > 0 ? `记录当前${activeDietMealType}` : '手动记录'}
              </Button>
            )}
          </div>
        </div>
      </div>
    );
  };

  const handleSaveDietRecord = async () => {
    if (dietSelectedFoods.length === 0) {
      message.warning('请先添加食物');
      return;
    }
    const invalidAmount = dietSelectedFoods.some((item) => !item.amount || item.amount <= 0);
    if (invalidAmount) {
      message.warning('请填写有效的摄入量');
      return;
    }
    try {
      setDietRecordSaving(true);
      const selectedFoods = dietSelectedFoods.map((item) => item.food);
      await addDietRecord({
        time: getNowTime(),
        mealType: dietRecordMealType,
        note: dietRecordNote.trim() || undefined,
        source: 'manual',
        items: dietSelectedFoods.map((item) => ({
          foodItemId: item.food.id,
          amount: item.amount,
        })),
      }, { skipErrorHandler: true });
      message.success('饮食记录已保存');
      pushRecentDietFoods(selectedFoods);
      void getTodayDietRecords({ skipErrorHandler: true })
        .then((res) => setDietRecentRecords(Array.isArray(res) ? res : []))
        .catch(() => {});
      if (isMobilePanel) {
        dietRecordSheet.requestClose();
      } else {
        setDietRecordOpen(false);
      }
      setDietRecordMealType(getMealByTime());
      setDietFoodKeyword('');
      setDietFoodOptions([]);
      setDietSelectedFoods([]);
      setDietRecordNote('');
    } catch {
      message.error('饮食记录保存失败，请稍后重试');
    } finally {
      setDietRecordSaving(false);
    }
  };

  const handleAddFood = (food: API.FoodItem) => {
    setDietSelectedFoods((prev) => {
      if (prev.some((item) => item.food.id === food.id)) {
        return prev;
      }
      return [...prev, { food, amount: food.baseAmount || 100 }];
    });
    pushRecentDietFoods([food]);
  };

  const handleApplyRecentRecord = (record: DietHistoryRecord) => {
    const items = Array.isArray(record.items) ? record.items : [];
    if (items.length === 0) {
      message.warning('上一餐没有可复制的食物');
      return;
    }
    const selected = items
      .map((item) => {
        const matchedMyFood = myFoods.find((food) => food.id === item.foodItemId);
        const matchedRecentFood = dietRecentFoods.find((food) => food.id === item.foodItemId);
        const matchedOption = dietFoodOptions.find((food) => food.id === item.foodItemId);
        const food = matchedMyFood || matchedRecentFood || matchedOption;
        if (!food || !food.id) return null;
        return {
          food,
          amount: item.amount || food.baseAmount || 100,
        };
      })
      .filter(Boolean) as SelectedFoodItem[];

    if (selected.length === 0) {
      message.warning('上一餐里的食物暂时无法自动带出，请先搜索或新建');
      return;
    }

    setDietRecordMealType((record.mealType as MealType) || getMealByTime());
    setDietSelectedFoods(selected);
    setDietRecordNote(record.note || '');
    pushRecentDietFoods(selected.map((item) => item.food));
    message.success('已复制上一餐，可以直接微调后保存');
  };

  const handleRemoveFood = (foodId: number) => {
    setDietSelectedFoods((prev) => prev.filter((item) => item.food.id !== foodId));
  };

  const handleFoodAmountChange = (foodId: number, value: number | null) => {
    setDietSelectedFoods((prev) => prev.map((item) => (
      item.food.id === foodId
        ? { ...item, amount: typeof value === 'number' ? value : 0 }
        : item
    )));
  };

  const openFoodEditor = (food?: API.FoodItem) => {
    if (food) {
      setFoodEditor({
        id: food.id,
        name: food.name || '',
        imageUrl: food.imageUrl || '',
        category: food.category || '',
        unit: food.unit || 'g',
        baseAmount: food.baseAmount || 100,
        calories: food.calories || 0,
        protein: food.protein || 0,
        carbs: food.carbs || 0,
        fat: food.fat || 0,
        fiber: food.fiber || 0,
      });
    } else {
      setFoodEditor(createDefaultFoodEditor());
    }
    if (isMobilePanel && dietRecordOpen) {
      setReturnToDietAfterFoodEditor(true);
      dietRecordSheet.requestClose();
      window.setTimeout(() => setFoodEditorOpen(true), 180);
      return;
    }
    setFoodEditorOpen(true);
  };

  const closeFoodEditor = () => {
    if (isMobilePanel && returnToDietAfterFoodEditor) {
      foodEditorSheet.requestClose();
      return;
    }
    setFoodEditorOpen(false);
  };

  const handleFoodRecognize = async (file: File) => {
    setFoodRecognizing(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      const r: any = await recognizeFoodImage(formData, { skipErrorHandler: true });
      if (!r) {
        message.error('识别失败，请重试');
        return;
      }
      const nutrition = r.nutritionPerUnit || r.nutritionPer100g || {};
      const perUnit = r.perUnitAmount || 100;
      setFoodEditor((prev) => ({
        ...prev,
        id: prev.id,
        name: r.foodName || prev.name,
        imageUrl: r.imageUrl || prev.imageUrl,
        unit: r.unit || prev.unit,
        baseAmount: perUnit,
        calories: nutrition.calories || 0,
        protein: nutrition.protein || 0,
        carbs: nutrition.carbs || 0,
        fat: nutrition.fat || 0,
        fiber: nutrition.fiber || 0,
      }));
      message.success('识别成功，请核对数据后保存');
    } catch (error: any) {
      message.error(error?.message || '识别失败');
    } finally {
      setFoodRecognizing(false);
    }
  };

  const handleFoodEditorChange = <K extends keyof FoodEditorState>(key: K, value: FoodEditorState[K]) => {
    setFoodEditor((prev) => ({ ...prev, [key]: value }));
  };

  const handleFoodImageUpload = async (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    if (foodEditor.id) {
      formData.append('foodId', String(foodEditor.id));
    }
    try {
      const url = await uploadFoodImage(formData, { skipErrorHandler: true });
      setFoodEditor((prev) => ({ ...prev, imageUrl: url || '' }));
      message.success('图片已上传');
    } catch {
      message.error('图片上传失败，请稍后重试');
    }
    return false;
  };

  const handleSaveFoodItem = async () => {
    if (!foodEditor.name.trim()) {
      message.warning('请填写食物名称');
      return;
    }
    if (!foodEditor.unit.trim()) {
      message.warning('请填写单位');
      return;
    }
    if (!foodEditor.baseAmount || foodEditor.baseAmount <= 0) {
      message.warning('请填写有效的营养基准量');
      return;
    }
    try {
      setFoodEditorSaving(true);
      const saved = await saveFoodItem({
        id: foodEditor.id,
        name: foodEditor.name.trim(),
        imageUrl: foodEditor.imageUrl || undefined,
        category: foodEditor.category.trim() || undefined,
        unit: foodEditor.unit.trim(),
        baseAmount: foodEditor.baseAmount,
        calories: foodEditor.calories,
        protein: foodEditor.protein,
        carbs: foodEditor.carbs,
        fat: foodEditor.fat,
        fiber: foodEditor.fiber,
      }, { skipErrorHandler: true });
      message.success(foodEditor.id ? '食物已更新' : '食物已创建');
      if (isMobilePanel && returnToDietAfterFoodEditor) {
        setFoodEditorOpen(false);
        window.setTimeout(() => setDietRecordOpen(true), 180);
        setReturnToDietAfterFoodEditor(false);
      } else {
        setFoodEditorOpen(false);
      }
      await fetchMyFoods();
      searchFoods(undefined, { skipErrorHandler: true })
        .then((res) => setPlanFoodLibrary(Array.isArray(res) ? res : []))
        .catch(() => {});
      if (saved?.id) {
        handleAddFood(saved);
      }
    } catch {
      message.error('保存食物失败，请稍后重试');
    } finally {
      setFoodEditorSaving(false);
    }
  };

  const updatePlanTrainingItem = <K extends keyof PlanTrainingDraft>(index: number, key: K, value: PlanTrainingDraft[K]) => {
    setPlanDraft((prev) => ({
      ...prev,
      trainingItems: prev.trainingItems.map((item, itemIndex) => (itemIndex === index ? { ...item, [key]: value } : item)),
    }));
  };

  const updatePlanDietItem = <K extends keyof PlanDietDraft>(index: number, key: K, value: PlanDietDraft[K]) => {
    setPlanDraft((prev) => ({
      ...prev,
      dietItems: prev.dietItems.map((item, itemIndex) => (itemIndex === index ? { ...item, [key]: value } : item)),
    }));
  };

  const addPlanTrainingItem = () => {
    setPlanDraft((prev) => ({
      ...prev,
      trainingItems: [...prev.trainingItems, {
        sectionType: 'main',
        sortOrder: prev.trainingItems.length,
        note: '',
      }],
    }));
  };

  const addPlanDietItem = () => {
    setPlanDraft((prev) => ({
      ...prev,
      dietItems: [...prev.dietItems, {
        sortOrder: prev.dietItems.length,
        amount: 100,
        unit: 'g',
        note: '',
      }],
    }));
  };

  const removePlanTrainingItem = (index: number) => {
    setPlanDraft((prev) => ({
      ...prev,
      trainingItems: prev.trainingItems.filter((_, itemIndex) => itemIndex !== index),
    }));
  };

  const removePlanDietItem = (index: number) => {
    setPlanDraft((prev) => ({
      ...prev,
      dietItems: prev.dietItems.filter((_, itemIndex) => itemIndex !== index),
    }));
  };

  const renderFoodSearchResults = () => (
    <div className="ex-diet-food-search-results">
      {dietFoodLoading ? <div className="ex-diet-food-empty">搜索中...</div> : dietFoodOptions.map((food) => (
        <button type="button" key={food.id} className="ex-diet-food-option" onClick={() => handleAddFood(food)}>
          <div className="ex-diet-food-option-main">
            {food.imageUrl ? <img src={food.imageUrl} alt={food.name} className="ex-diet-food-thumb" /> : <div className="ex-diet-food-thumb ex-diet-food-thumb--placeholder">{food.name?.slice(0, 1) || '食'}</div>}
            <div>
              <div className="ex-diet-food-option-name">{food.name}</div>
              <div className="ex-diet-food-option-meta">
                {(food.category || '食物')} · {formatFoodBase(food.baseAmount, food.unit)} · {(food.calories || 0).toFixed(0)} kJ ≈ {kjToKcal(food.calories).toFixed(0)} kcal
              </div>
            </div>
          </div>
          <span>添加</span>
        </button>
      ))}
      {!dietFoodLoading && dietFoodOptions.length === 0 && (
        <div className="ex-diet-food-empty">没搜到食物，先换个关键词试试</div>
      )}
    </div>
  );

  const renderMyFoods = () => (
    <>
      {latestDietRecord ? (
        <div className="ex-diet-recent-card">
          <div className="ex-diet-recent-card__head">
            <div>
              <div className="ex-diet-recent-card__title">复制上一餐</div>
              <div className="ex-diet-recent-card__meta">
                {(latestDietRecord.mealType || '最近一餐')}
                {latestDietRecord.time ? ` · ${latestDietRecord.time}` : ''}
              </div>
            </div>
            <Button type="primary" ghost onClick={() => handleApplyRecentRecord(latestDietRecord)}>
              一键带入
            </Button>
          </div>
          <div className="ex-diet-recent-card__foods">
            {(latestDietRecord.items || []).slice(0, 4).map((item, index) => (
              <span key={`${item.foodItemId || item.name || 'food'}-${index}`} className="ex-diet-recent-card__food">
                {item.foodName || item.name || `食物 ${index + 1}`}
                {item.amount ? ` ${item.amount}${item.unit || 'g'}` : ''}
              </span>
            ))}
          </div>
        </div>
      ) : null}
      {quickRecentFoods.length > 0 ? (
        <>
          <div className="ex-diet-form-label">最近常吃</div>
          <div className="ex-diet-my-foods ex-diet-my-foods--recent">
            {quickRecentFoods.map((food) => (
              <button type="button" key={food.id} className="ex-diet-recent-food-chip" onClick={() => handleAddFood(food)}>
                {food.imageUrl ? <img src={food.imageUrl} alt={food.name} className="ex-diet-food-thumb" /> : <div className="ex-diet-food-thumb ex-diet-food-thumb--placeholder">{food.name?.slice(0, 1) || '食'}</div>}
                <span>{food.name}</span>
              </button>
            ))}
          </div>
        </>
      ) : null}
      <div className="ex-diet-food-actions">
        <div className="ex-diet-form-label" style={{ marginBottom: 0 }}>我的食物</div>
        <div className="ex-diet-food-action-buttons">
          <Button icon={<CameraOutlined />} onClick={() => {
            setFoodEditor(createDefaultFoodEditor());
            setFoodEditorOpen(true);
            window.setTimeout(() => foodRecognizeFileRef.current?.click(), 200);
          }}>
            拍照识别新建
          </Button>
          <Button type="dashed" icon={<PlusOutlined />} onClick={() => openFoodEditor()}>
            新建食物
          </Button>
        </div>
      </div>
      <div className="ex-diet-my-foods">
        {myFoods.map((food) => (
          <div key={food.id} className="ex-diet-my-food-chip">
            <button type="button" className="ex-diet-my-food-main" onClick={() => handleAddFood(food)} title={food.name}>
              {food.imageUrl ? <img src={food.imageUrl} alt={food.name} className="ex-diet-food-thumb" /> : <div className="ex-diet-food-thumb ex-diet-food-thumb--placeholder">{food.name?.slice(0, 1) || '食'}</div>}
              <span title={food.name}>{food.name}</span>
            </button>
            <Button type="text" size="small" onClick={() => openFoodEditor(food)}>
              编辑
            </Button>
          </div>
        ))}
        {myFoods.length === 0 && <div className="ex-diet-food-empty">你还没有自建食物，可以先新建常吃的那几样</div>}
      </div>
    </>
  );

  const renderSelectedFoods = () => (
    <>
      <label className="ex-diet-form-label">已选食物</label>
      <div className="ex-diet-selected-list">
        {dietSelectedFoods.map((item) => (
          <div key={item.food.id} className="ex-diet-selected-item">
            <div className="ex-diet-selected-main">
              <div className="ex-diet-selected-name">{item.food.name}</div>
              <div className="ex-diet-selected-meta">
                每 {formatFoodBase(item.food.baseAmount, item.food.unit)}: {(item.food.calories || 0).toFixed(0)} kJ ≈ {kjToKcal(item.food.calories).toFixed(0)} kcal / 蛋白 {(item.food.protein || 0).toFixed(1)}g
              </div>
            </div>
            <InputNumber
              value={item.amount}
              onChange={(value) => handleFoodAmountChange(item.food.id, typeof value === 'number' ? value : null)}
              min={1}
              addonAfter={item.food.unit || 'g'}
              style={{ width: 132 }}
            />
            <Button type="text" danger onClick={() => handleRemoveFood(item.food.id)}>删除</Button>
          </div>
        ))}
        {dietSelectedFoods.length === 0 && <div className="ex-diet-food-empty">还没添加食物</div>}
      </div>
    </>
  );

  const renderFoodEditorBody = () => (
    <div className="ex-food-editor">
      <div className="ex-food-editor-upload">
        <div className="ex-food-editor-cover">
          {foodEditor.imageUrl ? <img src={foodEditor.imageUrl} alt={foodEditor.name || '食物图片'} /> : <div className="ex-food-editor-cover-placeholder">食物图片</div>}
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <Upload
            accept="image/*"
            showUploadList={false}
            beforeUpload={(file) => {
              void handleFoodImageUpload(file);
              return false;
            }}
          >
            <Button icon={<UploadOutlined />}>上传图片</Button>
          </Upload>
          <Button icon={<CameraOutlined />} loading={foodRecognizing} onClick={() => foodRecognizeFileRef.current?.click()}>
            {foodRecognizing ? '识别中...' : '拍照识别'}
          </Button>
          <input
            ref={foodRecognizeFileRef}
            type="file"
            accept="image/*"
            capture="environment"
            style={{ display: 'none' }}
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) void handleFoodRecognize(file);
              e.target.value = '';
            }}
          />
        </div>
      </div>
      <div className="ex-food-editor-grid">
        <div className="ex-food-editor-field ex-food-editor-field--wide">
          <label className="ex-diet-form-label">名称</label>
          <Input value={foodEditor.name} onChange={(e) => handleFoodEditorChange('name', e.target.value)} maxLength={32} placeholder="比如：水煮鸡胸肉" />
        </div>
        <div className="ex-food-editor-field">
          <label className="ex-diet-form-label">分类</label>
          <AutoComplete
            value={foodEditor.category}
            options={FOOD_CATEGORY_OPTIONS}
            onChange={(value) => handleFoodEditorChange('category', value)}
            placeholder="选择或输入"
            allowClear
            filterOption={(inputValue, option) => (option?.value || '').includes(inputValue)}
          />
        </div>
        <div className="ex-food-editor-field">
          <label className="ex-diet-form-label">单位</label>
          <Input value={foodEditor.unit} onChange={(e) => handleFoodEditorChange('unit', e.target.value)} maxLength={12} placeholder="g / ml / 个" />
        </div>
        <div className="ex-food-editor-field">
          <label className="ex-diet-form-label">营养基准量</label>
          <InputNumber value={foodEditor.baseAmount} min={1} precision={2} onChange={(value) => handleFoodEditorChange('baseAmount', typeof value === 'number' ? value : 0)} style={{ width: '100%' }} />
        </div>
        <div className="ex-food-editor-field">
          <label className="ex-diet-form-label">能量(kJ)</label>
          <InputNumber value={foodEditor.calories} min={0} precision={2} onChange={(value) => handleFoodEditorChange('calories', typeof value === 'number' ? value : 0)} style={{ width: '100%' }} />
        </div>
        <div className="ex-food-editor-field">
          <label className="ex-diet-form-label">蛋白质(g)</label>
          <InputNumber value={foodEditor.protein} min={0} precision={2} onChange={(value) => handleFoodEditorChange('protein', typeof value === 'number' ? value : 0)} style={{ width: '100%' }} />
        </div>
        <div className="ex-food-editor-field">
          <label className="ex-diet-form-label">碳水(g)</label>
          <InputNumber value={foodEditor.carbs} min={0} precision={2} onChange={(value) => handleFoodEditorChange('carbs', typeof value === 'number' ? value : 0)} style={{ width: '100%' }} />
        </div>
        <div className="ex-food-editor-field">
          <label className="ex-diet-form-label">脂肪(g)</label>
          <InputNumber value={foodEditor.fat} min={0} precision={2} onChange={(value) => handleFoodEditorChange('fat', typeof value === 'number' ? value : 0)} style={{ width: '100%' }} />
        </div>
        <div className="ex-food-editor-field">
          <label className="ex-diet-form-label">膳食纤维(g)</label>
          <InputNumber value={foodEditor.fiber} min={0} precision={2} onChange={(value) => handleFoodEditorChange('fiber', typeof value === 'number' ? value : 0)} style={{ width: '100%' }} />
        </div>
      </div>
      <div className="ex-diet-selected-meta">录入时直接填包装上的 kJ，系统会按 1 kcal = 4.184 kJ 自动换算成热量展示。</div>
    </div>
  );

  const renderPlanEditorBody = () => (
    <div className="ex-plan-editor">
      <div className="ex-plan-template-toolbar">
        <div className="ex-plan-editor-field ex-plan-editor-field--wide">
          <label className="ex-diet-form-label">{planEditorMode === 'training' ? '训练模板' : '饮食模板'}</label>
          <Select
            allowClear
            placeholder={planEditorMode === 'training' ? '选择已有训练模板' : '选择已有饮食模板'}
            value={planDraft.id}
            options={(planEditorMode === 'training' ? trainingTemplates : dietTemplates).map((template) => ({
              value: template.id,
              label: planEditorMode === 'diet' && (template as API.UserDietTemplate).mealType
                ? `${(template as API.UserDietTemplate).mealType} · ${template.name}`
                : template.name,
            }))}
            onChange={(value) => handleSelectPlanTemplate(value)}
          />
        </div>
        <Button type="dashed" icon={<PlusOutlined />} onClick={handleCreateNewPlanTemplate}>
          新建模板
        </Button>
      </div>
      <div className="ex-plan-editor-head">
        <div className="ex-plan-editor-field ex-plan-editor-field--wide">
          <label className="ex-diet-form-label">计划名称</label>
          <Input
            value={planDraft.name}
            onChange={(e) => setPlanDraft((prev) => ({ ...prev, name: e.target.value }))}
            placeholder={planEditorMode === 'training' ? '比如：胸肩三头' : '比如：减脂早餐A'}
            maxLength={40}
          />
        </div>
        {planEditorMode === 'diet' ? (
          <div className="ex-plan-editor-field">
            <label className="ex-diet-form-label">餐次类型</label>
            <Select
              value={planDraft.mealType}
              options={MEAL_OPTIONS.map((meal) => ({ value: meal, label: meal }))}
              onChange={(value) => setPlanDraft((prev) => ({ ...prev, mealType: value }))}
            />
          </div>
        ) : null}
      </div>
      <div className="ex-plan-editor-tip">
        {planEditorMode === 'training'
          ? '一套训练模板只维护动作顺序和阶段，组数、次数、休息时间会直接沿用动作库默认配置。'
          : '一套饮食模板只对应一个餐次，早餐、午餐、晚餐要分别建模板，再到天模板里自由组合。'}
      </div>

      {planEditorMode === 'training' ? (
        <div className="ex-plan-editor-section">
          <div className="ex-plan-editor-section-head">
            <div className="ex-plan-editor-section-title">训练计划</div>
            <Button type="dashed" size="small" icon={<PlusOutlined />} onClick={addPlanTrainingItem}>
              添加动作
            </Button>
          </div>
          <div className="ex-plan-editor-list">
            {planDraft.trainingItems.length > 0 ? (
              <div className="ex-plan-editor-table-head ex-plan-editor-table-head--training">
                <span>阶段</span>
                <span>动作</span>
                <span>备注</span>
                <span>操作</span>
              </div>
            ) : null}
            {planDraft.trainingItems.map((item, index) => (
              <div key={`training-${index}`} className="ex-plan-editor-row">
                <Select value={item.sectionType} options={PLAN_SECTION_OPTIONS} onChange={(value) => updatePlanTrainingItem(index, 'sectionType', value)} />
                <Select
                  showSearch
                  placeholder="选择动作"
                  value={item.exerciseId}
                  optionFilterProp="label"
                  options={allExercises.map((exercise) => ({
                    value: exercise.id,
                    label: `${exercise.name} · ${MUSCLE_GROUP_LABELS[exercise.muscleGroup as MuscleGroup] || exercise.muscleGroup}`,
                  }))}
                  onChange={(value) => updatePlanTrainingItem(index, 'exerciseId', value)}
                />
                <Input
                  value={item.note}
                  onChange={(e) => updatePlanTrainingItem(index, 'note', e.target.value)}
                  placeholder="备注（可选）"
                />
                <Button type="text" danger onClick={() => removePlanTrainingItem(index)}>删除</Button>
              </div>
            ))}
            {planDraft.trainingItems.length === 0 && <div className="ex-plan-empty">还没添加训练动作</div>}
          </div>
        </div>
      ) : (
        <div className="ex-plan-editor-section">
          <div className="ex-plan-editor-section-head">
            <div className="ex-plan-editor-section-title">饮食计划</div>
            <Button type="dashed" size="small" icon={<PlusOutlined />} onClick={addPlanDietItem}>
              添加食物
            </Button>
          </div>
          <div className="ex-plan-editor-list">
            {planDraft.dietItems.length > 0 ? (
              <div className="ex-plan-editor-table-head ex-plan-editor-table-head--diet">
                <span>食物</span>
                <span>分量</span>
                <span>单位</span>
                <span>备注</span>
                <span>操作</span>
              </div>
            ) : null}
            {planDraft.dietItems.map((item, index) => (
              <div key={`diet-${index}`} className="ex-plan-editor-row ex-plan-editor-row--diet">
                <Select
                  showSearch
                  placeholder="输入关键词搜索食物"
                  value={item.foodItemId}
                  filterOption={false}
                  onSearch={(val) => setPlanFoodKeyword(val)}
                  options={planFoodLibrary.map((food) => ({
                    value: food.id,
                    label: `${food.name} · ${food.category || '食物'}`,
                  }))}
                  onChange={(value) => {
                    const selected = planFoodLibrary.find((food) => food.id === value);
                    updatePlanDietItem(index, 'foodItemId', value);
                    if (selected?.unit) {
                      updatePlanDietItem(index, 'unit', selected.unit);
                    }
                    if (selected?.baseAmount) {
                      updatePlanDietItem(index, 'amount', selected.baseAmount);
                    }
                  }}
                />
                <InputNumber min={1} value={item.amount} onChange={(value) => updatePlanDietItem(index, 'amount', typeof value === 'number' ? value : 1)} />
                <Input value={item.unit} onChange={(e) => updatePlanDietItem(index, 'unit', e.target.value)} placeholder="单位" />
                <Input
                  value={item.note}
                  onChange={(e) => updatePlanDietItem(index, 'note', e.target.value)}
                  placeholder="备注（可选）"
                />
                <Button type="text" danger onClick={() => removePlanDietItem(index)}>删除</Button>
              </div>
            ))}
            {planDraft.dietItems.length === 0 && <div className="ex-plan-empty">还没添加饮食食物</div>}
          </div>
        </div>
      )}
    </div>
  );

  const dietPreview = useMemo(() => {
    return dietSelectedFoods.reduce((acc, item) => {
      const baseAmount = item.food.baseAmount || 100;
      const ratio = item.amount / baseAmount;
      acc.calories += kjToKcal(item.food.calories) * ratio;
      acc.protein += (item.food.protein || 0) * ratio;
      acc.carbs += (item.food.carbs || 0) * ratio;
      acc.fat += (item.food.fat || 0) * ratio;
      acc.fiber += (item.food.fiber || 0) * ratio;
      return acc;
    }, { calories: 0, protein: 0, carbs: 0, fat: 0, fiber: 0 });
  }, [dietSelectedFoods]);

  return (
    <div className="ex-page-shell">
      <div className="ex-page">
        <div className="ex-today-strip">
          <div className="ex-today-left">
            <div className="ex-today-card" onClick={() => setActivePanel('training')}>
              <div className="ex-today-card-label">今日安排</div>
              <div className="ex-today-card-title">训练计划</div>
              <div className="ex-today-card-desc">
                {planLoading
                  ? '正在读取当前计划...'
                  : trainingSchedule
                    ? todayTrainingItems.length
                      ? `${trainingSchedule.name} · Day ${trainingSchedule.todayIndex || 1} · ${todayTrainingTemplateName || ''}${todayTrainingTemplateName ? ' · ' : ''}${todayTrainingItems.length}个动作`
                      : `${trainingSchedule.name} · Day ${trainingSchedule.todayIndex || 1} · ${todayTrainingTemplateName || '未安排'}`
                    : '今天还没有可用的训练安排'}
              </div>
              <button className="ex-card-gear" onClick={(e) => { e.stopPropagation(); openManager('training'); }} type="button" aria-label="训练模板管理"><SettingOutlined /></button>
            </div>
            <div className="ex-today-card" onClick={() => setActivePanel('diet')}>
              <div className="ex-today-card-label">今日饮食</div>
              <div className="ex-today-card-title">全天安排</div>
              <div className="ex-today-card-desc">
                {planLoading
                  ? '正在读取当前计划...'
                  : dietSchedule
                    ? `${dietSchedule.name} · Day ${dietSchedule.todayIndex || 1}${todayDietDayTemplateName ? ` · ${todayDietDayTemplateName}` : ''}${todayDietMealLabel ? ` · 当前${todayDietMealLabel}` : ''}`
                    : '未设置饮食循环'}
              </div>
              <button className="ex-card-gear" onClick={(e) => { e.stopPropagation(); openManager('diet'); }} type="button" aria-label="饮食模板管理"><SettingOutlined /></button>
            </div>
          </div>
          {!hasActiveExerciseFilter && (
          <div className={`ex-today-rec${recExercise ? '' : ' ex-today-rec--placeholder'}`}>
            <div className="ex-today-rec-header">
              <span className="ex-today-rec-title">今日推荐</span>
              <div className="ex-today-rec-head-actions">
                <span className="ex-today-rec-badge">第 {calcUserDay(currentUser?.createTime)} 天</span>
                {recExercise && (
                  <span className="ex-today-rec-refresh" onClick={handleRefreshRec}><ReloadOutlined /></span>
                )}
              </div>
            </div>
            {recExercise ? (
              <div className="ex-today-rec-body">
                <div className="ex-today-rec-model">
                  <React.Suspense fallback={<div style={{ width: 48, height: 48, borderRadius: '50%', opacity: 0.15 }} />}>
                    <Model
                      type={recIsPosterior ? 'posterior' : 'anterior'}
                      data={recHighlight}
                      highlightedColors={HL_COLORS}
                      style={{ width: '100%', height: '100%' }}
                    />
                  </React.Suspense>
                  {recExercise.muscleGroup && (
                    <span className="ex-today-rec-model-label">
                      {MUSCLE_GROUP_LABELS[recExercise.muscleGroup] || recExercise.muscleGroup}
                    </span>
                  )}
                </div>
                <div className="ex-today-rec-info">
                  <div className="ex-today-rec-name">{recExercise.name}</div>
                  <div className="ex-today-rec-tags">
                    <span className="ex-today-rec-pill">{DIFF_LABELS[recExercise.difficulty] || recExercise.difficulty}</span>
                    {recExercise.equipment && (
                      <span className="ex-today-rec-pill">{recExercise.equipment}</span>
                    )}
                  </div>
                  {recExercise.tips && (
                    <div className="ex-today-rec-tips">{recExercise.tips}</div>
                  )}
                  <div className="ex-today-rec-btns">
                    <button className="ex-today-rec-fav" onClick={() => handleToggleFavorite(recExercise.id)}>
                      {favoriteIds.includes(recExercise.id) ? <HeartFilled style={{ color: '#164a41' }} /> : <HeartOutlined />} 收藏
                    </button>
                    <button className="ex-today-rec-goto" onClick={() => history.push(buildExerciseDetailUrl(recExercise))}>
                      查看详情 <RightOutlined style={{ fontSize: 12 }} />
                    </button>
                  </div>
                </div>
              </div>
            ) : (
              <div className="ex-today-rec-loading" aria-hidden="true">
                <div className="ex-today-rec-loading-model" />
                <div className="ex-today-rec-loading-lines">
                  <span className="ex-today-rec-loading-line ex-today-rec-loading-line--lg" />
                  <span className="ex-today-rec-loading-line ex-today-rec-loading-line--md" />
                  <span className="ex-today-rec-loading-line ex-today-rec-loading-line--sm" />
                </div>
              </div>
            )}
          </div>
          )}
        </div>

        <ExerciseExplorerFilters
          searchValue={searchText}
          searchPlaceholder="搜索动作名称、器械、难度..."
          onSearchChange={setSearchText}
          rows={filterRows}
        />

        {!loading && exercises.length > 0 && (
          <div className="ex-result-bar">
            <div className="ex-result-count" key={`count-${displayedExercises.length}`}>
              共 <strong>{displayedExercises.length}</strong> 个动作
            </div>
            <div className="ex-result-progress">
              已显示 <strong>{visibleExercises.length}</strong> / {displayedExercises.length}
            </div>
          </div>
        )}

        <div className="ex-content-shell">
        <Spin spinning={loading}>
          {displayedExercises.length > 0 ? (
            <>
              <div className="ex-grid">
                {visibleExercises.map((exercise, index) => (
                  <ExerciseCard
                    key={exercise.id}
                    exercise={exercise}
                    index={index}
                    isFavorited={favoriteIds.includes(exercise.id)}
                    onToggleFavorite={handleToggleFavorite}
                    onVideoPlay={(url) => setVideoUrl(url)}
                    onStartTraining={setTimerExercise}
                  />
                ))}
              </div>
              {hasMoreExercises && (
                <div className="ex-load-more" ref={loadMoreRef} aria-hidden="true">
                  <div className="ex-load-more-line" />
                  <span className="ex-load-more-text">继续下滑加载更多动作</span>
                </div>
              )}
            </>
          ) : !loading && (
            <div className="ex-empty">
              <div className="ex-empty-inner">
                <div className="ex-empty-illust ex-empty-float">
                  <FireOutlined />
                </div>
                <div className="ex-empty-title">{hasActiveExerciseFilter ? '当前筛选条件下没有动作' : '暂无该肌群的动作数据'}</div>
                <div className="ex-empty-desc">{hasActiveExerciseFilter ? '换个肌群、器械、难度或关键词试试' : '试试选择其他肌群分类'}</div>
              </div>
            </div>
          )}
        </Spin>
        </div>
      </div>

      <Modal
        open={!!videoUrl}
        footer={null}
        onCancel={() => setVideoUrl(null)}
        width={800}
        destroyOnClose
        className="app-modal-video ex-video-modal"
        styles={{ body: { padding: 0, display: 'flex', justifyContent: 'center', background: '#000' } }}
      >
        {videoUrl && (
          <video src={videoUrl} controls autoPlay style={{ width: '100%', maxHeight: '80vh' }} />
        )}
      </Modal>

      <TrainingTimer open={!!timerExercise} exercise={timerExercise} onClose={() => setTimerExercise(null)} />
      {(isMobilePanel ? todaySheet.mounted : !!activePanel) && renderTodayPanel()}

      {!isMobilePanel && managerOpen && (
        <div className="ex-today-modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) setManagerOpen(false); }}>
          <div className="ex-today-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ex-today-modal-header">
              <div className="ex-today-modal-title-row">
                <div className="ex-today-modal-title">
                  <span className="ex-today-modal-icon"><SettingOutlined /></span>
                  {managerMode === 'training' ? '训练模板管理' : '饮食模板管理'}
                </div>
                <div className="ex-today-modal-title-actions">
                  <button className="ex-today-modal-close" onClick={() => setManagerOpen(false)} type="button">&#10005;</button>
                </div>
              </div>
            </div>
            <div className="ex-today-modal-body">
              {renderManagerBody()}
            </div>
          </div>
        </div>
      )}
      {managerSheet.mounted && (
        <div className="ex-diet-sheet" style={managerSheet.sheetStyle}>
          <div className="ex-diet-sheet-handle" {...managerSheet.dragHandleProps} />
          <div className="ex-diet-sheet-head" {...managerSheet.dragHandleProps}>
            <div className="ex-diet-sheet-title-row">
              <div className="ex-diet-sheet-title">{managerMode === 'training' ? '训练模板管理' : '饮食模板管理'}</div>
            </div>
            <div className="ex-diet-sheet-subtitle">
              {managerMode === 'training' ? '管理训练模板和训练循环' : '管理饮食模板、天模板和饮食循环'}
            </div>
          </div>
          <div className="ex-diet-sheet-body">
            {renderManagerBody()}
          </div>
        </div>
      )}
      {!isMobilePanel && planEditorOpen && (
        <div className="ex-today-modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) setPlanEditorOpen(false); }}>
          <div className="ex-today-modal ex-plan-editor-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ex-today-modal-header ex-today-modal-header--training">
              <div className="ex-today-modal-title-row">
                <div className="ex-today-modal-title">
                  <span className="ex-today-modal-icon"><CalendarOutlined /></span>
                  {planEditorMode === 'training' ? '训练模板库' : '饮食单餐模板库'}
                </div>
                <div className="ex-today-modal-title-actions">
                  <button className="ex-today-modal-close" onClick={() => setPlanEditorOpen(false)} type="button">✕</button>
                </div>
              </div>
              <div className="ex-today-modal-subline">
                <div className="ex-today-modal-subtitle">
                  {planEditorMode === 'training' ? '先维护多套训练模板，再到训练循环里安排每个 Day 使用哪一套。' : '先维护多套单餐模板，再到天模板和饮食循环里自由组合。'}
                </div>
              </div>
            </div>
            <div className="ex-today-modal-body">
              {renderPlanEditorBody()}
            </div>
            <div className="ex-today-modal-footer">
              {planDraft.id ? (
                <Button danger className="ex-dialog-footer-btn ex-dialog-footer-btn--danger" loading={planEditorSaving} onClick={() => void handleDeletePlanDraft()}>
                  删除模板
                </Button>
              ) : <span />}
              <Button type="primary" className="ex-dialog-footer-btn ex-dialog-footer-btn--primary" loading={planEditorSaving} onClick={() => void handleSavePlanDraft()}>
                保存模板
              </Button>
            </div>
          </div>
        </div>
      )}
      {planEditorSheet.mounted && (
        <div className="ex-diet-sheet" style={planEditorSheet.sheetStyle}>
          <div className="ex-diet-sheet-handle" {...planEditorSheet.dragHandleProps} />
          <div className="ex-diet-sheet-head" {...planEditorSheet.dragHandleProps}>
            <div className="ex-diet-sheet-title">{planEditorMode === 'training' ? '训练模板库' : '饮食单餐模板库'}</div>
            <div className="ex-diet-sheet-subtitle">
              {planEditorMode === 'training' ? '先建模板，再安排训练循环' : '先建单餐模板，再组合天模板'}
            </div>
          </div>
          <div className="ex-diet-sheet-body">
            {renderPlanEditorBody()}
          </div>
          <div className="ex-diet-sheet-foot">
            <div style={{ display: 'flex', gap: 8 }}>
              <Button className="ex-dialog-footer-btn" onClick={() => planEditorSheet.requestClose()}>取消</Button>
              {planDraft.id ? <Button danger className="ex-dialog-footer-btn ex-dialog-footer-btn--danger" onClick={() => void handleDeletePlanDraft()}>删除</Button> : null}
            </div>
            <Button type="primary" className="ex-dialog-footer-btn ex-dialog-footer-btn--primary" loading={planEditorSaving} onClick={() => void handleSavePlanDraft()}>保存模板</Button>
          </div>
        </div>
      )}
      {!isMobilePanel && planScheduleOpen && (
        <div className="ex-today-modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) setPlanScheduleOpen(false); }}>
          <div className="ex-today-modal ex-plan-editor-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ex-today-modal-header ex-today-modal-header--training">
              <div className="ex-today-modal-title-row">
                <div className="ex-today-modal-title">
                  <span className="ex-today-modal-icon"><CalendarOutlined /></span>
                  {planEditorMode === 'training' ? '安排训练循环' : '安排饮食循环'}
                </div>
                <div className="ex-today-modal-title-actions">
                  <button className="ex-today-modal-close" onClick={() => setPlanScheduleOpen(false)} type="button">✕</button>
                </div>
              </div>
              <div className="ex-today-modal-subline">
                <div className="ex-today-modal-subtitle">
                  {planEditorMode === 'training' ? '选择每个 Day 使用哪套训练模板。' : '选择每个 Day 使用哪套饮食模板。'}
                </div>
              </div>
            </div>
            <div className="ex-today-modal-body">
              {renderPlanScheduleBody()}
            </div>
            <div className="ex-today-modal-footer">
              <Button className="ex-dialog-footer-btn" onClick={() => setPlanScheduleOpen(false)}>取消</Button>
              <Button type="primary" className="ex-dialog-footer-btn ex-dialog-footer-btn--primary" loading={planEditorSaving} onClick={() => void handleSavePlanSchedule()}>
                保存安排
              </Button>
            </div>
          </div>
        </div>
      )}
      {planScheduleSheet.mounted && (
        <div className="ex-diet-sheet" style={planScheduleSheet.sheetStyle}>
          <div className="ex-diet-sheet-handle" {...planScheduleSheet.dragHandleProps} />
          <div className="ex-diet-sheet-head" {...planScheduleSheet.dragHandleProps}>
            <div className="ex-diet-sheet-title">{planEditorMode === 'training' ? '安排训练循环' : '安排饮食循环'}</div>
            <div className="ex-diet-sheet-subtitle">{planEditorMode === 'training' ? '选择每个 Day 对应的训练模板' : '先创建天模板，再安排每天用哪个'}</div>
          </div>
          <div className="ex-diet-sheet-body">
            {renderPlanScheduleBody()}
          </div>
          <div className="ex-diet-sheet-foot">
            <Button className="ex-dialog-footer-btn" onClick={() => planScheduleSheet.requestClose()}>取消</Button>
            <Button type="primary" className="ex-dialog-footer-btn ex-dialog-footer-btn--primary" loading={planEditorSaving} onClick={() => void handleSavePlanSchedule()}>保存安排</Button>
          </div>
        </div>
      )}

      {!isMobilePanel && dayTemplateEditorOpen && (
        <div className="ex-today-modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) setDayTemplateEditorOpen(false); }}>
          <div className="ex-today-modal ex-plan-editor-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ex-today-modal-header">
              <div className="ex-today-modal-title-row">
                <div className="ex-today-modal-title">{dayTemplateDraft.id ? '编辑天模板' : '新建天模板'}</div>
                <button className="ex-today-modal-close" onClick={() => setDayTemplateEditorOpen(false)} type="button">✕</button>
              </div>
              <div className="ex-today-modal-subline">
                <div className="ex-today-modal-subtitle">为每天的每餐选择一个饮食模板</div>
              </div>
            </div>
            <div className="ex-today-modal-body">{renderDayTemplateEditor()}</div>
            <div className="ex-today-modal-footer">
              <Button className="ex-dialog-footer-btn" onClick={() => setDayTemplateEditorOpen(false)}>取消</Button>
              <Button type="primary" className="ex-dialog-footer-btn ex-dialog-footer-btn--primary" loading={dayTemplateSaving} onClick={() => void handleSaveDayTemplate()}>保存</Button>
            </div>
          </div>
        </div>
      )}

      {dayTemplateSheet.mounted && (
        <div className="ex-diet-sheet" style={dayTemplateSheet.sheetStyle}>
          <div className="ex-diet-sheet-handle" {...dayTemplateSheet.dragHandleProps} />
          <div className="ex-diet-sheet-head" {...dayTemplateSheet.dragHandleProps}>
            <div className="ex-diet-sheet-title">{dayTemplateDraft.id ? '编辑天模板' : '新建天模板'}</div>
            <div className="ex-diet-sheet-subtitle">为每天的每餐选择一个饮食模板</div>
          </div>
          <div className="ex-diet-sheet-body">{renderDayTemplateEditor()}</div>
          <div className="ex-diet-sheet-foot">
            <Button className="ex-dialog-footer-btn" onClick={() => dayTemplateSheet.requestClose()}>取消</Button>
            <Button type="primary" className="ex-dialog-footer-btn ex-dialog-footer-btn--primary" loading={dayTemplateSaving} onClick={() => void handleSaveDayTemplate()}>保存</Button>
          </div>
        </div>
      )}

      {!isMobilePanel && dietRecordOpen && (
        <div className="ex-today-modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) setDietRecordOpen(false); }}>
          <div className="ex-today-modal ex-plan-editor-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ex-today-modal-header ex-today-modal-header--diet">
              <div className="ex-today-modal-title-row">
                <div className="ex-today-modal-title">记录饮食</div>
                <button className="ex-today-modal-close" onClick={() => setDietRecordOpen(false)} type="button">✕</button>
              </div>
              <div className="ex-today-modal-subline">
                <div className="ex-today-modal-subtitle">选择当前餐次并记录你这次实际吃了什么</div>
              </div>
            </div>
            <div className="ex-today-modal-body">
              <div className="ex-diet-form">
                <label className="ex-diet-form-label">餐次</label>
                <Select
                  value={dietRecordMealType}
                  onChange={(v) => setDietRecordMealType(v)}
                  options={MEAL_OPTIONS.map(m => ({ value: m, label: m }))}
                  style={{ width: '100%', marginBottom: 16 }}
                />
                {renderMyFoods()}
                <label className="ex-diet-form-label">搜索食物</label>
                <Input
                  value={dietFoodKeyword}
                  onChange={(e) => setDietFoodKeyword(e.target.value)}
                  placeholder="输入食物名搜索，如鸡胸肉、燕麦、香蕉"
                  maxLength={60}
                />
                {renderFoodSearchResults()}
                {renderSelectedFoods()}
                <div className="ex-diet-nutrition-preview">
                  <div>热量 {dietPreview.calories.toFixed(0)} kcal</div>
                  <div>蛋白质 {dietPreview.protein.toFixed(1)} g</div>
                  <div>碳水 {dietPreview.carbs.toFixed(1)} g</div>
                  <div>脂肪 {dietPreview.fat.toFixed(1)} g</div>
                  <div>纤维 {dietPreview.fiber.toFixed(1)} g</div>
                </div>
                <label className="ex-diet-form-label">备注（可选）</label>
                <Input.TextArea
                  value={dietRecordNote}
                  onChange={(e) => setDietRecordNote(e.target.value)}
                  rows={3}
                  placeholder="比如：训练后补充、少糖、七分饱"
                  maxLength={120}
                  showCount
                  styles={{ textarea: { position: 'relative' }, count: { position: 'absolute', bottom: 6, right: 10, color: 'var(--text-3)', fontSize: 12 } }}
                />
              </div>
            </div>
            <div className="ex-today-modal-footer">
              <Button className="ex-dialog-footer-btn" onClick={() => setDietRecordOpen(false)}>取消</Button>
              <Button type="primary" className="ex-dialog-footer-btn ex-dialog-footer-btn--primary" loading={dietRecordSaving} onClick={handleSaveDietRecord}>
                保存
              </Button>
            </div>
          </div>
        </div>
      )}

      {dietRecordSheet.mounted && (
        <div className="ex-diet-sheet" style={dietRecordSheet.sheetStyle}>
          <div className="ex-diet-sheet-handle" {...dietRecordSheet.dragHandleProps} />
          <div className="ex-diet-sheet-head" {...dietRecordSheet.dragHandleProps}>
            <div className="ex-diet-sheet-title">记录饮食</div>
            <div className="ex-diet-sheet-subtitle">{currentMeal}</div>
          </div>
          <div className="ex-diet-sheet-body">
            <div className="ex-diet-form">
              <label className="ex-diet-form-label">餐次</label>
              <Select
                value={dietRecordMealType}
                onChange={(v) => setDietRecordMealType(v)}
                options={MEAL_OPTIONS.map(m => ({ value: m, label: m }))}
                style={{ width: '100%', marginBottom: 16 }}
              />
              {renderMyFoods()}
              <label className="ex-diet-form-label">搜索食物</label>
              <Input
                value={dietFoodKeyword}
                onChange={(e) => setDietFoodKeyword(e.target.value)}
                placeholder="输入食物名搜索，如鸡胸肉、燕麦、香蕉"
                maxLength={60}
              />
              {renderFoodSearchResults()}
              {renderSelectedFoods()}
              <div className="ex-diet-nutrition-preview">
                <div>热量 {dietPreview.calories.toFixed(0)} kcal</div>
                <div>蛋白质 {dietPreview.protein.toFixed(1)} g</div>
                <div>碳水 {dietPreview.carbs.toFixed(1)} g</div>
                <div>脂肪 {dietPreview.fat.toFixed(1)} g</div>
                <div>纤维 {dietPreview.fiber.toFixed(1)} g</div>
              </div>
              <label className="ex-diet-form-label">备注（可选）</label>
              <Input.TextArea
                value={dietRecordNote}
                onChange={(e) => setDietRecordNote(e.target.value)}
                rows={4}
                placeholder="比如：训练后补充、少糖、七分饱"
                maxLength={120}
                showCount
                styles={{ textarea: { position: 'relative' }, count: { position: 'absolute', bottom: 6, right: 10, color: 'var(--text-3)', fontSize: 12 } }}
              />
            </div>
          </div>
          <div className="ex-diet-sheet-foot">
            <Button className="ex-dialog-footer-btn" onClick={() => dietRecordSheet.requestClose()}>取消</Button>
            <Button type="primary" className="ex-dialog-footer-btn ex-dialog-footer-btn--primary" loading={dietRecordSaving} onClick={handleSaveDietRecord}>
              保存
            </Button>
          </div>
        </div>
      )}
      {!isMobilePanel && foodEditorOpen && (
        <div className="ex-today-modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) closeFoodEditor(); }}>
          <div className="ex-today-modal ex-plan-editor-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ex-today-modal-header ex-today-modal-header--diet">
              <div className="ex-today-modal-title-row">
                <div className="ex-today-modal-title">{foodEditor.id ? '编辑自建食物' : '新建食物'}</div>
                <button className="ex-today-modal-close" onClick={closeFoodEditor} type="button">✕</button>
              </div>
              <div className="ex-today-modal-subline">
                <div className="ex-today-modal-subtitle">保存后会回到饮食记录，方便你继续添加这一餐</div>
              </div>
            </div>
            <div className="ex-today-modal-body">
              {renderFoodEditorBody()}
            </div>
            <div className="ex-today-modal-footer">
              <Button className="ex-dialog-footer-btn" onClick={closeFoodEditor}>取消</Button>
              <Button type="primary" className="ex-dialog-footer-btn ex-dialog-footer-btn--primary" loading={foodEditorSaving} onClick={handleSaveFoodItem}>
                保存食物
              </Button>
            </div>
          </div>
        </div>
      )}
      {foodEditorSheet.mounted && (
        <div className="ex-diet-sheet" style={foodEditorSheet.sheetStyle}>
          <div className="ex-diet-sheet-handle" {...foodEditorSheet.dragHandleProps} />
          <div className="ex-diet-sheet-head" {...foodEditorSheet.dragHandleProps}>
            <div className="ex-diet-sheet-title-row">
              <Button type="text" className="ex-diet-sheet-back" onClick={closeFoodEditor}>返回</Button>
              <div>
                <div className="ex-diet-sheet-title">{foodEditor.id ? '编辑自建食物' : '新建食物'}</div>
                <div className="ex-diet-sheet-subtitle">保存后会回到上一层</div>
              </div>
            </div>
          </div>
          <div className="ex-diet-sheet-body">
            {renderFoodEditorBody()}
          </div>
          <div className="ex-diet-sheet-foot">
            <Button className="ex-dialog-footer-btn" onClick={closeFoodEditor}>取消</Button>
            <Button type="primary" className="ex-dialog-footer-btn ex-dialog-footer-btn--primary" loading={foodEditorSaving} onClick={handleSaveFoodItem}>
              保存食物
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};

export default Exercises;
