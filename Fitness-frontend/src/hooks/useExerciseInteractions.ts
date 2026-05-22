import { toggleFavorite } from '@/services/ant-design-pro/api';
import { useModel } from '@umijs/max';
import { useCallback, useMemo, useState } from 'react';

const parseFavoriteIds = (value?: string): number[] => {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

export const useExerciseInteractions = () => {
  const { initialState, setInitialState } = useModel('@@initialState');
  const [videoUrl, setVideoUrl] = useState<string | null>(null);

  const favoriteIds = useMemo(
    () => parseFavoriteIds(initialState?.currentUser?.favoritesExercises),
    [initialState?.currentUser?.favoritesExercises],
  );

  const handleToggleFavorite = useCallback(
    async (id: number) => {
      const ids = await toggleFavorite(id);
      setInitialState((state) => {
        if (!state?.currentUser) return state;
        return {
          ...state,
          currentUser: {
            ...state.currentUser,
            favoritesExercises: JSON.stringify(ids),
          },
        };
      });
    },
    [setInitialState],
  );

  return {
    favoriteIds,
    videoUrl,
    setVideoUrl,
    handleToggleFavorite,
  };
};
