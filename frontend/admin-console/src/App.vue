<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';

type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
type AmlRule = {
  id: string;
  code: string;
  name: string;
  severity: Severity;
  threshold: string;
  enabled: boolean;
  version: number;
  createdAt?: string;
  updatedAt?: string;
};

type Lang = 'en' | 'zh';

const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';
const REPO = 'https://github.com/Redchar1992/chainguard-casehub';

/* ---------- i18n (EN / 繁中) ---------- */
const DICT: Record<string, { en: string; zh: string }> = {
  tagline: { en: 'AML rule administration for ChainGuard CaseHub', zh: 'ChainGuard CaseHub 的 AML 規則管理' },
  'hero.headline': { en: 'Tune the rules that decide which wallets get flagged.', zh: '調校決定哪些錢包被標記的規則。' },
  'hero.sub': {
    en: 'The admin console governs the AML rule set behind ChainGuard CaseHub: enable or retire detection rules, review their severity and thresholds, and track each rule’s version — every wallet risk score downstream is built from exactly these rules.',
    zh: '管理主控台掌管 ChainGuard CaseHub 背後的 AML 規則集:啟用或停用偵測規則、檢視其嚴重度與門檻,並追蹤每條規則的版本 —— 下游所有錢包風險評分皆由這些規則構成。',
  },
  'hero.def': {
    en: 'ChainGuard — a compliance watchdog guarding on-chain activity. CaseHub — your hub for compliance investigation cases. This console governs the rules powering both.',
    zh: 'ChainGuard —— 守護鏈上活動的合規看門人。CaseHub —— 你的合規調查案件中樞。本主控台掌管驅動兩者的規則。',
  },
  'hero.cta': { en: 'Manage rules', zh: '管理規則' },
  'how.title': { en: 'How it works', zh: '運作方式' },
  'how.s1t': { en: 'Define detection rules', zh: '定義偵測規則' },
  'how.s1d': { en: 'Each AML rule has a severity and a threshold (e.g. transfer count, address age) that decides when it fires.', zh: '每條 AML 規則都有嚴重度與門檻(如轉帳次數、地址年齡),決定何時觸發。' },
  'how.s2t': { en: 'Enable & version', zh: '啟用與版本控管' },
  'how.s2d': { en: 'Toggle a rule on or off; every change bumps its version so the rule set is fully auditable.', zh: '開關規則;每次變更都會提升其版本,使規則集完全可稽核。' },
  'how.s3t': { en: 'Powers risk scoring', zh: '驅動風險評分' },
  'how.s3d': { en: 'Enabled rules are exactly what the risk engine evaluates wallets against to produce a score.', zh: '已啟用的規則正是風險引擎用來評估錢包、產生評分的依據。' },
  'how.go': { en: 'Open →', zh: '前往 →' },
  'sec.overview': { en: 'Rule set overview', zh: '規則集總覽' },
  'sec.manage': { en: 'AML rule management', zh: 'AML 規則管理' },
  'stat.enabled': { en: 'Enabled rules', zh: '已啟用規則' },
  'stat.critical': { en: 'Critical rules', zh: '嚴重等級規則' },
  'stat.version': { en: 'Max rule version', zh: '最高規則版本' },
  refresh: { en: 'Refresh rules', zh: '重新整理規則' },
  refreshing: { en: 'Loading…', zh: '載入中…' },
  'col.code': { en: 'Code', zh: '代碼' },
  'col.name': { en: 'Name', zh: '名稱' },
  'col.severity': { en: 'Severity', zh: '嚴重度' },
  'col.threshold': { en: 'Threshold', zh: '門檻' },
  'col.version': { en: 'Version', zh: '版本' },
  'col.enabled': { en: 'Enabled', zh: '啟用' },
  'auth.live': { en: 'Admin authenticated', zh: '管理員已驗證' },
  'auth.demo': { en: 'Offline demo mode', zh: '離線示範模式' },
  'foot.note': {
    en: 'Wired to the ChainGuard CaseHub backend (Spring Cloud). RBAC-protected: only admins may edit rules. Falls back to built-in demo rules when the API is unreachable.',
    zh: '已串接 ChainGuard CaseHub 後端(Spring Cloud)。受 RBAC 保護:僅管理員可編輯規則。API 無法連線時自動回退至內建示範規則。',
  },
};

const HINTS: Record<string, { en: string; zh: string }> = {
  aml: {
    en: 'AML (Anti-Money Laundering): rules and checks that detect funds linked to crime. Each rule below contributes to a wallet’s risk score when it fires.',
    zh: 'AML(反洗錢):用以偵測涉及犯罪資金的規則與檢查。下方每條規則觸發時都會貢獻至錢包的風險評分。',
  },
  severity: {
    en: 'Severity: how serious a match is (LOW → CRITICAL). Higher-severity rules add more to the risk score and push a wallet toward a higher risk band.',
    zh: '嚴重度:命中時的嚴重程度(LOW → CRITICAL)。嚴重度越高,對風險評分的加分越多,越會把錢包推向更高風險等級。',
  },
  threshold: {
    en: 'Threshold: the parameters that decide when a rule fires — e.g. transfer count within a window, address age, or amount. Stored as JSON.',
    zh: '門檻:決定規則何時觸發的參數 —— 如時間窗內的轉帳次數、地址年齡或金額。以 JSON 儲存。',
  },
  version: {
    en: 'Version: increments on every change to a rule, giving an auditable history of who tuned what and when.',
    zh: '版本:每次規則變更時遞增,提供可稽核的歷史 —— 誰在何時調整了什麼。',
  },
  rbac: {
    en: 'RBAC (Role-Based Access Control): permissions tied to roles. Only the ADMIN role may enable, disable, or edit AML rules; analysts and reviewers cannot.',
    zh: 'RBAC(角色型存取控制):權限綁定角色。僅 ADMIN 角色可啟用、停用或編輯 AML 規則;分析師與審查者不可。',
  },
  blacklist: {
    en: 'Blacklist exposure: a wallet transacted with a known sanctioned / stolen-funds / flagged address — typically a CRITICAL AML signal.',
    zh: '黑名單暴露:錢包與已知受制裁/贓款/被標記地址往來 —— 通常屬 CRITICAL 級 AML 訊號。',
  },
};

const lang = ref<Lang>((() => {
  try { return (localStorage.getItem('chainguard.lang') as Lang) || 'en'; } catch { return 'en'; }
})());
function t(key: string) { return DICT[key]?.[lang.value] ?? key; }
function hint(key: string) { return HINTS[key]?.[lang.value] ?? ''; }
function toggleLang() {
  lang.value = lang.value === 'en' ? 'zh' : 'en';
  try { localStorage.setItem('chainguard.lang', lang.value); } catch { /* ignore */ }
}

/* ---------- toast ---------- */
type Toast = { id: number; type: 'success' | 'warning' | 'error' | 'info'; text: string };
const toasts = reactive<Toast[]>([]);
let toastSeq = 0;
function pushToast(type: Toast['type'], text: string) {
  const id = ++toastSeq;
  toasts.push({ id, type, text });
  setTimeout(() => {
    const i = toasts.findIndex((x) => x.id === id);
    if (i >= 0) toasts.splice(i, 1);
  }, 3600);
}

/* ---------- data ---------- */
const rules = ref<AmlRule[]>([
  { id: 'demo-1', code: 'BLACKLIST_EXPOSURE', name: 'Blacklist Exposure', severity: 'CRITICAL', threshold: '{"counterpartyTag":"blacklist"}', enabled: true, version: 1 },
  { id: 'demo-2', code: 'HIGH_FREQUENCY_TRANSFER', name: 'High Frequency Transfer', severity: 'HIGH', threshold: '{"count":20,"windowMinutes":30}', enabled: true, version: 1 },
  { id: 'demo-3', code: 'NEW_ADDRESS_LARGE_WITHDRAWAL', name: 'New Address Large Withdrawal', severity: 'HIGH', threshold: '{"addressAgeDays":7,"amountUsd":10000}', enabled: true, version: 1 },
  { id: 'demo-4', code: 'MULTI_HOP_OBFUSCATION', name: 'Multi-hop Obfuscation', severity: 'MEDIUM', threshold: '{"hops":4,"windowMinutes":60}', enabled: false, version: 1 },
]);
const token = ref<string | null>(null);
const loading = ref(false);
const authed = computed(() => Boolean(token.value));
const enabledCount = computed(() => rules.value.filter((r) => r.enabled).length);
const criticalCount = computed(() => rules.value.filter((r) => r.severity === 'CRITICAL').length);
const maxVersion = computed(() => Math.max(...rules.value.map((r) => r.version), 1));

function sevClass(severity: Severity) {
  return `badge sev-${severity.toLowerCase()}`;
}

async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set('Content-Type', 'application/json');
  if (token.value) headers.set('Authorization', `Bearer ${token.value}`);
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (!response.ok) throw new Error(`API ${path} failed with status ${response.status}`);
  return response.json();
}

async function login() {
  const response = await fetch(`${API_BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin@chainguard.demo', password: 'demo-password' }),
  });
  if (!response.ok) throw new Error('Login failed');
  const body = await response.json();
  token.value = body.accessToken;
}

async function loadRules() {
  loading.value = true;
  try {
    if (!token.value) await login();
    rules.value = await apiRequest<AmlRule[]>('/api/rules');
    pushToast('success', 'AML rules loaded from backend');
  } catch {
    pushToast('warning', 'Backend unavailable — showing built-in demo rules.');
  } finally {
    loading.value = false;
  }
}

async function toggleRule(rule: AmlRule) {
  if (!token.value || rule.id.startsWith('demo-')) {
    pushToast('info', 'Offline demo mode: switch changed locally only.');
    return;
  }
  try {
    const updated = await apiRequest<AmlRule>(`/api/rules/${rule.id}/enabled`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled: rule.enabled }),
    });
    const index = rules.value.findIndex((item) => item.id === updated.id);
    if (index >= 0) rules.value[index] = updated;
    pushToast('success', `${updated.code} updated to v${updated.version}`);
  } catch {
    rule.enabled = !rule.enabled;
    pushToast('error', 'Failed to update rule. Change reverted.');
  }
}

onMounted(loadRules);
</script>

<template>
  <div class="app">
    <header class="topbar">
      <div class="brand">
        <span class="logo">ChainGuard CaseHub</span>
        <span class="tagline">{{ t('tagline') }}</span>
      </div>
      <div class="topbar-right">
        <button class="lang-toggle" @click="toggleLang">{{ lang === 'en' ? '繁中' : 'EN' }}</button>
        <span class="auth-pill" :class="authed ? 'live' : 'demo'">
          <span class="auth-dot" />
          {{ authed ? t('auth.live') : t('auth.demo') }}
        </span>
      </div>
    </header>

    <!-- hero -->
    <div class="hero">
      <div class="hero-glow" aria-hidden="true" />
      <div class="hero-text">
        <h1>{{ t('hero.headline') }}</h1>
        <p>{{ t('hero.sub') }}</p>
        <div class="namecard">
          <span class="term">ChainGuard CaseHub · Admin</span>
          <span class="def">{{ t('hero.def') }}</span>
        </div>
        <div><a class="cta" href="#rules">{{ t('hero.cta') }} →</a></div>
      </div>
      <div class="hero-art">
        <svg viewBox="0 0 280 180" class="hero-art-svg" role="img" aria-hidden="true">
          <defs>
            <linearGradient id="cgLg" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0" stop-color="#6aa0ff" />
              <stop offset="1" stop-color="#bcd4ff" />
            </linearGradient>
            <radialGradient id="cgGlow" cx="50%" cy="50%" r="50%">
              <stop offset="0" stop-color="#6aa0ff" stop-opacity="0.22" />
              <stop offset="1" stop-color="#6aa0ff" stop-opacity="0" />
            </radialGradient>
            <radialGradient id="cgScan" cx="0%" cy="0%" r="100%">
              <stop offset="0" stop-color="#6aa0ff" stop-opacity="0.32" />
              <stop offset="1" stop-color="#6aa0ff" stop-opacity="0" />
            </radialGradient>
            <clipPath id="cgRadarClip"><circle cx="140" cy="90" r="58" /></clipPath>
          </defs>
          <ellipse cx="140" cy="90" rx="124" ry="84" fill="url(#cgGlow)" />
          <g clip-path="url(#cgRadarClip)">
            <circle cx="140" cy="90" r="58" fill="none" stroke="#232a36" stroke-width="1" />
            <circle cx="140" cy="90" r="38" fill="none" stroke="#232a36" stroke-width="1" />
            <circle cx="140" cy="90" r="18" fill="none" stroke="#232a36" stroke-width="1" />
            <g class="cg-scan">
              <path d="M140 90 L140 32 A58 58 0 0 1 198 90 Z" fill="url(#cgScan)" />
              <line x1="140" y1="90" x2="140" y2="32" stroke="#6aa0ff" stroke-width="1.2" stroke-opacity="0.7" />
            </g>
          </g>
          <g class="cg-shield">
            <path d="M140 44 L176 56 V92 C176 116 160 130 140 138 C120 130 104 116 104 92 V56 Z" fill="none" stroke="url(#cgLg)" stroke-width="3.2" stroke-linejoin="round" />
            <path d="M126 90 L137 101 L156 78" fill="none" stroke="url(#cgLg)" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round" />
          </g>
          <g fill="none" stroke="url(#cgLg)" stroke-width="3" stroke-opacity="0.85">
            <rect x="56" y="138" width="40" height="24" rx="12" />
            <rect x="90" y="138" width="40" height="24" rx="12" />
            <rect x="124" y="138" width="40" height="24" rx="12" />
            <rect x="158" y="138" width="40" height="24" rx="12" />
            <rect x="192" y="138" width="40" height="24" rx="12" />
          </g>
        </svg>
      </div>
    </div>

    <!-- how it works -->
    <div class="how">
      <div class="section-title">{{ t('how.title') }}</div>
      <div class="steps">
        <a class="step" href="#rules">
          <div class="step-num">1</div>
          <div class="step-title">{{ t('how.s1t') }}</div>
          <div class="step-desc">{{ t('how.s1d') }}</div>
          <span class="step-cue">{{ t('how.go') }}</span>
        </a>
        <a class="step" href="#rules">
          <div class="step-num">2</div>
          <div class="step-title">{{ t('how.s2t') }}</div>
          <div class="step-desc">{{ t('how.s2d') }}</div>
          <span class="step-cue">{{ t('how.go') }}</span>
        </a>
        <a class="step" href="#rules">
          <div class="step-num">3</div>
          <div class="step-title">{{ t('how.s3t') }}</div>
          <div class="step-desc">{{ t('how.s3d') }}</div>
          <span class="step-cue">{{ t('how.go') }}</span>
        </a>
      </div>
    </div>

    <div class="subbar">
      <span class="chip">Spring Cloud backend</span>
      <span class="dot">·</span>
      <span class="chip">{{ authed ? 'live API' : 'offline demo' }}</span>
      <span class="dot">·</span>
      <a class="link" :href="REPO" target="_blank" rel="noreferrer">GitHub ↗</a>
    </div>

    <div v-if="!authed" class="banner">
      Not connected to the API. Start the backend and set <code>VITE_API_BASE</code> (default <code>{{ API_BASE }}</code>). Until then you are editing built-in demo rules locally.
    </div>

    <!-- overview stats -->
    <div class="section-title">{{ t('sec.overview') }}</div>
    <div class="grid-3">
      <div class="stat">
        <div class="stat-label">{{ t('stat.enabled') }}</div>
        <div class="stat-value accent">{{ enabledCount }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">
          {{ t('stat.critical') }}
          <span class="hint" tabindex="0" role="note" :aria-label="hint('severity')">
            <span class="hint-icon">i</span><span class="hint-text">{{ hint('severity') }}</span>
          </span>
        </div>
        <div class="stat-value critical">{{ criticalCount }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">
          {{ t('stat.version') }}
          <span class="hint" tabindex="0" role="note" :aria-label="hint('version')">
            <span class="hint-icon">i</span><span class="hint-text">{{ hint('version') }}</span>
          </span>
        </div>
        <div class="stat-value">v{{ maxVersion }}</div>
      </div>
    </div>

    <!-- rule table -->
    <section class="section" id="rules">
      <div class="card">
        <div class="card-title">
          <span class="ttl">
            {{ t('sec.manage') }}
            <span class="hint" tabindex="0" role="note" :aria-label="hint('aml')">
              <span class="hint-icon">i</span><span class="hint-text">{{ hint('aml') }}</span>
            </span>
            <span class="hint" tabindex="0" role="note" :aria-label="hint('rbac')">
              <span class="hint-icon">i</span><span class="hint-text">{{ hint('rbac') }}</span>
            </span>
          </span>
          <button class="primary" :disabled="loading" @click="loadRules">
            {{ loading ? t('refreshing') : t('refresh') }}
          </button>
        </div>

        <table class="tbl">
          <thead>
            <tr>
              <th>{{ t('col.code') }}</th>
              <th>{{ t('col.name') }}</th>
              <th>
                {{ t('col.severity') }}
                <span class="hint" tabindex="0" role="note" :aria-label="hint('severity')">
                  <span class="hint-icon">i</span><span class="hint-text">{{ hint('severity') }}</span>
                </span>
              </th>
              <th>
                {{ t('col.threshold') }}
                <span class="hint" tabindex="0" role="note" :aria-label="hint('threshold')">
                  <span class="hint-icon">i</span><span class="hint-text">{{ hint('threshold') }}</span>
                </span>
              </th>
              <th>{{ t('col.version') }}</th>
              <th>{{ t('col.enabled') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="rule in rules" :key="rule.id">
              <td class="code-cell">{{ rule.code }}</td>
              <td>{{ rule.name }}</td>
              <td><span :class="sevClass(rule.severity)">{{ rule.severity }}</span></td>
              <td class="thr-cell">{{ rule.threshold }}</td>
              <td class="ver-cell">v{{ rule.version }}</td>
              <td>
                <label class="switch">
                  <input type="checkbox" v-model="rule.enabled" @change="toggleRule(rule)" />
                  <span class="track"><span class="knob" /></span>
                </label>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <footer class="foot">
      <p>{{ t('foot.note') }}</p>
      <p class="foot-links">
        <a :href="REPO" target="_blank" rel="noreferrer">source</a>
        <a :href="`${API_BASE}/api/rules`" target="_blank" rel="noreferrer">rules API</a>
      </p>
    </footer>

    <!-- toasts -->
    <div class="toast-stack" aria-live="polite">
      <transition-group name="toast">
        <div v-for="ts in toasts" :key="ts.id" class="toast" :class="ts.type">
          <span class="toast-dot" />{{ ts.text }}
        </div>
      </transition-group>
    </div>
  </div>
</template>
