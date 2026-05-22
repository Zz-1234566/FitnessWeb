import { CloseOutlined } from '@ant-design/icons';
import React, { useEffect } from 'react';
import './index.less';

type AppDesktopDialogProps = {
  open: boolean;
  title: React.ReactNode;
  subtitle?: React.ReactNode;
  onClose: () => void;
  children: React.ReactNode;
  footer?: React.ReactNode;
  className?: string;
  bodyClassName?: string;
};

const updateOverlayLock = (delta: number) => {
  if (typeof document === 'undefined') {
    return;
  }
  const root = document.documentElement;
  const body = document.body;
  const current = Number(body.dataset.appOverlayCount || '0');
  const next = Math.max(0, current + delta);
  body.dataset.appOverlayCount = String(next);
  root.dataset.appOverlayCount = String(next);
  if (next > 0) {
    root.classList.add('app-overlay-lock');
    body.classList.add('app-overlay-lock');
    return;
  }
  root.classList.remove('app-overlay-lock');
  body.classList.remove('app-overlay-lock');
};

const AppDesktopDialog: React.FC<AppDesktopDialogProps> = ({
  open,
  title,
  subtitle,
  onClose,
  children,
  footer,
  className,
  bodyClassName,
}) => {
  useEffect(() => {
    if (!open) {
      return undefined;
    }
    updateOverlayLock(1);
    return () => updateOverlayLock(-1);
  }, [open]);

  if (!open) {
    return null;
  }

  return (
    <div
      className="app-dialog-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget) {
          onClose();
        }
      }}
    >
      <div className={`app-dialog-panel ${className || ''}`} onClick={(e) => e.stopPropagation()}>
        <div className="app-dialog-head">
          <div className="app-dialog-heading">
            <div className="app-dialog-title">{title}</div>
            {subtitle ? <div className="app-dialog-subtitle">{subtitle}</div> : null}
          </div>
          <button type="button" className="app-dialog-close" onClick={onClose} aria-label="关闭">
            <CloseOutlined />
          </button>
        </div>
        <div className={`app-dialog-body ${bodyClassName || ''}`}>
          {children}
        </div>
        {footer ? (
          <div className="app-dialog-foot">
            {footer}
          </div>
        ) : null}
      </div>
    </div>
  );
};

export default AppDesktopDialog;
