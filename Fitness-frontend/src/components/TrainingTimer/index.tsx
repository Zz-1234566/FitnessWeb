import {
  CheckCircleFilled,
  CloseOutlined,
  PauseOutlined,
  PlayCircleOutlined,
  RightOutlined,
} from '@ant-design/icons';
import { Button, message } from 'antd';
import React, { useEffect, useMemo, useState } from 'react';
import { MUSCLE_GROUP_LABELS } from '@/constants/exercise';
import useBottomSheetGesture from '@/hooks/useBottomSheetGesture';
import { addExerciseRecord, getTodayExerciseRecords } from '@/services/ant-design-pro/api';
import './index.less';

type TrainingTimerPhase = 'idle' | 'working' | 'paused' | 'done';

type TrainingDraft = {
  exerciseId: number;
  exerciseName: string;
  muscleGroup?: string;
  totalSets: number;
  recommendedReps?: string;
  status: TrainingTimerPhase;
  currentSet: number;
  completedSets: number;
  currentSetElapsed: number;
  totalElapsed: number;
  setTimes: number[];
  startedAt?: string;
  updatedAt: string;
};

type TrainingTimerProps = {
  open: boolean;
  exercise?: API.Exercise | null;
  onClose: () => void;
};

const DRAFT_STORAGE_KEY = 'fitness_training_timer_draft_v1';

const formatSeconds = (seconds: number) => {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
};

const getNowTime = () =>
  new Date().toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });

const getDefaultDraft = (exercise: API.Exercise): TrainingDraft => ({
  exerciseId: exercise.id,
  exerciseName: exercise.name,
  muscleGroup: exercise.muscleGroup,
  totalSets: exercise.recommendedSets || 1,
  recommendedReps: exercise.recommendedReps,
  status: 'idle',
  currentSet: 0,
  completedSets: 0,
  currentSetElapsed: 0,
  totalElapsed: 0,
  setTimes: [],
  updatedAt: new Date().toISOString(),
});

const readDraft = (): TrainingDraft | null => {
  if (typeof window === 'undefined') return null;
  try {
    const raw = window.localStorage.getItem(DRAFT_STORAGE_KEY);
    return raw ? (JSON.parse(raw) as TrainingDraft) : null;
  } catch {
    return null;
  }
};

const writeDraft = (draft: TrainingDraft | null) => {
  if (typeof window === 'undefined') return;
  if (!draft) {
    window.localStorage.removeItem(DRAFT_STORAGE_KEY);
    return;
  }
  window.localStorage.setItem(
    DRAFT_STORAGE_KEY,
    JSON.stringify({
      ...draft,
      updatedAt: new Date().toISOString(),
    }),
  );
};

const TrainingTimer: React.FC<TrainingTimerProps> = ({ open, exercise, onClose }) => {
  const [draft, setDraft] = useState<TrainingDraft | null>(null);
  const [records, setRecords] = useState<API.StructuredExerciseSession[]>([]);
  const [saving, setSaving] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [recordsOpen, setRecordsOpen] = useState(false);
  const [isMobileSheet, setIsMobileSheet] = useState(
    () => typeof window !== 'undefined' && window.innerWidth <= 768,
  );

  useEffect(() => {
    if (typeof window === 'undefined') return undefined;
    const media = window.matchMedia('(max-width: 768px)');
    const sync = () => setIsMobileSheet(media.matches);
    sync();
    if (media.addEventListener) {
      media.addEventListener('change', sync);
      return () => media.removeEventListener('change', sync);
    }
    media.addListener(sync);
    return () => media.removeListener(sync);
  }, []);

  const mobileSheet = useBottomSheetGesture(open && isMobileSheet, onClose);

  const progressSetNumber = draft
    ? draft.status === 'done'
      ? draft.totalSets
      : draft.completedSets + (draft.status === 'working' ? 1 : 0)
    : 0;

  useEffect(() => {
    if (!open || !exercise) return;

    const storageDraft = readDraft();
    if (storageDraft && storageDraft.exerciseId === exercise.id) {
      setDraft(storageDraft);
    } else {
      setDraft(getDefaultDraft(exercise));
    }

    let mounted = true;
    getTodayExerciseRecords({ skipErrorHandler: true })
      .then((res) => {
        if (!mounted) return;
        const parsed = Array.isArray(res) ? res : [];
        setRecords(parsed.slice().reverse());
        setRecordsOpen(parsed.length > 0);
      })
      .catch(() => {
        if (!mounted) return;
        setRecords([]);
      });

    return () => {
      mounted = false;
    };
  }, [exercise, open]);

  useEffect(() => {
    if (!open || !draft) return undefined;
    if (draft.status !== 'working') {
      writeDraft(draft);
      return undefined;
    }

    const timer = window.setInterval(() => {
      setDraft((prev) => {
        if (!prev || prev.status !== 'working') return prev;
        const next = {
          ...prev,
          currentSetElapsed: prev.currentSetElapsed + 1,
          totalElapsed: prev.totalElapsed + 1,
          updatedAt: new Date().toISOString(),
        };
        writeDraft(next);
        return next;
      });
    }, 1000);

    return () => window.clearInterval(timer);
  }, [draft, open]);

  useEffect(() => {
    if (!open) return undefined;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [open]);

  const progressSegments = useMemo(() => {
    if (!draft) return [];
    return Array.from({ length: draft.totalSets }, (_, index) => {
      const done = index < draft.completedSets;
      const current = draft.status !== 'idle' && draft.status !== 'done' && index === draft.currentSet;
      return {
        key: index,
        done,
        current,
        time: done ? formatSeconds(draft.setTimes[index] || 0) : null,
      };
    });
  }, [draft]);

  if ((!open && !mobileSheet.mounted) || !exercise || !draft) {
    return null;
  }

  const muscleLabel = draft.muscleGroup
    ? (exercise.muscleGroup === draft.muscleGroup ? draft.muscleGroup : exercise.muscleGroup)
    : exercise.muscleGroup;
  const displayMuscleLabel = muscleLabel ? (MUSCLE_GROUP_LABELS[muscleLabel] || muscleLabel) : '';

  const finishTraining = async () => {
    setSaving(true);
    const finishedAt = getNowTime();
    try {
      await addExerciseRecord(
        {
          exerciseId: exercise.id,
          time: finishedAt,
          exerciseName: exercise.name,
          muscleGroup: exercise.muscleGroup,
          completedSets: draft.completedSets,
          totalSets: draft.totalSets,
          durationSeconds: draft.totalElapsed,
          source: 'manual',
        },
        { skipErrorHandler: true },
      );

      const newRecord: API.StructuredExerciseSession = {
        time: finishedAt,
        name: exercise.name,
        durationSeconds: draft.totalElapsed,
        source: 'manual',
        items: [{
          exerciseId: exercise.id,
          name: exercise.name,
          muscleGroup: exercise.muscleGroup,
          completedSets: draft.completedSets,
          totalSets: draft.totalSets,
          durationSeconds: draft.totalElapsed,
          source: 'manual',
        }],
      };
      setRecords((prev) => [newRecord, ...prev]);
      setRecordsOpen(true);
      writeDraft(null);
      setDraft(getDefaultDraft(exercise));
      setConfirmOpen(false);
      message.success('训练记录已保存');
      onClose();
    } catch {
      message.error('记录保存失败，请先登录或稍后重试');
    } finally {
      setSaving(false);
    }
  };

  const handleStart = () => {
    const next: TrainingDraft = {
      ...draft,
      status: 'working',
      startedAt: draft.startedAt || new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    setDraft(next);
    writeDraft(next);
  };

  const handlePause = () => {
    const next = { ...draft, status: 'paused' as TrainingTimerPhase, updatedAt: new Date().toISOString() };
    setDraft(next);
    writeDraft(next);
  };

  const handleResume = () => {
    const next = { ...draft, status: 'working' as TrainingTimerPhase, updatedAt: new Date().toISOString() };
    setDraft(next);
    writeDraft(next);
  };

  const handleCompleteSet = () => {
    const nextSetTimes = [...draft.setTimes, draft.currentSetElapsed];
    const nextCompletedSets = draft.completedSets + 1;
    const nextIsDone = nextCompletedSets >= draft.totalSets;
    const next: TrainingDraft = {
      ...draft,
      status: nextIsDone ? 'done' : 'paused',
      completedSets: nextCompletedSets,
      currentSet: Math.min(draft.currentSet + 1, draft.totalSets - 1),
      currentSetElapsed: 0,
      setTimes: nextSetTimes,
      updatedAt: new Date().toISOString(),
    };
    setDraft(next);
    writeDraft(next);
  };

  const handleReset = () => {
    writeDraft(null);
    setDraft(getDefaultDraft(exercise));
    setConfirmOpen(false);
  };

  const handleSheetClose = () => {
    if (isMobileSheet) {
      mobileSheet.requestClose();
      return;
    }
    onClose();
  };

  const averageSetTime = draft.setTimes.length
    ? Math.round(draft.setTimes.reduce((sum, item) => sum + item, 0) / draft.setTimes.length)
    : 0;

  return (
    <>
      <div className="training-timer-overlay" onClick={handleSheetClose} />
      <div
        className="training-timer-sheet"
        role="dialog"
        aria-modal="true"
        style={isMobileSheet ? mobileSheet.sheetStyle : undefined}
      >
        <div className="training-timer-handle" {...(isMobileSheet ? mobileSheet.dragHandleProps : {})} />
        <div className="training-timer-top">
          <div className="training-timer-head">
            <strong>{exercise.name}</strong>
            <span>
              {[displayMuscleLabel, exercise.equipment, draft.totalSets && draft.recommendedReps ? `推荐 ${draft.totalSets}×${draft.recommendedReps}` : null]
                .filter(Boolean)
                .join(' · ')}
            </span>
          </div>
          <Button
            type="text"
            shape="circle"
            icon={<CloseOutlined />}
            className="training-timer-close"
            onClick={handleSheetClose}
          />
        </div>

        <div className="training-timer-setbar">
          <div
            className="training-timer-setbar-grid"
            style={{ gridTemplateColumns: `repeat(${Math.max(draft.totalSets, 1)}, minmax(0, 1fr))` }}
          >
          {progressSegments.map((segment) => (
            <div
              key={segment.key}
              className={`training-timer-setbar-segment${segment.done ? ' is-done' : ''}${segment.current ? ' is-current' : ''}`}
            >
              {segment.time && <span>{segment.time}</span>}
            </div>
          ))}
          </div>
        </div>

        <div className={`training-timer-setlabel${draft.status === 'done' ? ' is-done' : ''}`}>
            {draft.status === 'idle' && `共 ${draft.totalSets} 组`}
            {draft.status !== 'idle' && draft.status !== 'done' && (
              <>
                <span className="training-timer-setnum">{progressSetNumber}</span> / {draft.totalSets} 组
              </>
            )}
          {draft.status === 'done' && `已完成 ${draft.totalSets} 组`}
        </div>

          <div className={`training-timer-ring is-${draft.status}`}>
          <div className="training-timer-digits">{formatSeconds(draft.currentSetElapsed)}</div>
          <div className="training-timer-status">
            {draft.status === 'idle' && '准备开始训练'}
            {draft.status === 'working' && `第 ${draft.currentSet + 1} 组进行中`}
            {draft.status === 'paused' && '已暂停，可继续训练'}
            {draft.status === 'done' && '本次训练已完成'}
          </div>
        </div>

        <div className="training-timer-stats">
          <div className="training-timer-stat">
            <span>{draft.completedSets}</span>
            <small>已完成组数</small>
          </div>
          <div className="training-timer-stat">
            <span>{formatSeconds(draft.totalElapsed)}</span>
            <small>累计训练时长</small>
          </div>
        </div>

        <div className="training-timer-actions">
          {draft.status === 'idle' && (
            <button type="button" className="training-timer-btn is-primary" onClick={handleStart}>
              <PlayCircleOutlined />
              开始训练
            </button>
          )}
          {draft.status === 'working' && (
            <>
              <button type="button" className="training-timer-btn is-ghost" onClick={handlePause}>
                <PauseOutlined />
                暂停
              </button>
              <button type="button" className="training-timer-btn is-primary" onClick={handleCompleteSet}>
                <CheckCircleFilled />
                完成本组
              </button>
            </>
          )}
          {draft.status === 'paused' && (
            <>
              <button type="button" className="training-timer-btn is-danger" onClick={() => setConfirmOpen(true)}>
                结束训练
              </button>
              <button type="button" className="training-timer-btn is-primary" onClick={handleResume}>
                继续训练
              </button>
            </>
          )}
          {draft.status === 'done' && (
            <button type="button" className="training-timer-btn is-primary" onClick={() => setConfirmOpen(true)}>
              结束并记录
            </button>
          )}
        </div>

        <div className="training-timer-records">
          <button
            type="button"
            className={`training-timer-records-toggle${recordsOpen ? ' is-open' : ''}`}
            onClick={() => setRecordsOpen((prev) => !prev)}
          >
            <span>
              今日记录
              <em>{records.length} 次</em>
            </span>
            <RightOutlined />
          </button>
          {recordsOpen && (
            <div className="training-timer-records-body">
              {records.length > 0 ? (
                records.map((record, index) => (
                  <div className="training-timer-record-item" key={`${record.time || index}-${record.name || index}`}>
                    <div>
                      <strong>{record.name}</strong>
                      <span>
                        {[
                          Array.isArray(record.items) ? `${record.items.length} 个动作` : '',
                          record.time,
                        ].filter(Boolean).join(' · ')}
                      </span>
                      {Array.isArray(record.items) && record.items.length > 0 && (
                        <span>
                          {record.items.map((item) => [
                            item.name,
                            item.completedSets != null && item.totalSets != null ? `${item.completedSets}/${item.totalSets}组` : '',
                          ].filter(Boolean).join(' · ')).join(' ｜ ')}
                        </span>
                      )}
                    </div>
                    <b>{formatSeconds(record.durationSeconds || 0)}</b>
                  </div>
                ))
              ) : (
                <div className="training-timer-record-empty">今天还没有训练记录</div>
              )}
            </div>
          )}
        </div>
      </div>

      {confirmOpen && (
        <div className="training-timer-confirm" onClick={() => setConfirmOpen(false)}>
          <div className="training-timer-confirm-card" onClick={(event) => event.stopPropagation()}>
            <div className="training-timer-confirm-icon">
              <CheckCircleFilled />
            </div>
            <h3>{exercise.name} 训练完成</h3>
          <p>{[displayMuscleLabel, exercise.equipment, exercise.difficulty].filter(Boolean).join(' · ')}</p>

            <div className="training-timer-confirm-stats">
              <div>
                <strong>{Math.max(1, Math.round(draft.totalElapsed / 60))}</strong>
                <span>总时长(分钟)</span>
              </div>
              <div>
                <strong>
                  {draft.completedSets} / {draft.totalSets}
                </strong>
                <span>完成组数</span>
              </div>
              <div>
                <strong>{averageSetTime ? formatSeconds(averageSetTime) : '--:--'}</strong>
                <span>平均每组</span>
              </div>
            </div>

            <div className="training-timer-confirm-actions">
              <button type="button" className="training-timer-btn is-ghost" onClick={() => setConfirmOpen(false)}>
                取消
              </button>
              <button type="button" className="training-timer-btn is-primary" onClick={finishTraining} disabled={saving}>
                {saving ? '保存中...' : '确认记录'}
              </button>
            </div>

            {draft.status !== 'done' && (
              <button type="button" className="training-timer-reset" onClick={handleReset}>
                放弃当前训练草稿
              </button>
            )}
          </div>
        </div>
      )}
    </>
  );
};

export default TrainingTimer;
