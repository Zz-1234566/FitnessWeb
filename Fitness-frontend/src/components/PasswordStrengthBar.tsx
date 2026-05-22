import React, { useMemo } from 'react';
import { getPasswordStrength, PASSWORD_STRENGTH_LEVELS } from '@/utils/passwordStrength';

type PasswordStrengthBarProps = {
  password: string;
};

const PasswordStrengthBar: React.FC<PasswordStrengthBarProps> = ({ password }) => {
  const strength = useMemo(() => getPasswordStrength(password), [password]);

  if (!password) return null;

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
      <div style={{ display: 'flex', gap: 4, flex: 1 }}>
        {PASSWORD_STRENGTH_LEVELS.map(i => (
          <div key={i} style={{
            height: 3,
            flex: 1,
            borderRadius: 2,
            background: i <= strength.level ? strength.color : '#e8e8e8',
            transition: 'background 0.3s ease',
          }} />
        ))}
      </div>
      <span style={{ fontSize: 11, fontWeight: 600, minWidth: 16, textAlign: 'right', color: strength.color, transition: 'color 0.3s ease' }}>
        {strength.label}
      </span>
    </div>
  );
};

export default PasswordStrengthBar;
