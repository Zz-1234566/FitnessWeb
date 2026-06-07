import { Card, Button } from 'antd';
import { PlayCircleOutlined, HeartOutlined, HeartFilled, ToolOutlined } from '@ant-design/icons';
import React, { useEffect, useMemo, useRef, useState } from 'react';
import type { IExerciseData, Muscle } from 'react-body-highlighter';
import { MUSCLE_GROUP_LABELS, parseEquipment } from '@/constants/exercise';
import './ExerciseCard.less';

export interface ExerciseCardProps {
  exercise: API.Exercise;
  index?: number;
  isFavorited?: boolean;
  onToggleFavorite?: (id: number) => void;
  onVideoPlay?: (url: string) => void;
  onStartTraining?: (exercise: API.Exercise) => void;
}

/* ===== 肌群 → 模型肌肉映射 ===== */
const GROUP_MUSCLES: Record<string, Muscle[]> = {
  chest: ['chest'],
  back: ['upper-back', 'lower-back', 'trapezius'],
  shoulders: ['front-deltoids', 'back-deltoids'],
  arms: ['biceps', 'triceps', 'forearm'],
  legs: ['quadriceps', 'hamstring', 'calves', 'gluteal'],
  core: ['abs', 'obliques'],
};

const DIFF_PCT: Record<string, number> = {
  easy: 30,
  medium: 65,
  hard: 100,
  初级: 30,
  中级: 65,
  高级: 100,
};

const DIFF_META: Record<string, { className: string; color: string; label: string }> = {
  easy: { className: 'excard-badge-easy', color: '#52c41a', label: '初级' },
  medium: { className: 'excard-badge-medium', color: '#faad14', label: '中级' },
  hard: { className: 'excard-badge-hard', color: '#ff4d4f', label: '高级' },
  初级: { className: 'excard-badge-easy', color: '#52c41a', label: '初级' },
  中级: { className: 'excard-badge-medium', color: '#faad14', label: '中级' },
  高级: { className: 'excard-badge-hard', color: '#ff4d4f', label: '高级' },
};

const MOBILE_CARD_QUERY = '(max-width: 768px)';
const BodyModel = React.lazy(() => import('react-body-highlighter'));

const isMobileCardViewport = () =>
  typeof window !== 'undefined' && window.matchMedia(MOBILE_CARD_QUERY).matches;

const ExerciseCard: React.FC<ExerciseCardProps> = ({
  exercise,
  index = 0,
  isFavorited,
  onToggleFavorite,
  onVideoPlay,
  onStartTraining,
}) => {
  const diffKey = exercise.difficulty || '初级';
  const diffMeta = DIFF_META[diffKey] || DIFF_META.easy;
  const diffPct = DIFF_PCT[diffKey] ?? 25;
  const [favBounce, setFavBounce] = useState(false);
  const [inView, setInView] = useState(false);
  const [isMobileCard, setIsMobileCard] = useState(isMobileCardViewport);
  const cardRef = useRef<HTMLDivElement>(null);

  const modelData = useMemo((): IExerciseData[] => {
    if (isMobileCard) return [];
    const muscles = exercise.muscleGroup ? GROUP_MUSCLES[exercise.muscleGroup] : undefined;
    return muscles ? [{ name: 'hl', muscles, frequency: 2 }] : [];
  }, [exercise.muscleGroup, isMobileCard]);
  const isPosterior = exercise.muscleGroup === 'back';
  const hlColors = ['rgba(255, 80, 80, 0.8)', 'rgba(255, 80, 80, 0.8)'];

  useEffect(() => {
    if (typeof window === 'undefined') return undefined;

    const media = window.matchMedia(MOBILE_CARD_QUERY);
    const handleChange = () => setIsMobileCard(media.matches);
    handleChange();

    if (media.addEventListener) {
      media.addEventListener('change', handleChange);
      return () => media.removeEventListener('change', handleChange);
    }

    media.addListener(handleChange);
    return () => media.removeListener(handleChange);
  }, []);

  // 桌面端进入视口才挂载人体模型；移动端只显示轻量播放按钮。
  useEffect(() => {
    if (isMobileCard) {
      setInView(false);
      return undefined;
    }

    const node = cardRef.current;
    if (!node) return;
    const observer = new IntersectionObserver(
      ([entry]) => setInView(entry.isIntersecting),
      { rootMargin: '200px' },
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, [isMobileCard]);

  const handleFavClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    onToggleFavorite?.(exercise.id);
    setFavBounce(true);
    setTimeout(() => setFavBounce(false), 500);
  };

  return (
    <div
      ref={cardRef}
      className="excard-enter"
      style={{
        animationDelay: `${Math.min(index * 0.06, 0.5)}s`,
        '--excard-diff-color': diffMeta.color,
      } as React.CSSProperties}
    >
      <Card
        hoverable
        className={`excard${favBounce ? ' excard-fav-flash' : ''}`}
        styles={{ body: { padding: 0 } }}
      >
        <div className="excard-cover">
          {exercise.videoUrl ? (
            <>
              {isMobileCard ? (
                <button
                  className="excard-mobile-play"
                  type="button"
                  onClick={() => onVideoPlay?.(exercise.videoUrl!)}
                  aria-label={`播放${exercise.name}视频`}
                >
                  <span className="excard-play-icon">
                    <PlayCircleOutlined />
                  </span>
                </button>
              ) : (
                <>
                  <div className="excard-cover-model">
                    {inView ? (
                      <React.Suspense
                        fallback={
                          <div
                            className="excard-cover-model-fallback"
                            style={{ background: 'rgba(255, 80, 80, 0.3)' }}
                          />
                        }
                      >
                        <BodyModel
                          type={isPosterior ? 'posterior' : 'anterior'}
                          data={modelData}
                          highlightedColors={hlColors}
                          style={{ width: '100%', height: '100%' }}
                        />
                      </React.Suspense>
                    ) : (
                      <div
                        className="excard-cover-model-fallback"
                        style={{ background: 'rgba(255, 80, 80, 0.3)' }}
                      />
                    )}
                  </div>
                  {exercise.muscleGroup && (
                    <span className="excard-cover-group-label">
                      {MUSCLE_GROUP_LABELS[exercise.muscleGroup] || exercise.muscleGroup}
                    </span>
                  )}
                </>
              )}
              {!isMobileCard && (
                <div className="excard-play-overlay" onClick={() => onVideoPlay?.(exercise.videoUrl!)}>
                  <div className="excard-play-icon">
                    <PlayCircleOutlined />
                  </div>
                </div>
              )}
            </>
          ) : (
            <span className="excard-placeholder">
              <HeartOutlined style={{ fontSize: 32, opacity: 0.25 }} />
              <span className="excard-placeholder-text">暂无视频</span>
            </span>
          )}

          <span className={`excard-badge ${diffMeta.className}`}>{diffMeta.label}</span>

          {onToggleFavorite && (
            <Button
              type="text"
              icon={
                isFavorited ? (
                  <HeartFilled style={{ color: '#e8654a' }} />
                ) : (
                  <HeartOutlined style={{ color: '#bbb' }} />
                )
              }
              onClick={handleFavClick}
              className={`excard-fav-btn${favBounce ? ' excard-fav-bounce' : ''}`}
            />
          )}
        </div>

        <div className="excard-body">
          <div className="excard-headline">
            <div className="excard-name">{exercise.name}</div>
            {onStartTraining && (
              <button
                type="button"
                className="excard-inline-train-btn"
                onClick={() => onStartTraining(exercise)}
              >
                开始训练
              </button>
            )}
          </div>

          <div className="excard-meta">
            {exercise.muscleGroup && (
              <span className="excard-muscle-tag">
                {MUSCLE_GROUP_LABELS[exercise.muscleGroup] || exercise.muscleGroup}
              </span>
            )}

            <span className="excard-meta-item">
              <ToolOutlined style={{ fontSize: 12 }} />
              {parseEquipment(exercise.equipment || '').join(' · ') || exercise.equipment}
            </span>
          </div>

          {exercise.recommendedSets && exercise.recommendedReps && (
            <div className="excard-params">
              <div className="excard-param-item">
                <span className="excard-param-val">{exercise.recommendedSets}</span>
                <span className="excard-param-label">组数</span>
              </div>

              <div className="excard-param-sep" />

              <div className="excard-param-item">
                <span className="excard-param-val">{exercise.recommendedReps}</span>
                <span className="excard-param-label">次数</span>
              </div>

              {exercise.restSeconds && (
                <>
                  <div className="excard-param-sep" />
                  <div className="excard-param-item">
                    <span className="excard-param-val">{exercise.restSeconds}s</span>
                    <span className="excard-param-label">组间休息</span>
                  </div>
                </>
              )}
            </div>
          )}

          <div className="excard-diff-bar">
            <div className="excard-diff-label">
              <span>难度</span>
              <span>{diffMeta.label}</span>
            </div>
            <div className="excard-diff-track">
              <div
                className="excard-diff-fill"
                style={{
                  width: `${diffPct}%`,
                  background: `linear-gradient(90deg, ${diffMeta.color}, ${diffMeta.color}88)`,
                }}
              />
            </div>
          </div>

          {(exercise.steps || exercise.tips) && (
            <div className="excard-details">
              {exercise.steps && (
                <div className="excard-detail-section">
                  <div className="excard-detail-label excard-detail-steps">训练步骤</div>
                  <div className="excard-detail-text">{exercise.steps}</div>
                </div>
              )}

              {exercise.tips && (
                <div className="excard-detail-section">
                  <div className="excard-detail-label excard-detail-tips">注意事项</div>
                  <div className="excard-detail-text">{exercise.tips}</div>
                </div>
              )}
            </div>
          )}
        </div>
      </Card>
    </div>
  );
};

export default React.memo(ExerciseCard);
