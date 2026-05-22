import { Spin, Modal } from 'antd';
import { SearchOutlined, StarFilled, StarOutlined } from '@ant-design/icons';
import { useModel } from '@umijs/max';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getFavoriteList } from '@/services/ant-design-pro/api';
import ExerciseCard from '@/components/ExerciseCard';
import ExerciseExplorerFilters, { type ExerciseExplorerFilterRow } from '@/components/ExerciseExplorerFilters';
import TrainingTimer from '@/components/TrainingTimer';
import {
  DIFFICULTY_OPTIONS,
  getEquipmentOptions,
  MUSCLE_GROUP_COLORS,
  MUSCLE_GROUP_FILTERS,
  MuscleGroup,
} from '@/constants/exercise';
import { FAVORITES_EMPTY_COPY, FAVORITES_SEARCH_COPY } from '@/constants/favorites';
import { useExerciseInteractions } from '@/hooks/useExerciseInteractions';
import { getGreeting } from '@/utils/greeting';
import TypewriterGreeting, { buildGreetingMessages } from '@/components/TypewriterGreeting';
import './favorites.less';

const FAVORITES_GREET_LINES = [
  '常练的动作先留在这里，想开始的时候会更快。',
  '收藏不是囤着看，挑一个最顺手的先练起来。',
  '把常用动作放在眼前，会更容易把训练接回正轨。',
  '今天想偷懒也没关系，先点开一个收藏动作看看。',
  '这些都是你留下来的偏好，拿来直接开练最合适。',
  '先从熟悉的动作开始，身体更容易进入状态。',
];

const buildFavoritesGreetingVariants = (greeting: string, username?: string) => {
  const name = username?.trim() || '朋友';
  return [
    `${greeting}，${name}`,
    `${name}，今天从收藏里挑一个开始`,
    `${greeting}，这些动作都在等你继续`,
  ];
};

const Favorites: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  const currentUser = initialState?.currentUser;
  const heroMessages = useMemo(
    () =>
      buildGreetingMessages(
        buildFavoritesGreetingVariants(getGreeting(), currentUser?.username),
        FAVORITES_GREET_LINES,
      ),
    [currentUser?.username],
  );
  const [selectedGroup, setSelectedGroup] = useState<MuscleGroup>('all');
  const [selectedDifficulty, setSelectedDifficulty] = useState('all');
  const [selectedEquipment, setSelectedEquipment] = useState('all');
  const [searchText, setSearchText] = useState('');
  const [exercises, setExercises] = useState<API.Exercise[]>([]);
  const [loading, setLoading] = useState(false);
  const [visibleCount, setVisibleCount] = useState(8);
  const [timerExercise, setTimerExercise] = useState<API.Exercise | null>(null);
  const loadMoreRef = useRef<HTMLDivElement | null>(null);
  const { videoUrl, setVideoUrl, handleToggleFavorite } = useExerciseInteractions();

  const fetchFavorites = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getFavoriteList();
      setExercises(res || []);
    } catch {
      setExercises([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchFavorites();
  }, [fetchFavorites]);

  const filteredExercises = useMemo(() => {
    let result = exercises;
    if (selectedGroup !== 'all') {
      result = result.filter((e) => e.muscleGroup === selectedGroup);
    }
    if (selectedDifficulty !== 'all') {
      result = result.filter((e) => e.difficulty === selectedDifficulty);
    }
    if (selectedEquipment !== 'all') {
      result = result.filter((e) => e.equipment === selectedEquipment);
    }
    if (searchText.trim()) {
      const kw = searchText.trim().toLowerCase();
      result = result.filter((e) =>
        e.name?.toLowerCase().includes(kw) ||
        e.equipment?.toLowerCase().includes(kw) ||
        e.difficulty?.toLowerCase().includes(kw),
      );
    }
    return result;
  }, [exercises, searchText, selectedDifficulty, selectedEquipment, selectedGroup]);

  const groupCounts = useMemo(() => {
    const counts: Record<string, number> = { all: exercises.length };
    exercises.forEach((e) => {
      if (e.muscleGroup) counts[e.muscleGroup] = (counts[e.muscleGroup] || 0) + 1;
    });
    return counts;
  }, [exercises]);

  const isEmptyResult = filteredExercises.length === 0 && !loading;

  const visibleExercises = useMemo(() => filteredExercises.slice(0, visibleCount), [filteredExercises, visibleCount]);
  const hasMore = visibleCount < filteredExercises.length;
  const equipmentOptions = useMemo(() => getEquipmentOptions(exercises), [exercises]);

  const favoriteFilterRows = useMemo<ExerciseExplorerFilterRow[]>(
    () => [
      {
        label: '肌群',
        activeKey: selectedGroup,
        onChange: (key) => setSelectedGroup(key as MuscleGroup),
        options: MUSCLE_GROUP_FILTERS.map((g) => ({
          key: g.key,
          label: g.label,
          color: g.key !== 'all' ? MUSCLE_GROUP_COLORS[g.key] : '#164A41',
          count: groupCounts[g.key] || 0,
          showDot: g.key !== 'all',
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
    [equipmentOptions, groupCounts, selectedDifficulty, selectedEquipment, selectedGroup],
  );

  useEffect(() => { setVisibleCount(8); }, [selectedGroup, selectedDifficulty, selectedEquipment, searchText]);

  useEffect(() => {
    const node = loadMoreRef.current;
    if (!node || !hasMore) return undefined;
    const observer = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting) setVisibleCount(c => Math.min(c + 8, filteredExercises.length)); },
      { rootMargin: '200px' },
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, [hasMore, filteredExercises.length]);

  const handleRemoveFavorite = async (id: number) => {
    await handleToggleFavorite(id);
    setExercises(prev => prev.filter(ex => ex.id !== id));
  };

  const collectedGroupCount = useMemo(
    () => MUSCLE_GROUP_FILTERS.filter((g) => g.key !== 'all' && (groupCounts[g.key] || 0) > 0).length,
    [groupCounts],
  );
  const hasActiveFilter =
    selectedGroup !== 'all' ||
    selectedDifficulty !== 'all' ||
    selectedEquipment !== 'all' ||
    searchText.trim().length > 0;
  const emptyCopy = isEmptyResult && exercises.length > 0 && hasActiveFilter
    ? FAVORITES_EMPTY_COPY.noMatches
    : FAVORITES_EMPTY_COPY.noFavorites;
  return (
    <div className="fav-page">
      <TypewriterGreeting
        className="fav-hero-lite"
        titleClassName="fav-hero-lite-title"
        subClassName="fav-hero-lite-sub"
        messages={heroMessages}
      />

      <ExerciseExplorerFilters
        searchValue={searchText}
        searchPlaceholder={FAVORITES_SEARCH_COPY.placeholder}
        onSearchChange={setSearchText}
        rows={favoriteFilterRows}
      />

      {!loading && exercises.length > 0 && (
        <div className="fav-result-bar">
          <div className="fav-result-count">
            共 <strong>{filteredExercises.length}</strong> 个收藏动作
          </div>
          <div className="fav-result-progress">
            已覆盖 <strong>{collectedGroupCount}</strong> 个肌群
          </div>
        </div>
      )}

      <div className="fav-content-shell">
      <Spin spinning={loading}>
        {filteredExercises.length > 0 && (
          <>
            <div className="fav-grid">
              {visibleExercises.map((ex, i) => (
                <ExerciseCard
                  key={ex.id}
                  exercise={ex}
                  index={i}
                  isFavorited={true}
                  onToggleFavorite={handleRemoveFavorite}
                  onVideoPlay={(url) => setVideoUrl(url)}
                  onStartTraining={setTimerExercise}
                />
              ))}
            </div>
            {hasMore && (
              <div className="fav-load-more" ref={loadMoreRef} aria-hidden="true">
                <div className="fav-load-more-line" />
                <span className="fav-load-more-text">继续下滑加载更多收藏动作</span>
              </div>
            )}
          </>
        )}
        {isEmptyResult && (
          <div className="fav-empty">
            <div className="fav-empty-inner">
              <div className="fav-empty-illust">
                {exercises.length > 0 ? <SearchOutlined /> : <StarOutlined />}
              </div>
              <div className="fav-empty-title">{emptyCopy.title}</div>
              <div className="fav-empty-desc">{emptyCopy.desc}</div>
            </div>
          </div>
        )}
      </Spin>
      </div>

      <Modal
        open={!!videoUrl}
        footer={null}
        onCancel={() => setVideoUrl(null)}
        width={800}
        destroyOnClose
        className="app-modal-video fav-video-modal"
        styles={{ body: { padding: 0, display: 'flex', justifyContent: 'center', background: '#000' } }}
      >
        {videoUrl && <video src={videoUrl} controls autoPlay style={{ width: '100%', maxHeight: '80vh' }} />}
      </Modal>

      <TrainingTimer open={!!timerExercise} exercise={timerExercise} onClose={() => setTimerExercise(null)} />
    </div>
  );
};

export default Favorites;
