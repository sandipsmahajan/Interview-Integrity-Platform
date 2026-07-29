export type RiskLevel = 'low' | 'medium' | 'high';

export function riskLevel(score: number): RiskLevel {
  if (score >= 70) {
    return 'high';
  }
  if (score >= 35) {
    return 'medium';
  }
  return 'low';
}

export function formatPercent(value: number): string {
  return `${Math.round(value)}%`;
}
