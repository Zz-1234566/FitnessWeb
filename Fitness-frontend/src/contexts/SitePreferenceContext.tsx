import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';

export type SiteTheme = 'light' | 'dark';

type SitePreferenceValue = {
  theme: SiteTheme;
  setTheme: (theme: SiteTheme) => void;
  toggleTheme: () => void;
};

const THEME_KEY = 'site-theme';

const SitePreferenceContext = createContext<SitePreferenceValue | null>(null);

const getDefaultTheme = (): SiteTheme => {
  if (typeof window === 'undefined') return 'light';
  const stored = window.localStorage.getItem(THEME_KEY);
  if (stored === 'light' || stored === 'dark') return stored;
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
};

export const SitePreferenceProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [theme, setTheme] = useState<SiteTheme>(getDefaultTheme);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    window.localStorage.setItem(THEME_KEY, theme);
  }, [theme]);

  useEffect(() => {
    document.documentElement.lang = 'zh-CN';
  }, []);

  const value = useMemo<SitePreferenceValue>(
    () => ({
      theme,
      setTheme,
      toggleTheme: () => setTheme((prev) => (prev === 'light' ? 'dark' : 'light')),
    }),
    [theme],
  );

  return (
    <SitePreferenceContext.Provider value={value}>
      {children}
    </SitePreferenceContext.Provider>
  );
};

export const useSitePreferences = () => {
  const context = useContext(SitePreferenceContext);
  if (!context) {
    throw new Error('useSitePreferences must be used within SitePreferenceProvider');
  }
  return context;
};
