import { createI18n } from 'vue-i18n';
import zhCN from './zh-CN.json';
import enUS from './en-US.json';

export type LocaleKey = 'zh-CN' | 'en-US';

export const LOCALE_STORAGE_KEY = 'qa-live-locale';

export const SUPPORTED_LOCALES: LocaleKey[] = ['zh-CN', 'en-US'];

export const DEFAULT_LOCALE: LocaleKey = 'zh-CN';

/** 从 localStorage 读取上次选择的语言，无效或缺失时回落到默认语言 */
export function resolveInitialLocale(): LocaleKey {
  try {
    const stored = window.localStorage.getItem(LOCALE_STORAGE_KEY);
    if (stored && (SUPPORTED_LOCALES as string[]).includes(stored)) {
      return stored as LocaleKey;
    }
  } catch (e) {
    // localStorage 不可用（隐私模式等）时静默回落到默认语言
  }
  return DEFAULT_LOCALE;
}

/** 持久化当前语言，刷新后保持上次选择 */
export function persistLocale(locale: LocaleKey) {
  try {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, locale);
  } catch (e) {
    // 忽略存储异常，不影响语言切换本身
  }
}

export const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: resolveInitialLocale(),
  fallbackLocale: DEFAULT_LOCALE,
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
});

/** 切换语言并持久化，返回切换后的语言 */
export function setLocale(locale: LocaleKey) {
  i18n.global.locale.value = locale;
  persistLocale(locale);
  if (typeof document !== 'undefined') {
    document.documentElement.setAttribute('lang', locale);
  }
  return locale;
}

export default i18n;
