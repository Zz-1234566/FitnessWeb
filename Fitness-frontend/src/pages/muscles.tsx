import { FireOutlined } from '@ant-design/icons';
import { Spin } from 'antd';
import { useModel } from '@umijs/max';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import BodyModel from 'react-body-highlighter';
import ExerciseCard from '@/components/ExerciseCard';
import { MUSCLE_GROUP_LABELS, type MuscleGroup } from '@/constants/exercise';
import { getExercisesByGroup } from '@/services/ant-design-pro/api';
import { useExerciseInteractions } from '@/hooks/useExerciseInteractions';
import TypewriterGreeting from '@/components/TypewriterGreeting';
import useEnsureCurrentUser from '@/hooks/useEnsureCurrentUser';
import TrainingTimer from '@/components/TrainingTimer';
import '../pages/welcome.less';

const MUSCLE_TO_GROUP: Record<string, MuscleGroup> = {
  chest: 'chest',
  'upper-back': 'back',
  'lower-back': 'back',
  trapezius: 'back',
  'front-deltoids': 'shoulders',
  'back-deltoids': 'shoulders',
  biceps: 'arms',
  triceps: 'arms',
  forearm: 'arms',
  quadriceps: 'legs',
  hamstring: 'legs',
  calves: 'legs',
  gluteal: 'legs',
  abs: 'core',
  obliques: 'core',
};

const getGroupMuscles = (groupId: string) =>
  Object.keys(MUSCLE_TO_GROUP).filter((m) => MUSCLE_TO_GROUP[m] === groupId);

const ANTERIOR_POLYGON_ORDER = [
  'chest', 'chest', 'obliques', 'obliques', 'abs', 'abs',
  'biceps', 'biceps', 'triceps', 'triceps', 'neck', 'neck',
  'front-deltoids', 'front-deltoids', 'head', 'adductor', 'adductor',
  'quadriceps', 'quadriceps', 'quadriceps', 'quadriceps', 'quadriceps', 'quadriceps',
  'knees', 'knees', 'calves', 'calves', 'calves', 'calves',
  'forearm', 'forearm', 'forearm', 'forearm',
];

const POSTERIOR_POLYGON_ORDER = [
  'head', 'trapezius', 'trapezius', 'back-deltoids', 'back-deltoids',
  'upper-back', 'upper-back', 'triceps', 'triceps', 'triceps', 'triceps',
  'lower-back', 'lower-back', 'forearm', 'forearm', 'forearm', 'forearm',
  'gluteal', 'gluteal', 'abductors', 'abductors',
  'hamstring', 'hamstring', 'hamstring', 'hamstring',
  'knees', 'knees', 'calves', 'calves', 'calves', 'calves',
  'left-soleus', 'right-soleus',
];

const getPolygonMuscle = (view: string, index: number) => {
  const order = view === 'anterior' ? ANTERIOR_POLYGON_ORDER : POSTERIOR_POLYGON_ORDER;
  return order[index] ?? null;
};

const HL_COLORS = ['rgba(255, 80, 80, 0.8)', 'rgba(255, 80, 80, 0.8)'];

const MUSCLE_SUB_LINES = [
  '点击人体图，找到你想练的部位。',
  '正面背面都能选，试试看。',
  '选中肌群后直接查看训练动作。',
  '今天想练哪里？从这里开始。',
];

const Muscles: React.FC = () => {
  useEnsureCurrentUser();
  const { initialState } = useModel('@@initialState');
  const currentUser = initialState?.currentUser;
  const { favoriteIds, videoUrl, setVideoUrl, handleToggleFavorite } = useExerciseInteractions();

  const [timerExercise, setTimerExercise] = useState<API.Exercise | null>(null);
  const [hoveredGroup, setHoveredGroup] = useState<MuscleGroup | null>(null);
  const [selectedGroup, setSelectedGroup] = useState<MuscleGroup | null>(null);
  const [exercises, setExercises] = useState<API.Exercise[]>([]);
  const [loading, setLoading] = useState(false);
  const [visibleCount, setVisibleCount] = useState(8);
  const loadMoreRef = useRef<HTMLDivElement | null>(null);

  const visibleExercises = useMemo(() => exercises.slice(0, visibleCount), [exercises, visibleCount]);
  const hasMore = visibleCount < exercises.length;

  const fetchExercises = useCallback(async (group: MuscleGroup) => {
    setSelectedGroup(group);
    setVisibleCount(8);
    setLoading(true);
    try {
      const list = await getExercisesByGroup(group);
      setExercises(list || []);
    } catch {
      setExercises([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const ref = loadMoreRef.current;
    if (!ref || !hasMore) return;
    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (entry.isIntersecting) setVisibleCount((c) => Math.min(c + 8, exercises.length));
      },
      { rootMargin: '200px' },
    );
    observer.observe(ref);
    return () => observer.disconnect();
  }, [hasMore, exercises.length]);

  const handleModelClick = useCallback(
    (data: { muscle?: string }) => {
      const group = data.muscle ? MUSCLE_TO_GROUP[data.muscle] : null;
      if (group) fetchExercises(group);
    },
    [fetchExercises],
  );

  const handlePolygonHover = useCallback((e: React.MouseEvent, view: string) => {
    const target = e.target;
    if (target.tagName !== 'polygon') {
      setHoveredGroup(null);
      return;
    }
    const svg = target.closest('svg');
    if (!svg) return;
    const polygons = Array.from(svg.querySelectorAll('polygon'));
    const index = polygons.indexOf(target as Element);
    const muscle = getPolygonMuscle(view, index);
    if (muscle) {
      const group = MUSCLE_TO_GROUP[muscle];
      if (group) { setHoveredGroup(group); return; }
    }
    setHoveredGroup(null);
  }, []);

  const highlightData = useMemo(() => {
    const data: { name: string; muscles: string[]; frequency: number }[] = [];
    if (selectedGroup) {
      const muscles = getGroupMuscles(selectedGroup);
      if (muscles.length > 0) data.push({ name: 'Selected', muscles, frequency: 2 });
    }
    if (hoveredGroup && hoveredGroup !== selectedGroup) {
      const muscles = getGroupMuscles(hoveredGroup);
      if (muscles.length > 0) data.push({ name: 'Hovered', muscles, frequency: 1 });
    }
    return data;
  }, [hoveredGroup, selectedGroup]);

  const hoveredLabel = hoveredGroup ? MUSCLE_GROUP_LABELS[hoveredGroup] : '胸部';
  const displayName = currentUser?.username?.trim() || '朋友';
  const heroMessages = useMemo(
    () => ({
      lines: [
        `你好，${displayName}`,
        `${displayName}，今天也见面了`,
        `你好，准备活动一下吗`,
      ],
      subs: MUSCLE_SUB_LINES,
    }),
    [displayName],
  );

  return (
    <div className="wc-page">
      <div className="wc-container">
        <TypewriterGreeting
          className="wc-hero"
          titleClassName="wc-hero-title"
          subClassName="wc-hero-sub"
          messages={heroMessages}
        />

        <div className="wc-model-panel">
          <div className="wc-model-panel-header">
            <div className="wc-model-panel-title">
              <span className="wc-model-panel-dot" />
              肌肉导航
            </div>
            <div className={`wc-hover-hint${hoveredGroup ? ' is-visible' : ''}`}>
              悬停中：<strong>{hoveredLabel}</strong>
            </div>
          </div>

          <div className="wc-model-grid">
            <div className="wc-model-col">
              <div className="wc-view-label wc-view-label--anterior">正面视图</div>
              <div
                className="wc-model-wrap"
                onMouseMove={(e) => handlePolygonHover(e, 'anterior')}
                onMouseLeave={() => setHoveredGroup(null)}
              >
                <BodyModel
                  data={highlightData}
                  onClick={handleModelClick}
                  highlightedColors={HL_COLORS}
                  style={{ width: '100%', height: 'auto' }}
                />
              </div>
            </div>
            <div className="wc-model-col">
              <div className="wc-view-label wc-view-label--posterior">背面视图</div>
              <div
                className="wc-model-wrap"
                onMouseMove={(e) => handlePolygonHover(e, 'posterior')}
                onMouseLeave={() => setHoveredGroup(null)}
              >
                <BodyModel
                  type="posterior"
                  data={highlightData}
                  onClick={handleModelClick}
                  highlightedColors={HL_COLORS}
                  style={{ width: '100%', height: 'auto' }}
                />
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
              <FireOutlined />
              <span>点击模型上任意高亮区域，查看该肌群的训练动作</span>
            </div>
          )}
        </div>

        {selectedGroup && (
          <div className="wc-exercises-section">
            <div className="wc-exercises-header">
              <div className="wc-exercises-hero-icon">
                <FireOutlined />
              </div>
              <div>
                <span className="wc-exercises-title">
                  {MUSCLE_GROUP_LABELS[selectedGroup]}训练动作
                </span>
                {!loading && exercises.length > 0 && (
                  <span className="wc-exercises-count">共 {exercises.length} 个动作</span>
                )}
              </div>
            </div>

            <Spin spinning={loading}>
              {exercises.length > 0 ? (
                <>
                  <div className="wc-exercises-grid">
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
                    {hasMore && <div ref={loadMoreRef} style={{ height: 1 }} aria-hidden="true" />}
                  </div>
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

      {videoUrl && (
        <div className="wc-video-overlay" onClick={() => setVideoUrl(null)}>
          <video
            src={videoUrl}
            controls
            autoPlay
            style={{ width: '100%', maxHeight: '80vh' }}
          />
        </div>
      )}

      <TrainingTimer open={!!timerExercise} exercise={timerExercise} onClose={() => setTimerExercise(null)} />
    </div>
  );
};

export default Muscles;
