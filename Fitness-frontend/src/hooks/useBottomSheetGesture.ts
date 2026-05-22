import { useCallback, useEffect, useRef, useState } from 'react';

const OPEN_OFFSET = 56;
const CLOSE_THRESHOLD = 56;
const MAX_DRAG_OFFSET = 420;
const SHEET_TRANSITION = 'transform 240ms cubic-bezier(0.22, 1, 0.36, 1)';

type DragState = {
  startY: number;
  startOffset: number;
  dragging: boolean;
};

export default function useBottomSheetGesture(open: boolean, onClosed: () => void) {
  const [mounted, setMounted] = useState(open);
  const [offsetY, setOffsetY] = useState(0);
  const [dragging, setDragging] = useState(false);
  const [closing, setClosing] = useState(false);
  const closeTimerRef = useRef<number | null>(null);
  const offsetRef = useRef(0);
  const dragStateRef = useRef<DragState>({ startY: 0, startOffset: 0, dragging: false });

  const clearCloseTimer = useCallback(() => {
    if (closeTimerRef.current !== null) {
      window.clearTimeout(closeTimerRef.current);
      closeTimerRef.current = null;
    }
  }, []);

  useEffect(() => {
    if (!open) {
      if (!closing) {
        setMounted(false);
        setOffsetY(0);
        offsetRef.current = 0;
        setDragging(false);
      }
      return undefined;
    }

    clearCloseTimer();
    setMounted(true);
    setClosing(false);
    setDragging(false);
    setOffsetY(OPEN_OFFSET);
    offsetRef.current = OPEN_OFFSET;
    const raf = window.requestAnimationFrame(() => {
      setOffsetY(0);
      offsetRef.current = 0;
    });
    return () => window.cancelAnimationFrame(raf);
  }, [clearCloseTimer, open]);

  useEffect(() => () => clearCloseTimer(), [clearCloseTimer]);

  const requestClose = useCallback(() => {
    if (!mounted || closing) return;
    clearCloseTimer();
    setClosing(true);
    setDragging(false);
    const closingOffset = window.innerHeight * 0.72;
    setOffsetY(closingOffset);
    offsetRef.current = closingOffset;
    closeTimerRef.current = window.setTimeout(() => {
      setMounted(false);
      setClosing(false);
      setOffsetY(0);
      offsetRef.current = 0;
      onClosed();
    }, 240);
  }, [clearCloseTimer, closing, mounted, onClosed]);

  const handleTouchStart = useCallback((event: React.TouchEvent<HTMLElement>) => {
    if (!mounted || closing || event.touches.length === 0) return;
    const touch = event.touches[0];
    dragStateRef.current = {
      startY: touch.clientY,
      startOffset: offsetY,
      dragging: true,
    };
    setDragging(true);
  }, [closing, mounted, offsetY]);

  const handleTouchMove = useCallback((event: React.TouchEvent<HTMLElement>) => {
    if (!dragStateRef.current.dragging || event.touches.length === 0) return;
    const touch = event.touches[0];
    const delta = touch.clientY - dragStateRef.current.startY;
    const nextOffset = Math.max(0, Math.min(MAX_DRAG_OFFSET, dragStateRef.current.startOffset + delta));
    setOffsetY(nextOffset);
    offsetRef.current = nextOffset;
  }, []);

  const handleTouchEnd = useCallback(() => {
    if (!dragStateRef.current.dragging) return;
    dragStateRef.current.dragging = false;
    setDragging(false);
    if (offsetRef.current > CLOSE_THRESHOLD) {
      requestClose();
      return;
    }
    setOffsetY(0);
    offsetRef.current = 0;
  }, [requestClose]);

  return {
    mounted,
    requestClose,
    dragHandleProps: {
      onTouchStart: handleTouchStart,
      onTouchMove: handleTouchMove,
      onTouchEnd: handleTouchEnd,
      onTouchCancel: handleTouchEnd,
    },
    sheetStyle: {
      transform: `translateY(${offsetY}px)`,
      transition: dragging ? 'none' : SHEET_TRANSITION,
    } as React.CSSProperties,
  };
}
