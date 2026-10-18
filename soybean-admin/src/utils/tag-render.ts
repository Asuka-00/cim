import { h } from 'vue';
import { NTag, type TagProps } from 'naive-ui';
import { CommonState } from '@/service/api/common';
import { $t } from '@/locales';

type TagType = NonNullable<TagProps['type']>;

export function renderTag(label: unknown, type: TagType = 'default') {
  if (label === null || label === undefined || label === '') return '—';
  return h(NTag, { size: 'small', type, bordered: false }, { default: () => String(label) });
}

export function renderEnabledTag(enabled?: string | null) {
  const on = enabled === CommonState.ENABLED;
  return renderTag(on ? $t('common.enabled') : $t('common.disabled'), on ? 'success' : 'default');
}

export function renderBoolTag(
  value: boolean | null | undefined,
  trueType: TagType = 'success',
  falseType: TagType = 'default'
) {
  return renderTag(value ? $t('common.yesOrNo.yes') : $t('common.yesOrNo.no'), value ? trueType : falseType);
}

export function renderResultTag(status?: string | null) {
  const key = (status || '').toUpperCase();
  const type: TagType = /SUCCESS|OK|PASS|DONE/.test(key)
    ? 'success'
    : /FAIL|ERROR|TIMEOUT|REJECT/.test(key)
      ? 'error'
      : /RUN|PEND|WAIT|PROCESS/.test(key)
        ? 'info'
        : 'default';
  return renderTag(status, type);
}

export function renderHttpStatusTag(status?: number | null) {
  if (status == null) return '—';
  const type: TagType = status < 300 ? 'success' : status < 400 ? 'info' : status < 500 ? 'warning' : 'error';
  return renderTag(status, type);
}
