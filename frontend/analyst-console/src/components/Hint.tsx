import { useI18n } from '../i18n';

/** Small ⓘ icon with a hover/focus tooltip — explains AML / compliance jargon inline. */
export function Hint({ text }: { text: string }) {
  return (
    <span className="hint" tabIndex={0} role="note" aria-label={text}>
      <span className="hint-icon">i</span>
      <span className="hint-text">{text}</span>
    </span>
  );
}

/** Domain-term glossary, bilingual. Keep wording plain — these explain jargon to non-experts. */
const GLOSSARY = {
  aml: {
    en: 'AML (Anti-Money Laundering): rules and checks that detect funds linked to crime — here, by scoring a wallet against suspicious on-chain patterns.',
    zh: 'AML(反洗錢):用以偵測涉及犯罪資金的規則與檢查 —— 在此即依據可疑的鏈上行為模式為錢包評分。',
  },
  riskScore: {
    en: 'Risk score (0–100): a single number combining every rule that fired, weighted by severity. Higher = more suspicious.',
    zh: '風險評分(0–100):結合所有觸發規則、依嚴重度加權後的單一數字。越高代表越可疑。',
  },
  riskBand: {
    en: 'Risk band: the score bucketed into LOW / MEDIUM / HIGH / CRITICAL, so analysts can triage at a glance.',
    zh: '風險等級:將評分歸入 LOW / MEDIUM / HIGH / CRITICAL,讓分析師一眼分流。',
  },
  blacklist: {
    en: 'Blacklist exposure: the wallet transacted with an address known to be sanctioned, stolen-funds, or otherwise flagged — a strong AML signal.',
    zh: '黑名單暴露:該錢包曾與已知受制裁、贓款或其他被標記的地址往來 —— 屬強烈的 AML 訊號。',
  },
  rulesFired: {
    en: 'Rules fired: the specific AML detection rules this wallet matched. Each contributes points to the total risk score.',
    zh: '觸發規則:該錢包命中的具體 AML 偵測規則。每條都為總風險評分貢獻分數。',
  },
  caseStatus: {
    en: 'Case lifecycle: OPEN (just raised) → REVIEWING (analyst working it) → ESCALATED (sent to a senior reviewer) → CLOSED (resolved). Transitions are enforced server-side.',
    zh: '案件生命週期:OPEN(剛提出)→ REVIEWING(分析師處理中)→ ESCALATED(上呈資深審查)→ CLOSED(已結案)。狀態轉換由伺服器端強制。',
  },
  assignee: {
    en: 'Assignee: the analyst responsible for working the case. Assigned automatically when a case is opened.',
    zh: '承辦人:負責處理該案件的分析師。開立案件時自動指派。',
  },
  aiSummary: {
    en: 'AI summary: a draft written by an LLM from the risk signals and analyst notes — a narrative, key risk factors, and recommended next actions. A starting point, not a verdict.',
    zh: 'AI 摘要:由大型語言模型根據風險訊號與分析師筆記草擬 —— 含敘述、關鍵風險因子與建議動作。僅為起點,而非定論。',
  },
  confidence: {
    en: 'Confidence: how sure the AI model is about its draft, given the signal strength. Always verify before acting.',
    zh: '信心度:在現有訊號強度下,AI 模型對其草稿的把握程度。採取行動前務必人工核實。',
  },
} as const;

export type GlossaryKey = keyof typeof GLOSSARY;

/** Hint bound to the current language. */
export function TermHint({ term }: { term: GlossaryKey }) {
  const { lang } = useI18n();
  return <Hint text={GLOSSARY[term][lang]} />;
}
