import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { getEnabledDictItems, type DictItemVO } from '@/service/api/rtd/system-management/dict-item';

/** 进程内缓存：同一 dictType 只拉取一次原始 VO */
const cache = new Map<string, DictItemVO[]>();

export interface DictOption {
  label: string;
  value: string;
  cssClass: string;
  sortOrder: number;
}

const pickLabel = (item: DictItemVO, locale: string) => {
  const labels = item.labels;
  const lang = locale.startsWith('en') ? 'en' : 'zh';
  return labels?.[lang] || labels?.zh || item.itemLabel || item.itemCode || '';
};

const toOption = (item: DictItemVO, locale: string): DictOption => ({
  label: pickLabel(item, locale),
  value: item.itemCode,
  cssClass: item.cssClass || '',
  sortOrder: item.sortOrder ?? 0
});

/**
 * 拉取并缓存指定数据字典类型的启用选项。
 * 返回 options(响应式) 与 load()；load 幂等，命中缓存直接复用。
 * 切换语言时按 locale 从 labels 重算 label，无需重新请求。
 */
export function useDictOptions(dictType: string) {
  const items = ref<DictItemVO[]>([]);
  const loading = ref(false);
  const { locale } = useI18n();

  const options = computed(() => items.value.map(item => toOption(item, locale.value)));

  const load = async () => {
    if (cache.has(dictType)) {
      items.value = cache.get(dictType)!;
      return;
    }
    loading.value = true;
    try {
      const res = await getEnabledDictItems(dictType);
      const sorted = [...(res.data ?? [])].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
      cache.set(dictType, sorted);
      items.value = sorted;
    } finally {
      loading.value = false;
    }
  };

  /** 根据 item_code 找显示文案，用于表格列回显 */
  const labelOf = (code?: string | null) => options.value.find(o => o.value === code)?.label ?? code ?? '';

  /** 根据 item_code 找 css_class，用于 Tag 着色 */
  const tagTypeOf = (code?: string | null) => options.value.find(o => o.value === code)?.cssClass ?? '';

  return { options, loading, load, labelOf, tagTypeOf };
}
