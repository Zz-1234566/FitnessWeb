import { Spin, Modal } from 'antd';
import {
  AimOutlined,
  FireOutlined,
} from '@ant-design/icons';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Model, { type IExerciseData, type IMuscleStats, type Muscle } from 'react-body-highlighter';
import { useModel } from '@umijs/max';
import { getExercisesByGroup, getSummaryNotification } from '@/services/ant-design-pro/api';
import ExerciseCard from '@/components/ExerciseCard';
import TrainingTimer from '@/components/TrainingTimer';
import TypewriterGreeting, { buildGreetingMessages } from '@/components/TypewriterGreeting';
import useEnsureCurrentUser from '@/hooks/useEnsureCurrentUser';
import { useExerciseInteractions } from '@/hooks/useExerciseInteractions';
import { MUSCLE_GROUP_LABELS, MUSCLE_GROUP_COLORS } from '@/constants/exercise';
import { getGreeting } from '@/utils/greeting';
import './welcome.less';

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

/* ===== 人体模型多边形映射 ===== */
const ANTERIOR_POLYGON_MAP: Muscle[] = [
  'chest', 'chest', 'obliques', 'obliques', 'abs', 'abs',
  'biceps', 'biceps', 'triceps', 'triceps', 'neck', 'neck',
  'front-deltoids', 'front-deltoids', 'head',
  'adductor', 'adductor',
  'quadriceps', 'quadriceps', 'quadriceps', 'quadriceps', 'quadriceps', 'quadriceps',
  'knees', 'knees', 'calves', 'calves', 'calves', 'calves',
  'forearm', 'forearm', 'forearm', 'forearm',
];

const POSTERIOR_POLYGON_MAP: Muscle[] = [
  'head', 'trapezius', 'trapezius', 'back-deltoids', 'back-deltoids',
  'upper-back', 'upper-back', 'triceps', 'triceps', 'triceps', 'triceps',
  'lower-back', 'lower-back', 'forearm', 'forearm', 'forearm', 'forearm',
  'gluteal', 'gluteal', 'abductors', 'abductors',
  'hamstring', 'hamstring', 'hamstring', 'hamstring', 'knees', 'knees',
  'calves', 'calves', 'calves', 'calves', 'left-soleus', 'right-soleus',
];

const getMuscleFromPolygon = (modelType: 'anterior' | 'posterior', index: number): Muscle | null =>
  (modelType === 'anterior' ? ANTERIOR_POLYGON_MAP : POSTERIOR_POLYGON_MAP)[index] ?? null;

/* ===== 共用常量 ===== */
const HL_COLORS = ['rgba(255, 80, 80, 0.8)', 'rgba(255, 80, 80, 0.8)'];

const HOME_FRIENDLY_LINES = [
  '今天也来了，见到你还挺开心的。',
  '这会儿状态看着不错，慢慢开始就行。',
  '你今天的气色挺好，随便练一点也算赚到。',
  '又见面了，今天也适合把身体活动开。',
  '看起来比前几天更有精神，继续保持。',
  '今天这股劲儿挺好，别浪费在发呆上。',
  '来了就很不错，剩下的我们慢慢来。',
  '你今天看起来挺在线，练一会儿会更舒服。',
];

const buildWelcomeGreetingVariants = (greeting: string, username?: string) => {
  const name = username?.trim() || '朋友';
  return [
    `${greeting}，${name}`,
    `${name}，今天也见面了`,
    `${greeting}，准备活动一下吗`,
  ];
};

/* ===== 天数计算 ===== */
const getNextEightAM = () => {
  const next = new Date();
  next.setHours(8, 0, 0, 0);
  if (next.getTime() <= Date.now()) {
    next.setDate(next.getDate() + 1);
  }
  return next;
};

/* ===== 主组件 ===== */
  const Welcome: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  const currentUser = initialState?.currentUser;
  const heroMessages = useMemo(
    () =>
      buildGreetingMessages(
        buildWelcomeGreetingVariants(getGreeting(), currentUser?.username),
        HOME_FRIENDLY_LINES,
      ),
    [currentUser?.username],
  );
  useEnsureCurrentUser();
  const { favoriteIds, videoUrl, setVideoUrl, handleToggleFavorite } = useExerciseInteractions();
  const [timerExercise, setTimerExercise] = useState<API.Exercise | null>(null);

  const [hoveredMuscle, setHoveredMuscle] = useState<Muscle | null>(null);
  const [selectedGroup, setSelectedGroup] = useState<string | null>(null);
  const [exercises, setExercises] = useState<API.Exercise[]>([]);
  const [exercisesLoading, setExercisesLoading] = useState(false);
  const [exVisibleCount, setExVisibleCount] = useState(8);
  const exLoadMoreRef = useRef<HTMLDivElement | null>(null);

  const [aiCard, setAiCard] = useState<API.SummaryNotificationCard | null>(null);
  const [aiExpanded, setAiExpanded] = useState(false);

  const syncSummaryCard = useCallback(async () => {
    try {
      const res: any = await getSummaryNotification({ skipErrorHandler: true });
      const cards: API.SummaryNotificationCard[] = res?.data?.cards || res?.cards || [];
      setAiCard(cards.find((card) => card.type === 'daily') || cards[0] || null);
    } catch {
      setAiCard(null);
    }
  }, []);

  /* ----- 数据加载 ----- */
  useEffect(() => {
    let timer = 0;

    const scheduleNextRefresh = () => {
      window.clearTimeout(timer);
      timer = window.setTimeout(async () => {
        await syncSummaryCard();
        scheduleNextRefresh();
      }, Math.max(1000, getNextEightAM().getTime() - Date.now()));
    };

    void syncSummaryCard();
    scheduleNextRefresh();

    const handleVisibilityChange = () => {
      if (!document.hidden) {
        void syncSummaryCard();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      window.clearTimeout(timer);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [syncSummaryCard]);

  /* ----- 模型交互 ----- */
  const loadExercises = useCallback(async (groupId: string) => {
    setSelectedGroup(groupId);
    setExVisibleCount(8);
    setExercisesLoading(true);
    try { setExercises(await getExercisesByGroup(groupId)); }
    catch { setExercises([]); }
    finally { setExercisesLoading(false); }
  }, []);

  const visibleExercises = useMemo(() => exercises.slice(0, exVisibleCount), [exercises, exVisibleCount]);
  const hasMoreExercises = exVisibleCount < exercises.length;

  useEffect(() => {
    const node = exLoadMoreRef.current;
    if (!node || !hasMoreExercises) return undefined;
    const observer = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting) setExVisibleCount(c => Math.min(c + 8, exercises.length)); },
      { rootMargin: '200px' },
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, [hasMoreExercises, exercises.length]);

  const handleClick = useCallback((stats: IMuscleStats) => {
    const groupId = muscleToGroupId[stats.muscle];
    if (groupId) loadExercises(groupId);
  }, [loadExercises]);

  const handleMouseMove = useCallback((e: React.MouseEvent<HTMLDivElement>, modelType: 'anterior' | 'posterior') => {
    const target = e.target as SVGPolygonElement;
    if (target.tagName === 'polygon') {
      const svg = target.closest('svg');
      if (svg) {
        const polygons = Array.from(svg.querySelectorAll('polygon')) as SVGPolygonElement[];
        const muscle = getMuscleFromPolygon(modelType, polygons.indexOf(target));
        if (muscle) { setHoveredMuscle(prev => prev === muscle ? prev : muscle); return; }
      }
    }
    setHoveredMuscle(null);
  }, []);

  const modelData = useMemo((): IExerciseData[] => {
    const result: IExerciseData[] = [];
    if (selectedGroup) {
      const muscles = getGroupMuscles(selectedGroup);
      if (muscles.length > 0) result.push({ name: 'Selected', muscles, frequency: 2 });
    }
    if (hoveredMuscle) {
      const hGroup = muscleToGroupId[hoveredMuscle];
      if (hGroup && hGroup !== selectedGroup) {
        const muscles = getGroupMuscles(hGroup);
        if (muscles.length > 0) result.push({ name: 'Hovered', muscles, frequency: 1 });
      }
    }
    return result;
  }, [hoveredMuscle, selectedGroup]);

  const hoveredGroup = hoveredMuscle ? muscleToGroupId[hoveredMuscle] : null;

  return (
    <div className="wc-page">
      <div className="wc-container">
        {/* ===== Hero ===== */}
        <TypewriterGreeting
          className="wc-hero"
          titleClassName="wc-hero-title"
          subClassName="wc-hero-sub"
          messages={heroMessages}
        />

        {/* ===== 人体模型面板 ===== */}
        <div className="wc-model-panel">
          <div className="wc-model-panel-header">
            <div className="wc-model-panel-title">
              <span className="wc-model-panel-dot" />
              肌肉导航
            </div>
            <div className={`wc-hover-hint${hoveredGroup ? ' is-visible' : ''}`}>
              悬停中：<strong>{hoveredGroup ? MUSCLE_GROUP_LABELS[hoveredGroup] : '胸部'}</strong>
            </div>
          </div>

          <div className="wc-model-grid">
            <div className="wc-model-col">
              <div className="wc-view-label wc-view-label--anterior">正面视图</div>
              <div className="wc-model-wrap" onMouseMove={e => handleMouseMove(e, 'anterior')} onMouseLeave={() => setHoveredMuscle(null)}>
                <Model data={modelData} onClick={handleClick} highlightedColors={HL_COLORS} style={{ width: '100%', height: 'auto', maxWidth: 240 }} />
              </div>
            </div>
            <div className="wc-model-col">
              <div className="wc-view-label wc-view-label--posterior">背面视图</div>
              <div className="wc-model-wrap" onMouseMove={e => handleMouseMove(e, 'posterior')} onMouseLeave={() => setHoveredMuscle(null)}>
                <Model type="posterior" data={modelData} onClick={handleClick} highlightedColors={HL_COLORS} style={{ width: '100%', height: 'auto', maxWidth: 240 }} />
              </div>
            </div>
          </div>

          {selectedGroup ? (
            <div className="wc-selected-badge">
              <span className="wc-selected-check">&#10003;</span>
              已选中：<strong>{MUSCLE_GROUP_LABELS[selectedGroup]}</strong>
            </div>
          ) : (
            <div className="wc-model-tip">
              <AimOutlined />
              <span>点击模型上任意高亮区域，查看该肌群的训练动作</span>
            </div>
          )}
        </div>

        {/* ===== 训练动作结果 ===== */}
        {selectedGroup && (
          <div className="wc-exercises-section">
            <div className="wc-exercises-header">
              <div className="wc-exercises-hero-icon"><FireOutlined /></div>
              <div>
                <span className="wc-exercises-title">{MUSCLE_GROUP_LABELS[selectedGroup]}训练动作</span>
                {!exercisesLoading && exercises.length > 0 && <span className="wc-exercises-count">共 {exercises.length} 个动作</span>}
              </div>
            </div>
            <Spin spinning={exercisesLoading}>
              {exercises.length > 0 ? (
                <>
                  <div className="wc-exercises-grid">
                    {visibleExercises.map((exercise, i) => (
                      <ExerciseCard key={exercise.id} exercise={exercise} index={i}
                        isFavorited={favoriteIds.includes(exercise.id)}
                        onToggleFavorite={handleToggleFavorite}
                        onVideoPlay={url => setVideoUrl(url)}
                        onStartTraining={setTimerExercise}
                      />
                    ))}
                  </div>
                  {hasMoreExercises && (
                    <div ref={exLoadMoreRef} style={{ height: 1 }} aria-hidden="true" />
                  )}
                </>
              ) : (
                <div className="wc-empty">
                  <div className="wc-empty-inner">
                    <div className="wc-empty-icon"><FireOutlined /></div>
                    <div className="wc-empty-title">暂无该肌群的动作数据</div>
                    <div className="wc-empty-desc">试试选择其他肌群</div>
                  </div>
                </div>
              )}
            </Spin>
          </div>
        )}
      </div>

      <Modal open={!!videoUrl} footer={null} onCancel={() => setVideoUrl(null)} width={800} destroyOnClose className="app-modal-video wc-video-modal"
        styles={{ body: { padding: 0, display: 'flex', justifyContent: 'center', background: '#000' } }}>
        {videoUrl && <video src={videoUrl} controls autoPlay style={{ width: '100%', maxHeight: '80vh' }} />}
      </Modal>
      <TrainingTimer open={!!timerExercise} exercise={timerExercise} onClose={() => setTimerExercise(null)} />
    </div>
  );
};

export default Welcome;
