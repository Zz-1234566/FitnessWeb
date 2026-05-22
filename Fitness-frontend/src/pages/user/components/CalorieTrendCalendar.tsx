import React, { useMemo, useState } from 'react';

interface TrendPoint {
  date: string;
  label: string;
  intakeCalories?: number | null;
  targetCalories?: number | null;
  calorieBalance?: number | null;
}

interface CalorieTrendCalendarProps {
  points: TrendPoint[];
  period?: string;
  loading?: boolean;
  drillYear?: number;
  drillMonth?: number;
  onDrillDown?: (year?: number, month?: number) => void;
}

function fmtNum(n: number): string {
  return Math.abs(Math.round(n)).toString();
}

/* ===== 月视图：日历盈余 ===== */
const CalorieMonthView: React.FC<{ points: TrendPoint[]; initialYear?: number; initialMonth?: number }> = ({ points, initialYear, initialMonth }) => {
  const now = new Date();
  const [viewYear, setViewYear] = useState(initialYear ?? now.getFullYear());
  const [viewMonth, setViewMonth] = useState(initialMonth ?? (now.getMonth() + 1));

  const balanceMap = useMemo(() => {
    const m = new Map<number, number | null>();
    points.forEach((p) => {
      const parts = p.date.split('-');
      const y = parseInt(parts[0], 10);
      const mo = parseInt(parts[1], 10);
      const d = parseInt(parts[2], 10);
      if (y === viewYear && mo === viewMonth) {
        const target = p.targetCalories;
        const intake = p.intakeCalories;
        if (target != null && intake != null) {
          m.set(d, target - intake);
        } else if (p.calorieBalance != null) {
          m.set(d, p.calorieBalance);
        } else {
          m.set(d, null);
        }
      }
    });
    return m;
  }, [points, viewYear, viewMonth]);

  const totalDays = new Date(viewYear, viewMonth, 0).getDate();
  const firstDayWeek = new Date(viewYear, viewMonth - 1, 1).getDay();
  const firstDayMon = firstDayWeek === 0 ? 7 : firstDayWeek;

  const changeMonth = (dir: number) => {
    let m = viewMonth + dir;
    let y = viewYear;
    if (m < 1) { m = 12; y--; }
    if (m > 12) { m = 1; y++; }
    setViewMonth(m);
    setViewYear(y);
  };

  const monthNames = ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月'];
  const weekLabels = ['一','二','三','四','五','六','日'];

  let cells: React.ReactNode[] = [];
  for (let i = 0; i < firstDayMon - 1; i++) {
    cells.push(<div key={`e${i}`} className="cc-day-slot" />);
  }
  for (let d = 1; d <= totalDays; d++) {
    const isToday = d === now.getDate() && viewMonth === now.getMonth() + 1 && viewYear === now.getFullYear();
    const balance = balanceMap.get(d);
    const hasData = balance != null;
    const isOver = hasData && balance! < 0;
    const cls = `cc-day-cell${isToday ? ' cc-today' : ''}${isOver ? ' cc-over' : ''}${hasData && !isOver ? ' cc-surplus' : ''}`;
    cells.push(
      <div key={d} className={cls}>
        <div className="cc-circle">
          {hasData ? fmtNum(balance!) : <span className="cc-no-data">--</span>}
        </div>
        <span className="cc-day-label">{d}日</span>
      </div>,
    );
  }

  return (
    <div className="cc-month-view">
      <div className="wc-month-header">
        <button className="wc-month-nav" onClick={() => changeMonth(-1)}>&lt;</button>
        <span className="wc-month-title">{viewYear}年{monthNames[viewMonth - 1]}</span>
        <button className="wc-month-nav" onClick={() => changeMonth(1)}>&gt;</button>
      </div>
      <div className="wc-weekdays">
        {weekLabels.map((w) => <div key={w} className="wc-weekday">{w}</div>)}
      </div>
      <div className="cc-days">{cells}</div>
    </div>
  );
};

/* ===== 年视图：月份卡片 ===== */
const CalorieYearView: React.FC<{ points: TrendPoint[]; onDrillDown?: (year?: number, month?: number) => void }> = ({ points, onDrillDown }) => {
  const now = new Date();
  const [viewYear, setViewYear] = useState(now.getFullYear());
  const [goMonth, setGoMonth] = useState<number | null>(null);

  const monthData = useMemo(() => {
    const m = new Map<number, { surplus: number; over: number; count: number }>();
    points.forEach((p) => {
      if (p.label) {
        const moStr = p.label.replace('月', '');
        const mo = parseInt(moStr, 10);
        if (mo >= 1 && mo <= 12) {
          if (!m.has(mo)) m.set(mo, { surplus: 0, over: 0, count: 0 });
          const entry = m.get(mo)!;
          const target = p.targetCalories;
          const intake = p.intakeCalories;
          let balance: number | null = null;
          if (target != null && intake != null) balance = target - intake;
          else if (p.calorieBalance != null) balance = p.calorieBalance;
          if (balance != null) {
            entry.count++;
            if (balance >= 0) entry.surplus += balance;
            else entry.over += Math.abs(balance);
          }
        }
      }
    });
    return m;
  }, [points]);

  const changeYear = (dir: number) => setViewYear(viewYear + dir);
  const monthNames = ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月'];
  const isCurrentMonth = (mo: number) => mo === now.getMonth() + 1 && viewYear === now.getFullYear();

  if (goMonth != null) {
    onDrillDown?.(viewYear, goMonth);
    return null;
  }

  return (
    <div className="cc-year-view">
      <div className="wc-month-header">
        <button className="wc-month-nav" onClick={() => changeYear(-1)}>&lt;</button>
        <span className="wc-month-title">{viewYear}年</span>
        <button className="wc-month-nav" onClick={() => changeYear(1)}>&gt;</button>
      </div>
      <div className="wc-year-grid">
        {monthNames.map((name, i) => {
          const mo = i + 1;
          const data = monthData.get(mo);
          const isCurrent = isCurrentMonth(mo);
          let badge: React.ReactNode;
          if (!data || data.count === 0) {
            badge = <span className="wc-card-badge wc-card-badge--empty">暂无</span>;
          } else {
            const net = data.surplus - data.over;
            const isOver = net < 0;
            const sign = net < 0 ? '+' : net > 0 ? '-' : '';
            badge = <span className={`wc-card-badge${isOver ? ' wc-card-badge--up' : net > 0 ? ' wc-card-badge--down' : ''}`}>
              {net !== 0 ? `${sign}${Math.abs(net)}` : '0'} kcal
            </span>;
          }
          return (
            <div key={mo} className={`wc-month-card${isCurrent ? ' wc-month-card--current' : ''}`} onClick={() => setGoMonth(mo)}>
              <span className="wc-card-label">{name}</span>
              {badge}
            </div>
          );
        })}
      </div>
    </div>
  );
};

/* ===== 主组件 ===== */
const CalorieTrendCalendar: React.FC<CalorieTrendCalendarProps> = ({ points, period, loading, drillYear, drillMonth, onDrillDown }) => {
  if (loading) {
    return <div className="weight-chart-empty" style={{ height: 280 }}>加载中...</div>;
  }

  if (period === 'year') {
    return <CalorieYearView points={points} onDrillDown={onDrillDown} />;
  }

  if (period === 'month') {
    return <CalorieMonthView points={points} initialYear={drillYear} initialMonth={drillMonth} />;
  }

  return null;
};

export default CalorieTrendCalendar;
