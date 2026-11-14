import { create } from "zustand";
import { enMessages } from "./en";
import { viMessages } from "./vi";

export type Locale = "vi" | "en";

const DICTIONARIES = {
  vi: viMessages,
  en: enMessages,
} as const;

type I18nState = {
  locale: Locale;
  setLocale: (locale: Locale) => void;
};

export const useI18nStore = create<I18nState>((set) => ({
  locale: "vi",
  setLocale: (locale) => set({ locale }),
}));

export function translate(locale: Locale, key: string): string {
  const dict = DICTIONARIES[locale] ?? DICTIONARIES.vi;
  const parts = key.split(".");
  let current: unknown = dict;
  for (const part of parts) {
    if (current && typeof current === "object" && part in current) {
      current = (current as Record<string, unknown>)[part];
    } else {
      return key;
    }
  }
  return typeof current === "string" ? current : key;
}

export function useI18n() {
  const locale = useI18nStore((s) => s.locale);
  const setLocale = useI18nStore((s) => s.setLocale);
  return {
    locale,
    setLocale,
    t: (key: string) => translate(locale, key),
  };
}
