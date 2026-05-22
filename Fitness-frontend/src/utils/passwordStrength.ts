export const PASSWORD_STRENGTH_LEVELS = [1, 2, 3];

export const getPasswordStrength = (pwd: string) => {
  if (!pwd) return { level: 0, label: '', color: '' };
  let score = 0;
  if (pwd.length >= 8) score++;
  if (pwd.length >= 12) score++;
  if (/[A-Z]/.test(pwd)) score++;
  if (/[0-9]/.test(pwd)) score++;
  if (/[^A-Za-z0-9]/.test(pwd)) score++;
  if (score <= 2) return { level: 1, label: '弱', color: '#ff4d4f' };
  if (score <= 3) return { level: 2, label: '中', color: '#faad14' };
  return { level: 3, label: '强', color: '#52c41a' };
};
