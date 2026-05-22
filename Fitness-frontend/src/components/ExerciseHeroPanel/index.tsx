import React from 'react';
import './index.less';

type ExerciseHeroStat = {
  value: React.ReactNode;
  label: string;
};

type ExerciseHeroPanelProps = {
  icon: React.ReactNode;
  label: string;
  title: string;
  subtitle: string;
  stats: ExerciseHeroStat[];
  decorWord?: string;
  backgroundQuote?: string;
  className?: string;
};

const ExerciseHeroPanel: React.FC<ExerciseHeroPanelProps> = ({
  icon,
  label,
  title,
  subtitle,
  stats,
  decorWord,
  backgroundQuote,
  className,
}) => (
  <div className={`exercise-hero-panel${className ? ` ${className}` : ''}`}>
    {(decorWord || backgroundQuote) && (
      <div className="exercise-hero-poster" aria-hidden="true">
        {decorWord && <div className="exercise-hero-decor">{decorWord}</div>}
        {backgroundQuote && <div className="exercise-hero-quote">{backgroundQuote}</div>}
      </div>
    )}
    <div className="exercise-hero-copy">
      <div className="exercise-hero-label">
        <span className="exercise-hero-label-dot" />
        {label}
      </div>

      <div className="exercise-hero-head">
        <div className="exercise-hero-icon">{icon}</div>
        <h1 className="exercise-hero-title">{title}</h1>
      </div>

      <p className="exercise-hero-sub">{subtitle}</p>

      <div className="exercise-hero-stats">
        {stats.map((stat) => (
          <div className="exercise-hero-stat" key={stat.label}>
            <div className="exercise-hero-stat-val">{stat.value}</div>
            <div className="exercise-hero-stat-label">{stat.label}</div>
          </div>
        ))}
      </div>
    </div>
  </div>
);

export default ExerciseHeroPanel;
