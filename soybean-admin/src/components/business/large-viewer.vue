<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';
import { useVirtualizer } from '@tanstack/vue-virtual';
import ConvertAnsi from 'ansi-to-html';
import { $t } from '@/locales';

defineOptions({ name: 'LargeViewer' });

interface Props {
  lines: string[];
  loading?: boolean;
  height?: number;
  rowHeight?: number;
  emptyText?: string;
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  height: 520,
  rowHeight: 24,
  emptyText: ''
});

type LogLine = {
  lineNumber: number;
  raw: string;
  plain: string;
};

type MatchItem = {
  lineNumber: number;
  start: number;
  end: number;
};

const keyword = ref('');
const filterEnabled = ref(false);
const caseSensitive = ref(false);
const activeMatchIndex = ref(-1);
const jumpLineNumber = ref<number | null>(null);
const parentRef = ref<HTMLElement | null>(null);

const ansiConverter = new ConvertAnsi({
  escapeXML: true,
  fg: '#d4d4d4',
  bg: '#111827'
});

const ansiPattern = new RegExp(
  `[${String.fromCharCode(0x1b, 0x9b)}][[\\]()#;?]*(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9A-ORZcf-nqry=><]`,
  'g'
);

const stripAnsi = (value: string) => value.replace(ansiPattern, '');

const lineItems = computed<LogLine[]>(() =>
  props.lines.map((line, index) => ({
    lineNumber: index + 1,
    raw: line ?? '',
    plain: stripAnsi(line ?? '')
  }))
);

const normalizedKeyword = computed(() => (caseSensitive.value ? keyword.value : keyword.value.toLowerCase()));
const hasKeyword = computed(() => normalizedKeyword.value.length > 0);

const getMatchRanges = (line: LogLine): Array<[number, number]> => {
  if (!hasKeyword.value) return [];
  const text = caseSensitive.value ? line.plain : line.plain.toLowerCase();
  const searchKeyword = normalizedKeyword.value;
  const ranges: Array<[number, number]> = [];
  let searchIndex = 0;

  if (!searchKeyword) return ranges;

  while (searchIndex <= text.length) {
    const matchIndex = text.indexOf(searchKeyword, searchIndex);
    if (matchIndex < 0) break;
    ranges.push([matchIndex, matchIndex + searchKeyword.length]);
    searchIndex = matchIndex + searchKeyword.length;
  }

  return ranges;
};

const isMatch = (line: LogLine) => {
  if (!hasKeyword.value) return true;
  return getMatchRanges(line).length > 0;
};

const visibleLines = computed(() => {
  if (!filterEnabled.value || !hasKeyword.value) {
    return lineItems.value;
  }
  return lineItems.value.filter(isMatch);
});

const matchedItems = computed<MatchItem[]>(() => {
  if (!hasKeyword.value) return [];
  return lineItems.value.flatMap(line =>
    getMatchRanges(line).map(([start, end]) => ({
      lineNumber: line.lineNumber,
      start,
      end
    }))
  );
});

const matchCount = computed(() => matchedItems.value.length);
const activeMatch = computed(() => matchedItems.value[activeMatchIndex.value]);
const activeMatchDisplayIndex = computed(() => (activeMatchIndex.value >= 0 ? activeMatchIndex.value + 1 : 0));

const rowVirtualizer = useVirtualizer(
  computed(() => ({
    count: visibleLines.value.length,
    getScrollElement: () => parentRef.value,
    estimateSize: () => props.rowHeight,
    overscan: 12
  }))
);

const virtualItems = computed(() => rowVirtualizer.value.getVirtualItems());
const totalSize = computed(() => rowVirtualizer.value.getTotalSize());

const renderAnsi = (value: string) => ansiConverter.toHtml(value);

const scrollToLineNumber = async (lineNumber: number) => {
  const index = visibleLines.value.findIndex(line => line.lineNumber === lineNumber);
  if (index < 0) return;
  await nextTick();
  rowVirtualizer.value.scrollToIndex(index, { align: 'center' });
};

const focusMatch = (index: number) => {
  const total = matchedItems.value.length;
  if (!total) {
    activeMatchIndex.value = -1;
    return;
  }

  activeMatchIndex.value = (index + total) % total;
  void scrollToLineNumber(matchedItems.value[activeMatchIndex.value].lineNumber);
};

const focusPrevMatch = () => focusMatch(activeMatchIndex.value - 1);
const focusNextMatch = () => focusMatch(activeMatchIndex.value + 1);
const scrollToTop = () => rowVirtualizer.value.scrollToIndex(0, { align: 'start' });
const scrollToBottom = () => {
  const lastIndex = visibleLines.value.length - 1;
  if (lastIndex >= 0) {
    rowVirtualizer.value.scrollToIndex(lastIndex, { align: 'end' });
  }
};

const jumpToLine = () => {
  if (!jumpLineNumber.value) return;
  const targetLine = Math.min(Math.max(Math.trunc(jumpLineNumber.value), 1), lineItems.value.length);
  jumpLineNumber.value = targetLine;
  void scrollToLineNumber(targetLine);
};

const renderLineContent = (line: LogLine) => {
  if (!hasKeyword.value) return renderAnsi(line.raw);

  const source = line.raw;
  const matchedRanges = getMatchRanges(line);

  if (!matchedRanges.length) return renderAnsi(source);

  let plainIndex = 0;
  let rawChunk = '';
  let html = '';
  let rangeIndex = 0;
  let activeRange = matchedRanges[rangeIndex];
  let inMatch = false;

  const flushChunk = () => {
    if (!rawChunk) return;
    const rendered = renderAnsi(rawChunk);
    const isActiveMatch =
      inMatch &&
      activeMatch.value?.lineNumber === line.lineNumber &&
      activeMatch.value.start === activeRange?.[0] &&
      activeMatch.value.end === activeRange?.[1];
    html += inMatch
      ? `<mark class="rounded px-2px text-black ${
          isActiveMatch ? 'bg-orange-400 ring-1 ring-orange-200' : 'bg-yellow-300'
        }">${rendered}</mark>`
      : rendered;
    rawChunk = '';
  };

  for (let rawIndex = 0; rawIndex < source.length; rawIndex += 1) {
    const char = source[rawIndex];
    if (char === '\u001b' || char === '\u009b') {
      const rest = source.slice(rawIndex);
      const match = rest.match(ansiPattern);
      if (match?.index === 0) {
        rawChunk += match[0];
        rawIndex += match[0].length - 1;
        continue;
      }
    }

    if (activeRange && plainIndex === activeRange[0] && !inMatch) {
      flushChunk();
      inMatch = true;
    }
    if (activeRange && plainIndex === activeRange[1] && inMatch) {
      flushChunk();
      inMatch = false;
      rangeIndex += 1;
      activeRange = matchedRanges[rangeIndex];
    }

    rawChunk += char;
    plainIndex += 1;
  }

  flushChunk();
  return html;
};

watch(
  () => [visibleLines.value.length, props.rowHeight],
  () => {
    rowVirtualizer.value.measure();
  }
);

watch(
  () => [normalizedKeyword.value, filterEnabled.value, props.lines],
  () => {
    if (matchedItems.value.length) {
      focusMatch(0);
      return;
    }
    activeMatchIndex.value = -1;
  }
);
</script>

<template>
  <div class="grid gap-12px">
    <div class="grid gap-8px">
      <NInput v-model:value="keyword" clearable :placeholder="$t('common.largeViewer.searchPlaceholder')">
        <template #prefix>
          <SvgIcon icon="mdi:magnify" />
        </template>
      </NInput>

      <div class="flex flex-wrap items-center justify-between gap-12px">
        <NSpace align="center">
          <NCheckbox v-model:checked="filterEnabled" :disabled="!hasKeyword">
            {{ $t('common.largeViewer.filterMatches') }}
          </NCheckbox>
          <NCheckbox v-model:checked="caseSensitive">
            {{ $t('common.largeViewer.caseSensitive') }}
          </NCheckbox>
        </NSpace>

        <NSpace>
          <NTooltip>
            <template #trigger>
              <NButton :disabled="!visibleLines.length" @click="scrollToTop">
                <template #icon>
                  <SvgIcon icon="mdi:arrow-collapse-up" />
                </template>
              </NButton>
            </template>
            {{ $t('common.largeViewer.jumpTop') }}
          </NTooltip>
          <NTooltip>
            <template #trigger>
              <NButton :disabled="!visibleLines.length" @click="scrollToBottom">
                <template #icon>
                  <SvgIcon icon="mdi:arrow-collapse-down" />
                </template>
              </NButton>
            </template>
            {{ $t('common.largeViewer.jumpBottom') }}
          </NTooltip>
          <NTooltip>
            <template #trigger>
              <NButton :disabled="!matchCount" @click="focusPrevMatch">
                <template #icon>
                  <SvgIcon icon="mdi:chevron-up" />
                </template>
              </NButton>
            </template>
            {{ $t('common.largeViewer.prevMatch') }}
          </NTooltip>
          <NTooltip>
            <template #trigger>
              <NButton :disabled="!matchCount" @click="focusNextMatch">
                <template #icon>
                  <SvgIcon icon="mdi:chevron-down" />
                </template>
              </NButton>
            </template>
            {{ $t('common.largeViewer.nextMatch') }}
          </NTooltip>
        </NSpace>

        <div class="ml-auto flex flex-wrap items-center justify-end gap-8px">
          <NInputNumber
            v-model:value="jumpLineNumber"
            :min="1"
            :max="lineItems.length || 1"
            :disabled="!lineItems.length"
            :show-button="false"
            class="w-100px"
            :placeholder="$t('common.largeViewer.jumpLine')"
            @keydown.enter="jumpToLine"
          />
          <NButton :disabled="!lineItems.length" @click="jumpToLine">
            {{ $t('common.largeViewer.jump') }}
          </NButton>
          <div class="min-w-80px whitespace-nowrap text-right text-12px text-#999">
            {{
              hasKeyword
                ? $t('common.largeViewer.matchPosition', {
                    current: activeMatchDisplayIndex,
                    total: matchCount
                  })
                : $t('common.largeViewer.totalLines', { count: lines.length })
            }}
          </div>
        </div>
      </div>
    </div>

    <div class="relative overflow-hidden rounded-6px border border-white/10 bg-#111827">
      <div
        ref="parentRef"
        class="overflow-auto font-mono text-12px leading-24px text-#d4d4d4"
        :style="{ height: `${height}px` }"
      >
        <div v-if="visibleLines.length" class="relative" :style="{ height: `${totalSize}px` }">
          <div
            v-for="virtualRow in virtualItems"
            :key="visibleLines[virtualRow.index].lineNumber"
            class="absolute left-0 top-0 flex w-full min-w-max border-b border-white/5"
            :style="{
              height: `${virtualRow.size}px`,
              transform: `translateY(${virtualRow.start}px)`
            }"
          >
            <span
              class="sticky left-0 z-1 w-56px shrink-0 select-none border-r border-white/10 bg-#111827 pr-12px text-right text-#6b7280"
            >
              {{ visibleLines[virtualRow.index].lineNumber }}
            </span>
            <span class="whitespace-pre px-12px" v-html="renderLineContent(visibleLines[virtualRow.index])" />
          </div>
        </div>
        <div v-else class="grid h-full place-items-center p-24px text-#9ca3af">
          {{ emptyText || $t('common.largeViewer.emptyContent') }}
        </div>
      </div>

      <div v-if="loading" class="absolute inset-0 z-2 grid place-items-center bg-black/40 text-#d4d4d4">
        {{ $t('common.loading') }}
      </div>
    </div>
  </div>
</template>
