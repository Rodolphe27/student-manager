import { describe, it, expect } from 'vitest';
import { getErrorMessage } from './errorMessage';

function errorWithMessage(message: unknown) {
  return { response: { data: { message } } };
}

describe('getErrorMessage', () => {
  it('returns the backend message when present and well-formed', () => {
    expect(getErrorMessage(errorWithMessage('Course code already exists: CS-101'), 'fallback'))
      .toBe('Course code already exists: CS-101');
  });

  it('falls back when the error has no response body', () => {
    expect(getErrorMessage(new Error('network error'), 'fallback')).toBe('fallback');
  });

  it('falls back when the message field is missing', () => {
    expect(getErrorMessage({ response: { data: {} } }, 'fallback')).toBe('fallback');
  });

  it('falls back when the message field is an empty string', () => {
    expect(getErrorMessage(errorWithMessage(''), 'fallback')).toBe('fallback');
  });

  it('falls back when the message looks stack-trace-shaped', () => {
    const stackTraceLike = 'java.lang.NullPointerException\n\tat com.student_manager.Foo.bar(Foo.java:42)';
    expect(getErrorMessage(errorWithMessage(stackTraceLike), 'fallback')).toBe('fallback');
  });

  it('falls back when the message mentions an Exception class name', () => {
    expect(getErrorMessage(errorWithMessage('DataIntegrityViolationException occurred'), 'fallback'))
      .toBe('fallback');
  });

  it('falls back when the message is implausibly long', () => {
    const tooLong = 'x'.repeat(301);
    expect(getErrorMessage(errorWithMessage(tooLong), 'fallback')).toBe('fallback');
  });

  it('accepts a message right at the length boundary', () => {
    const atLimit = 'x'.repeat(300);
    expect(getErrorMessage(errorWithMessage(atLimit), 'fallback')).toBe(atLimit);
  });
});
