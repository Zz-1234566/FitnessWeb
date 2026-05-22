export const BMI_GAUGE_R = 62;
export const BMI_CIRCUMFERENCE = 2 * Math.PI * BMI_GAUGE_R;

export const BMI_SCALE_ITEMS = [
  { key: 'thin', label: '偏瘦' },
  { key: 'normal', label: '正常' },
  { key: 'overweight', label: '偏胖' },
  { key: 'obese', label: '肥胖' },
];

export function bmiStatus(v: number) {
  if (v < 18.5) return { text: '偏瘦', color: '#faad14', bg: 'rgba(250,173,20,0.08)', range: 'thin' };
  if (v < 24) return { text: '正常', color: '#52c41a', bg: 'rgba(82,196,26,0.08)', range: 'normal' };
  if (v < 28) return { text: '偏胖', color: '#faad14', bg: 'rgba(250,173,20,0.08)', range: 'overweight' };
  return { text: '肥胖', color: '#ff4d4f', bg: 'rgba(255,77,79,0.08)', range: 'obese' };
}
