export {};

declare global {
  export interface Window {
    /** NProgress instance */
    NProgress?: import('nprogress').NProgress;
    /** Loading bar instance */
    $loadingBar?: import('naive-ui').LoadingBarProviderInst;
    /** Dialog instance */
    $dialog?: import('naive-ui').DialogProviderInst;
    /** Message instance */
    $message?: import('naive-ui').MessageProviderInst;
    /** Notification instance */
    $notification?: import('naive-ui').NotificationProviderInst;
  }

  /** Build time of the project */
  export const BUILD_TIME: string;
  /** public/semi-logo-default.svg, else public/logo.{svg,png,ico} */
  export const BRAND_DEFAULT_LOGO: string;
  /** public/semi-logo-small.svg, else public/favicon.{svg,png,ico}, else logo */
  export const BRAND_DEFAULT_FAVICON: string;
}
