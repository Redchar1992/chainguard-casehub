import { createContext, useContext, useState, type ReactNode } from 'react';

export type Lang = 'en' | 'zh';

/** Flat dictionary: key → { en, zh(繁中) }. Proper nouns / codes left untranslated. */
export const DICT = {
  tagline: { en: 'AML compliance case management for on-chain activity', zh: '鏈上活動的 AML 合規案件管理' },

  // hero
  'hero.headline': {
    en: 'Score a wallet, open a case, investigate it — without leaving the console.',
    zh: '評分錢包、開立案件、展開調查 —— 全程不離開主控台。',
  },
  'hero.sub': {
    en: 'ChainGuard CaseHub turns raw on-chain activity into an AML risk score, lets you open a compliance case in one click, drafts an AI investigation summary, and moves the case through a review workflow to resolution.',
    zh: 'ChainGuard CaseHub 將原始鏈上活動轉化為 AML 風險評分,讓你一鍵開立合規案件,自動草擬 AI 調查摘要,並以審查流程推進案件直至結案。',
  },
  'hero.def': {
    en: 'ChainGuard — a compliance watchdog guarding on-chain activity. CaseHub — your hub for compliance investigation cases, from open to close.',
    zh: 'ChainGuard —— 守護鏈上活動的合規看門人。CaseHub —— 你的合規調查案件中樞,從開立到結案。',
  },
  'hero.cta': { en: 'Open the workbench', zh: '進入工作台' },

  // how it works
  'how.title': { en: 'How it works', zh: '運作方式' },
  'how.s1t': { en: 'Score the wallet', zh: '評分錢包' },
  'how.s1d': {
    en: 'Enter an address and get an AML risk score and band, with the exact rules that fired.',
    zh: '輸入地址,取得 AML 風險評分與等級,並列出觸發的具體規則。',
  },
  'how.s2t': { en: 'Open a case', zh: '開立案件' },
  'how.s2d': {
    en: 'Promote a risky wallet into a compliance case, assigned and tracked through its lifecycle.',
    zh: '將高風險錢包升級為合規案件,指派負責人並追蹤其生命週期。',
  },
  'how.s3t': { en: 'AI investigation', zh: 'AI 調查' },
  'how.s3d': {
    en: 'Generate an AI draft summarizing risk factors and recommended next actions for the analyst.',
    zh: '生成 AI 草稿,歸納風險因子與建議的後續動作,供分析師參考。',
  },
  'how.s4t': { en: 'Review & resolve', zh: '審查與結案' },
  'how.s4d': {
    en: 'Move the case OPEN → REVIEWING → ESCALATED → CLOSED through a controlled workflow.',
    zh: '透過受控流程推進案件 OPEN → REVIEWING → ESCALATED → CLOSED。',
  },
  'how.go': { en: 'Open →', zh: '前往 →' },

  // sections
  'sec.workbench': { en: 'Investigation workbench', zh: '調查工作台' },
  'sec.score': { en: 'Wallet risk', zh: '錢包風險' },
  'sec.case': { en: 'Case lifecycle', zh: '案件生命週期' },

  // wallet investigation
  'wallet.title': { en: 'Wallet investigation', zh: '錢包調查' },
  'wallet.placeholder': { en: 'Enter wallet address', zh: '輸入錢包地址' },
  'wallet.evaluate': { en: 'Evaluate risk', zh: '評估風險' },
  'wallet.evaluating': { en: 'Evaluating…', zh: '評估中…' },

  // risk score
  'risk.score': { en: 'Risk score', zh: '風險評分' },
  'risk.band': { en: 'Risk band', zh: '風險等級' },
  'risk.rules': { en: 'Rules fired', zh: '觸發規則' },
  'risk.cached': { en: 'served from cache', zh: '來自快取' },
  'risk.fresh': { en: 'freshly evaluated', zh: '即時評估' },
  'risk.triggered': { en: 'Triggered AML rules', zh: '觸發的 AML 規則' },
  'risk.noRules': { en: 'No rules fired for this wallet — looks clean.', zh: '此錢包未觸發任何規則 —— 看起來乾淨。' },

  // case
  'case.title': { en: 'Compliance case', zh: '合規案件' },
  'case.create': { en: 'Open case for this wallet', zh: '為此錢包開立案件' },
  'case.creating': { en: 'Opening…', zh: '開立中…' },
  'case.none': { en: 'No case yet. Open one from a scored wallet to start the workflow.', zh: '尚無案件。從已評分的錢包開立案件即可啟動流程。' },
  'case.id': { en: 'Case ID', zh: '案件編號' },
  'case.wallet': { en: 'Wallet', zh: '錢包' },
  'case.assignee': { en: 'Assignee', zh: '承辦人' },
  'case.status': { en: 'Status', zh: '狀態' },
  'case.advance': { en: 'Advance status', zh: '推進狀態' },
  'case.moveTo': { en: 'Move to', zh: '移至' },

  // AI
  'ai.title': { en: 'AI investigation summary', zh: 'AI 調查摘要' },
  'ai.generate': { en: 'Generate AI summary', zh: '生成 AI 摘要' },
  'ai.generating': { en: 'Generating…', zh: '生成中…' },
  'ai.empty': { en: 'Open a case, then generate an AI summary to draft an investigation: a plain-language narrative, the key risk factors, and recommended next actions.', zh: '先開立案件,再生成 AI 摘要以草擬調查:白話敘述、關鍵風險因子,以及建議的後續動作。' },
  'ai.factors': { en: 'Risk factors', zh: '風險因子' },
  'ai.actions': { en: 'Recommended actions', zh: '建議動作' },
  'ai.confidence': { en: 'Confidence', zh: '信心度' },

  // auth
  'auth.live': { en: 'Analyst authenticated', zh: '分析師已驗證' },
  'auth.demo': { en: 'Offline demo mode', zh: '離線示範模式' },

  // footer
  'foot.note': {
    en: 'Wired to the ChainGuard CaseHub backend (Spring Cloud: auth · risk-engine · case-service · ai-investigator). Falls back to a built-in demo when the API is unreachable.',
    zh: '已串接 ChainGuard CaseHub 後端(Spring Cloud:auth · risk-engine · case-service · ai-investigator)。API 無法連線時自動回退至內建示範資料。',
  },
} as const;

export type I18nKey = keyof typeof DICT;

interface I18nValue {
  lang: Lang;
  setLang: (l: Lang) => void;
  t: (key: I18nKey) => string;
}

const Ctx = createContext<I18nValue | null>(null);

export function LangProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>(() => {
    try {
      return (localStorage.getItem('chainguard.lang') as Lang) || 'en';
    } catch {
      return 'en';
    }
  });
  const setLang = (l: Lang) => {
    setLangState(l);
    try {
      localStorage.setItem('chainguard.lang', l);
    } catch {
      /* ignore */
    }
  };
  const t = (key: I18nKey) => DICT[key][lang];
  return <Ctx.Provider value={{ lang, setLang, t }}>{children}</Ctx.Provider>;
}

export function useI18n(): I18nValue {
  const c = useContext(Ctx);
  if (!c) throw new Error('useI18n must be used within LangProvider');
  return c;
}
