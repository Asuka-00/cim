import { reactive, ref, type Ref } from 'vue';
import type { FlatResponseData } from '@sa/axios';
import type { PageDTO } from '@/service/api/common';

export interface PageParams {
  page: number;
  pageSize: number;
}

export interface UsePageQueryOptions<T, Q extends object, P extends object> {
  initialQuery: Q;
  fetcher: (params: PageParams & P) => Promise<FlatResponseData<unknown, PageDTO<T>>>;
  buildQuery?: (query: Q) => P;
  pageSize?: number;
}

const cloneQuery = <Q extends object>(query: Q): Q =>
  Object.fromEntries(Object.entries(query).map(([k, v]) => [k, Array.isArray(v) ? [...v] : v])) as Q;

export function usePageQuery<T, Q extends object, P extends object = Q>(options: UsePageQueryOptions<T, Q, P>) {
  const loading = ref(false);
  const list = ref<T[]>([]) as Ref<T[]>;
  const total = ref(0);
  const currentPage = ref(1);
  const pageSize = ref(options.pageSize ?? 20);
  const searchParams = reactive(cloneQuery(options.initialQuery)) as Q;

  let requestSeq = 0;

  const fetchData = async () => {
    const seq = ++requestSeq;
    loading.value = true;
    try {
      const query = options.buildQuery ? options.buildQuery(searchParams) : (searchParams as unknown as P);
      const res = await options.fetcher({
        page: currentPage.value,
        pageSize: pageSize.value,
        ...query
      });
      if (seq !== requestSeq) return;
      list.value = res.data?.records ?? [];
      total.value = res.data?.total ?? 0;
    } finally {
      if (seq === requestSeq) loading.value = false;
    }
  };

  const handleSearch = () => {
    currentPage.value = 1;
    return fetchData();
  };

  const handleReset = () => {
    Object.assign(searchParams, cloneQuery(options.initialQuery));
    return handleSearch();
  };

  const onPageUpdate = (page: number) => {
    currentPage.value = page;
    fetchData();
  };

  const onSizeUpdate = (size: number) => {
    pageSize.value = size;
    currentPage.value = 1;
    fetchData();
  };

  return {
    loading,
    list,
    total,
    currentPage,
    pageSize,
    searchParams,
    fetchData,
    handleSearch,
    handleReset,
    onPageUpdate,
    onSizeUpdate
  };
}
