export type MuscleGroup = 'all' | 'chest' | 'back' | 'shoulders' | 'arms' | 'legs' | 'core';

export type ExerciseFilterOption = {
  key: string;
  label: string;
  color?: string;
};

export const MUSCLE_GROUP_LABELS: Record<string, string> = {
  chest: '胸部',
  back: '背部',
  shoulders: '肩部',
  arms: '手臂',
  legs: '腿部',
  core: '核心',
};

export const MUSCLE_GROUP_COLORS: Record<string, string> = {
  chest: '#e8654a',
  back: '#3b8eea',
  shoulders: '#a87bff',
  arms: '#f5a623',
  legs: '#36b37e',
  core: '#ff6b81',
};

export const MUSCLE_GROUP_FILTERS: { key: MuscleGroup; label: string }[] = [
  { key: 'all', label: '全部' },
  { key: 'chest', label: MUSCLE_GROUP_LABELS.chest },
  { key: 'back', label: MUSCLE_GROUP_LABELS.back },
  { key: 'shoulders', label: MUSCLE_GROUP_LABELS.shoulders },
  { key: 'arms', label: MUSCLE_GROUP_LABELS.arms },
  { key: 'legs', label: MUSCLE_GROUP_LABELS.legs },
  { key: 'core', label: MUSCLE_GROUP_LABELS.core },
];

export const DIFFICULTY_OPTIONS: ExerciseFilterOption[] = [
  { key: 'all', label: '全部', color: '#164A41' },
  { key: '初级', label: '初级', color: '#52c41a' },
  { key: '中级', label: '中级', color: '#faad14' },
  { key: '高级', label: '高级', color: '#ff4d4f' },
];

export const GROUP_TARGET_MUSCLES: Record<string, string[]> = {
  chest: ['胸大肌', '三角肌前束', '肱三头肌'],
  back: ['背阔肌', '斜方肌', '菱形肌', '肱二头肌'],
  shoulders: ['三角肌前束', '三角肌中束', '三角肌后束', '斜方肌'],
  arms: ['肱二头肌', '肱三头肌', '前臂肌群'],
  legs: ['股四头肌', '腘绳肌', '臀大肌', '小腿三头肌'],
  core: ['腹直肌', '腹外斜肌', '腹内斜肌', '腹横肌'],
};

export const EQUIPMENT_ORDER = ['自重', '哑铃', '杠铃', '弹力带', '绳索', '器械', '壶铃', 'TRX', '瑜伽垫'];

export const getEquipmentOptions = (list: API.Exercise[]): ExerciseFilterOption[] => {
  const unique = Array.from(new Set(list.map((item) => item.equipment?.trim()).filter(Boolean) as string[]));
  const sorted = unique.sort((a, b) => {
    const ia = EQUIPMENT_ORDER.indexOf(a);
    const ib = EQUIPMENT_ORDER.indexOf(b);
    if (ia === -1 && ib === -1) return a.localeCompare(b, 'zh-CN');
    if (ia === -1) return 1;
    if (ib === -1) return -1;
    return ia - ib;
  });

  return [{ key: 'all', label: '全部', color: '#164A41' }, ...sorted.map((item) => ({ key: item, label: item }))];
};
