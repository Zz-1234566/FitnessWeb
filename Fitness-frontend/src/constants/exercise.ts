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

export const EQUIPMENT_ORDER = ['无器械', '哑铃', '杠铃', '弹力带', '绳索', '壶铃', 'TRX', '瑜伽垫',
  '平凳', '上斜凳', '下斜凳', '坐姿凳', '卧推架', '推胸机', '蝴蝶机',
  '划船机', '高位下拉器', '龙门架', '深蹲架', '史密斯机',
  '腿举机', '倒蹬机', '腿屈伸机', '腿弯举机', '哈克深蹲机',
  '提踵机', '侧平举机', '推肩机', '臂屈伸机', '弯举机', '下压机',
  '卷腹机', '牧师椅', '髋外展机', '罗马椅',
  '单杠', '双杠', 'T杠', 'V把',
  '战绳架', '战绳', '药球', '跳绳'];

/** 标准化难度字段，兼容中英文和各种大小写 */
export const normalizeExerciseDifficulty = (value?: string): string => {
  const text = (value || '').trim().toLowerCase();
  if (!text) return '';
  if (text === '初级' || text === 'easy' || text === 'beginner') return '初级';
  if (text === '中级' || text === 'medium' || text === 'intermediate') return '中级';
  if (text === '高级' || text === 'hard' || text === 'advanced') return '高级';
  return (value || '').trim();
};

/** 标准化肌群字段，兼容中英文 */
export const normalizeExerciseMuscleGroup = (value?: string): MuscleGroup | '' => {
  const text = (value || '').trim().toLowerCase();
  switch (text) {
    case 'chest':
    case '胸部':
      return 'chest';
    case 'back':
    case '背部':
      return 'back';
    case 'shoulders':
    case '肩部':
      return 'shoulders';
    case 'arms':
    case '手臂':
      return 'arms';
    case 'legs':
    case '腿部':
      return 'legs';
    case 'core':
    case '核心':
      return 'core';
    default:
      return '';
  }
};

/** 解析 equipment 字段（JSON 数组或旧格式字符串） */
export const parseEquipment = (equipment: string): string[] => {
  if (!equipment) return [];
  const trimmed = equipment.trim();
  try {
    const parsed = JSON.parse(trimmed);
    if (Array.isArray(parsed)) return parsed.filter(Boolean);
  } catch {
    // 旧格式兼容：单个字符串
  }
  return trimmed ? [trimmed] : [];
};

export const getEquipmentOptions = (list: API.Exercise[]): ExerciseFilterOption[] => {
  const allEquipment = list.flatMap((item) => parseEquipment(item.equipment || ''));
  const unique = Array.from(new Set(allEquipment)).filter(Boolean);
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
