import { describe, expect, it } from 'vitest';
import { formatDate, formatDateTime, formatRelative, initials, titleCase } from '../lib/format';

describe('format utilities', () => {
  it('formats dates with a medium style', () => {
    expect(formatDate('2025-01-15T10:30:00Z')).toMatch(/2025/);
  });

  it('returns an em dash for missing dates', () => {
    expect(formatDate(null)).toBe('—');
    expect(formatDate(undefined)).toBe('—');
    expect(formatDate('not-a-date')).toBe('—');
  });

  it('formats date times', () => {
    expect(formatDateTime('2025-01-15T10:30:00Z')).toMatch(/2025/);
  });

  it('returns a fallback for missing relative dates', () => {
    expect(formatRelative(null)).toBe('—');
  });

  it('produces initials', () => {
    expect(initials('Ada Lovelace')).toBe('AL');
    expect(initials('Grace')).toBe('G');
    expect(initials(null)).toBe('?');
    expect(initials('')).toBe('?');
  });

  it('title-cases snake case', () => {
    expect(titleCase('SCHEDULED')).toBe('Scheduled');
    expect(titleCase('no_show')).toBe('No Show');
    expect(titleCase(null)).toBe('');
  });
});
