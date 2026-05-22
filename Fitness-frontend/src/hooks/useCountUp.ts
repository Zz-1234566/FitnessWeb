import { useEffect, useRef, useState } from 'react';

export function useCountUp(target: number, duration = 700, delay = 0, decimal = false) {
  const [value, setValue] = useState(0);
  const rafRef = useRef<number>(0);

  useEffect(() => {
    if (!target) return;
    const timer = setTimeout(() => {
      const start = performance.now();
      const step = (now: number) => {
        const p = Math.min((now - start) / duration, 1);
        const ease = 1 - Math.pow(1 - p, 3);
        const val = ease * target;
        setValue(decimal ? +val.toFixed(1) : Math.round(val));
        if (p < 1) rafRef.current = requestAnimationFrame(step);
      };
      rafRef.current = requestAnimationFrame(step);
    }, delay);
    return () => {
      clearTimeout(timer);
      cancelAnimationFrame(rafRef.current);
    };
  }, [target, duration, delay, decimal]);

  return value;
}
