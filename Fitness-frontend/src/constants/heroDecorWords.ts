export const HERO_DECOR_WORDS = [
  '自律',
  '热爱',
  '坚持',
  '突破',
  '进阶',
  '精进',
  '专注',
  '强韧',
  '蜕变',
  '冲刺',
  '狠练',
  '耐力',
  '爆发',
  '觉醒',
  '上强',
  '疯魔',
];

export const pickRandomHeroDecorWord = () =>
  HERO_DECOR_WORDS[Math.floor(Math.random() * HERO_DECOR_WORDS.length)] || HERO_DECOR_WORDS[0];
