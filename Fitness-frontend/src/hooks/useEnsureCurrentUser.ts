import { useModel } from '@umijs/max';
import { useEffect, useRef } from 'react';

export const useEnsureCurrentUser = () => {
  const { initialState, setInitialState } = useModel('@@initialState');
  const triedRef = useRef(false);

  useEffect(() => {
    if (initialState?.currentUser || triedRef.current) {
      return;
    }

    triedRef.current = true;
    void initialState?.fetchUserInfo?.().then((userInfo) => {
      if (!userInfo) return;
      setInitialState((state) => ({
        ...state,
        currentUser: userInfo,
      }));
    });
  }, [initialState, setInitialState]);
};

export default useEnsureCurrentUser;
