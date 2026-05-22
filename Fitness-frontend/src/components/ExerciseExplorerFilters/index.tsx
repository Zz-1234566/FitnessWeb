import { SearchOutlined } from '@ant-design/icons';
import { Input } from 'antd';
import React from 'react';
import './index.less';

export type ExerciseExplorerFilterOption = {
  key: string;
  label: string;
  color?: string;
  count?: number;
  showDot?: boolean;
};

export type ExerciseExplorerFilterRow = {
  label: string;
  options: ExerciseExplorerFilterOption[];
  activeKey: string;
  onChange: (key: string) => void;
};

type ExerciseExplorerFiltersProps = {
  searchValue: string;
  searchPlaceholder: string;
  onSearchChange: (value: string) => void;
  rows: ExerciseExplorerFilterRow[];
};

const ExerciseExplorerFilters: React.FC<ExerciseExplorerFiltersProps> = ({
  searchValue,
  searchPlaceholder,
  onSearchChange,
  rows,
}) => (
  <>
    <div className="exercise-search-bar">
      <Input
        prefix={<SearchOutlined style={{ color: 'var(--text-3)' }} />}
        placeholder={searchPlaceholder}
        allowClear
        value={searchValue}
        onChange={(e) => onSearchChange(e.target.value)}
      />
    </div>

    <div className="exercise-filter-panel">
      <div className="exercise-filter-groups">
        {rows.map((row) => (
          <div className="exercise-filter-row" key={row.label}>
            <div className="exercise-filter-label">{row.label}</div>
            <div className="exercise-filters">
              {row.options.map((option) => {
                const isActive = row.activeKey === option.key;
                const color = option.color || '#164A41';
                return (
                  <button
                    key={option.key}
                    className={`exercise-filter-btn ${isActive ? 'exercise-filter-btn-active' : ''}`}
                    style={isActive ? ({ '--exercise-active-color': color } as React.CSSProperties) : undefined}
                    onClick={() => row.onChange(option.key)}
                  >
                    {option.showDot && <span className="exercise-filter-dot" style={{ background: color }} />}
                    <span>{option.label}</span>
                    {typeof option.count === 'number' && option.count > 0 && (
                      <span className="exercise-filter-count">{option.count}</span>
                    )}
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </div>
  </>
);

export default ExerciseExplorerFilters;
