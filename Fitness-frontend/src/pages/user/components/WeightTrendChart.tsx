import React, { useMemo, useState } from 'react';
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';

interface TrendPoint {
  date: string;
  label: string;
  weight?: number | null;
}

interface WeightTrendChartProps {
  points: TrendPoint[];
  period?: string;
  loading?: boolean;
  drillYear?: number;
  drillMonth?: number;
  onDrillDown?: (year?: number, month?: number) => void;
}

/* ===== 周视图：面积图 ===== */
const WeekView: React.FC<{ points: TrendPoint[] }> = ({ points }) => {
  const chartData = useMemo(() =>
    points.map((p) => ({ date: p.label, weight: p.weight ?? null })),
    [points],
  );

  const hasAnyWeight = chartData.some((p) => p.weight != null);
  if (!hasAnyWeight || chartData.length === 0) {
    return <div className="weight-chart-empty">暂无体重数据</div>;
  }

  const vals = chartData.map((p) => p.weight).filter((w): w is number => w != null);
  const minW = Math.min(...vals);
  const maxW = Math.max(...vals);
  const pad = Math.max((maxW - minW) * 0.3, 1);
  const yDomain: [number, number] = [Math.floor(minW - pad), Math.ceil(maxW + pad)];

  return (
    <div className="weight-chart-container" style={{ width: '100%', height: '100%' }}>
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
          <defs>
            <linearGradient id="weightGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#1f6a5d" stopOpacity={0.2} />
              <stop offset="100%" stopColor="#1f6a5d" stopOpacity={0.02} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(22,74,65,0.08)" vertical={false} />
          <YAxis domain={yDomain} tickCount={4} tick={{ fontSize: 11, fill: '#999' }} axisLine={false} tickLine={false} width={36} />
          <XAxis dataKey="date" tick={{ fontSize: 11, fill: '#999' }} axisLine={false} tickLine={false} />
          <Tooltip
            content={({ active, payload }) => {
              if (!active || !payload?.length) return null;
              const d = payload[0].payload;
              if (d.weight == null) return null;
              return (
                <div className="weight-chart-tooltip">
                  <div className="weight-chart-tooltip-date">{d.date}</div>
                  <div className="weight-chart-tooltip-value">{d.weight.toFixed(2)} kg</div>
                </div>
              );
            }}
            isAnimationActive={false}
          />
          <Area type="monotone" dataKey="weight" stroke="#1f6a5d" strokeWidth={2} fill="url(#weightGradient)"
            dot={{ r: 3, fill: '#1f6a5d', strokeWidth: 0 }}
            activeDot={{ r: 5, fill: '#164a41', stroke: '#fff', strokeWidth: 2 }}
            connectNulls={false} isAnimationActive={false} />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
};

/* ===== 月视图：日历小圆 ===== */
const MonthView: React.FC<{ points: TrendPoint[]; initialYear?: number; initialMonth?: number }> = ({ points, initialYear, initialMonth }) => {
  const now = new Date();
  const [viewYear, setViewYear] = useState(initialYear ?? now.getFullYear());
  const [viewMonth, setViewMonth] = useState(initialMonth ?? (now.getMonth() + 1));

  const weightMap = useMemo(() => {
    const m = new Map<number, number>();
    points.forEach((p) => {
      if (p.weight != null) {
        const parts = p.date.split('-');
        const y = parseInt(parts[0], 10);
        const mo = parseInt(parts[1], 10);
        const d = parseInt(parts[2], 10);
        if (y === viewYear && mo === viewMonth) {
          m.set(d, p.weight);
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
    cells.push(<div key={`e${i}`} className="wc-day-slot" />);
  }
  for (let d = 1; d <= totalDays; d++) {
    const isToday = d === now.getDate() && viewMonth === now.getMonth() + 1 && viewYear === now.getFullYear();
    const w = weightMap.get(d);
    cells.push(
      <div key={d} className={`wc-day-cell${isToday ? ' wc-today' : ''}`}>
        <div className="wc-circle">{w != null ? w.toFixed(2) : <span className="wc-no-data">--</span>}</div>
        <span className="wc-day-label">{d}日</span>
      </div>,
    );
  }

  return (
    <div className="wc-month-view">
      <div className="wc-month-header">
        <button className="wc-month-nav" onClick={() => changeMonth(-1)}>&lt;</button>
        <span className="wc-month-title">{viewYear}年{monthNames[viewMonth - 1]}</span>
        <button className="wc-month-nav" onClick={() => changeMonth(1)}>&gt;</button>
      </div>
      <div className="wc-weekdays">
        {weekLabels.map((w) => <div key={w} className="wc-weekday">{w}</div>)}
      </div>
      <div className="wc-days">{cells}</div>
    </div>
  );
};

/* ===== 年视图：月份卡片 ===== */
const YearView: React.FC<{ points: TrendPoint[]; onDrillDown?: (year?: number, month?: number) => void }> = ({ points, onDrillDown }) => {
  const now = new Date();
  const [viewYear, setViewYear] = useState(now.getFullYear());
  const [goMonth, setGoMonth] = useState<number | null>(null);

  const monthData = useMemo(() => {
    const m = new Map<number, number[]>();
    points.forEach((p) => {
      if (p.weight != null && p.label) {
        const moStr = p.label.replace('月', '');
        const mo = parseInt(moStr, 10);
        if (mo >= 1 && mo <= 12) {
          if (!m.has(mo)) m.set(mo, []);
          m.get(mo)!.push(p.weight);
        }
      }
    });
    return m;
  }, [points]);

  const changeYear = (dir: number) => setViewYear(viewYear + dir);
  const monthNames = ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月'];
  const isCurrentMonth = (m: number) => m === now.getMonth() + 1 && viewYear === now.getFullYear();

  if (goMonth != null) {
    onDrillDown?.(viewYear, goMonth);
    return null;
  }

  return (
    <div className="wc-year-view">
      <div className="wc-month-header">
        <button className="wc-month-nav" onClick={() => changeYear(-1)}>&lt;</button>
        <span className="wc-month-title">{viewYear}年</span>
        <button className="wc-month-nav" onClick={() => changeYear(1)}>&gt;</button>
      </div>
      <div className="wc-year-grid">
        {monthNames.map((name, i) => {
          const m = i + 1;
          const vals = monthData.get(m);
          const isCurrent = isCurrentMonth(m);
          let badge: React.ReactNode;
          if (!vals || vals.length === 0) {
            badge = <span className="wc-card-badge wc-card-badge--empty">暂无</span>;
          } else {
            const minW = Math.min(...vals);
            const change = vals.length >= 2 ? vals[vals.length - 1] - vals[0] : 0;
            const isDown = change < 0;
            const isUp = change > 0;
            const sign = isUp ? '+' : '-';
            badge = (
              <div className="wc-card-detail">
                <span className="wc-card-badge wc-card-badge--value">{minW.toFixed(2)}kg</span>
                <span className={`wc-card-badge${isUp ? ' wc-card-badge--up' : isDown ? ' wc-card-badge--down' : ''}`}>
                  {change !== 0 ? `${sign}${Math.abs(change).toFixed(2)}kg` : '0'}
                </span>
              </div>
            );
          }
          return (
            <div key={m} className={`wc-month-card${isCurrent ? ' wc-month-card--current' : ''}`} onClick={() => setGoMonth(m)}>
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
const WeightTrendChart: React.FC<WeightTrendChartProps> = ({ points, period, loading, drillYear, drillMonth, onDrillDown }) => {
  if (loading) {
    return <div className="weight-chart-empty" style={{ height: 200 }}>加载中...</div>;
  }

  if (period === 'year') {
    return <YearView points={points} onDrillDown={onDrillDown} />;
  }

  if (period === 'month') {
    return <MonthView points={points} initialYear={drillYear} initialMonth={drillMonth} />;
  }

  return <WeekView points={points} />;
};

export default WeightTrendChart;
