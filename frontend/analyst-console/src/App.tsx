import { useEffect, useMemo, useState } from 'react';
import { Hero, HowItWorks } from './components/Hero';
import { TermHint } from './components/Hint';
import { useToast } from './components/Toast';
import { useI18n } from './i18n';

type TriggeredRule = {
  code: string;
  severity: string;
  description: string;
  scoreImpact: number;
};

type WalletRisk = {
  walletAddress: string;
  riskScore: number;
  riskLevel: string;
  triggeredRules: TriggeredRule[];
  cached: boolean;
};

type CaseResponse = {
  id: string;
  walletAddress: string;
  title: string;
  status: string;
  riskScore: number;
  riskLevel: string;
  assignee: string;
  createdAt: string;
  updatedAt: string;
};

type AiSummary = {
  summary: string;
  riskFactors: string[];
  recommendedActions: string[];
  confidence: string;
};

const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';
const REPO = 'https://github.com/Redchar1992/chainguard-casehub';

const CASE_STATES = ['OPEN', 'REVIEWING', 'ESCALATED', 'CLOSED'] as const;
type CaseState = (typeof CASE_STATES)[number];

/** Mirror of the backend state machine (CaseStatus.java) so the UI can offer only legal moves. */
const ALLOWED_TRANSITIONS: Record<CaseState, CaseState[]> = {
  OPEN: ['REVIEWING', 'CLOSED'],
  REVIEWING: ['ESCALATED', 'CLOSED', 'OPEN'],
  ESCALATED: ['REVIEWING', 'CLOSED'],
  CLOSED: [],
};

const demoRisk: WalletRisk = {
  walletAddress: '0x00new-blacklist-bad0',
  riskScore: 90,
  riskLevel: 'CRITICAL',
  cached: false,
  triggeredRules: [
    {
      code: 'BLACKLIST_EXPOSURE',
      severity: 'CRITICAL',
      description: 'Wallet interacted with a known blacklisted address.',
      scoreImpact: 45,
    },
    {
      code: 'HIGH_FREQUENCY_TRANSFER',
      severity: 'HIGH',
      description: 'Wallet shows high-frequency transfer behavior in a short time window.',
      scoreImpact: 25,
    },
    {
      code: 'NEW_ADDRESS_LARGE_WITHDRAWAL',
      severity: 'HIGH',
      description: 'New wallet received or withdrew a large amount shortly after creation.',
      scoreImpact: 20,
    },
  ],
};

/** Map an AML level/severity to a theme color token suffix. */
function riskKey(level: string): 'low' | 'medium' | 'high' | 'critical' {
  switch (level?.toUpperCase()) {
    case 'CRITICAL':
      return 'critical';
    case 'HIGH':
      return 'high';
    case 'MEDIUM':
      return 'medium';
    default:
      return 'low';
  }
}

async function apiRequest<T>(path: string, token: string | null, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set('Content-Type', 'application/json');
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });

  if (!response.ok) {
    throw new Error(`API ${path} failed with status ${response.status}`);
  }

  return response.json();
}

function SeverityBadge({ level }: { level: string }) {
  return <span className={`badge sev-${riskKey(level)}`}>{level?.toUpperCase()}</span>;
}

function Pipeline({ status }: { status: CaseState }) {
  const currentIdx = CASE_STATES.indexOf(status);
  const subs: Record<CaseState, string> = {
    OPEN: 'raised',
    REVIEWING: 'analyst working',
    ESCALATED: 'senior review',
    CLOSED: 'resolved',
  };
  return (
    <div className="pipeline" role="list">
      {CASE_STATES.map((s, i) => {
        const cls = i < currentIdx ? 'done' : i === currentIdx ? 'active' : '';
        return (
          <div className={`pl-step ${cls}`} role="listitem" key={s}>
            <div className="pl-name">{s}</div>
            <div className="pl-sub">{subs[s]}</div>
          </div>
        );
      })}
    </div>
  );
}

export function App() {
  const { lang, setLang, t } = useI18n();
  const toast = useToast();

  const [wallet, setWallet] = useState('0x00new-blacklist-bad0');
  const [risk, setRisk] = useState<WalletRisk>(demoRisk);
  const [token, setToken] = useState<string | null>(null);
  const [createdCase, setCreatedCase] = useState<CaseResponse | null>(null);
  const [aiSummary, setAiSummary] = useState<AiSummary | null>(null);
  const [loading, setLoading] = useState(false);

  const authed = Boolean(token);

  useEffect(() => {
    async function login() {
      try {
        const response = await apiRequest<{ accessToken: string }>('/api/auth/login', null, {
          method: 'POST',
          body: JSON.stringify({ username: 'analyst@chainguard.demo', password: 'Analyst123!' }),
        });
        setToken(response.accessToken);
      } catch {
        setToken(null);
      }
    }
    login();
  }, []);

  const evaluateWallet = async () => {
    setLoading(true);
    setCreatedCase(null);
    setAiSummary(null);
    try {
      const response = await apiRequest<WalletRisk>(`/api/risk/wallets/${wallet}`, token);
      setRisk(response);
      toast.success(response.cached ? 'Loaded risk result from cache' : 'Wallet risk evaluated');
    } catch {
      setRisk({ ...demoRisk, walletAddress: wallet });
      toast.warning('Backend unavailable — showing built-in demo risk result.');
    } finally {
      setLoading(false);
    }
  };

  const createCase = async () => {
    setLoading(true);
    try {
      const response = await apiRequest<CaseResponse>('/api/cases', token, {
        method: 'POST',
        body: JSON.stringify({
          walletAddress: risk.walletAddress,
          title: `${risk.riskLevel} wallet investigation`,
          riskScore: risk.riskScore,
          riskLevel: risk.riskLevel,
        }),
      });
      setCreatedCase(response);
      setAiSummary(null);
      toast.success(`Case opened: ${response.id.slice(0, 8)}…`);
    } catch {
      // Offline demo fallback: synthesize a local case so the workflow stays explorable.
      if (!authed) {
        const demo: CaseResponse = {
          id: `demo-${Math.random().toString(16).slice(2, 10)}`,
          walletAddress: risk.walletAddress,
          title: `${risk.riskLevel} wallet investigation`,
          status: 'OPEN',
          riskScore: risk.riskScore,
          riskLevel: risk.riskLevel,
          assignee: 'analyst@chainguard.demo',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        };
        setCreatedCase(demo);
        setAiSummary(null);
        toast.warning('Backend unavailable — opened a local demo case.');
      } else {
        toast.error('Failed to open case. Check case-service and PostgreSQL.');
      }
    } finally {
      setLoading(false);
    }
  };

  const advanceStatus = async (next: CaseState) => {
    if (!createdCase) return;
    const isDemoCase = createdCase.id.startsWith('demo-');
    if (!authed || isDemoCase) {
      setCreatedCase({ ...createdCase, status: next, updatedAt: new Date().toISOString() });
      toast.info(`Demo: case moved to ${next}.`);
      return;
    }
    setLoading(true);
    try {
      const response = await apiRequest<CaseResponse>(`/api/cases/${createdCase.id}/status`, token, {
        method: 'PATCH',
        body: JSON.stringify({ status: next }),
      });
      setCreatedCase(response);
      toast.success(`Case moved to ${response.status}`);
    } catch {
      toast.error(`Could not move case to ${next}.`);
    } finally {
      setLoading(false);
    }
  };

  const generateAiSummary = async () => {
    if (!createdCase) {
      toast.info('Open a case before generating an AI summary.');
      return;
    }
    setLoading(true);
    try {
      const response = await apiRequest<AiSummary>(`/api/ai/cases/${createdCase.id}/summary`, token, {
        method: 'POST',
        body: JSON.stringify({
          walletAddress: risk.walletAddress,
          riskScore: risk.riskScore,
          riskLevel: risk.riskLevel,
          triggeredRules: risk.triggeredRules.map((rule) => rule.code),
          analystNotes: ['Demo note: counterparty exposure and rapid movement require review.'],
        }),
      });
      setAiSummary(response);
      toast.success('AI investigation draft generated');
    } catch {
      if (!authed) {
        setAiSummary({
          summary: `Wallet ${risk.walletAddress} scored ${risk.riskScore}/100 (${risk.riskLevel}). The activity pattern combines direct exposure to a flagged counterparty with rapid, high-frequency movement of funds shortly after the address became active — a profile consistent with layering. Recommend prompt analyst review before the wallet transacts further.`,
          riskFactors: risk.triggeredRules.map((r) => r.description),
          recommendedActions: [
            'Confirm the blacklisted counterparty against the latest sanctions / flagged-address feeds.',
            'Trace upstream and downstream hops to map the fund flow.',
            'Escalate to a senior reviewer if exposure is confirmed.',
          ],
          confidence: 'MEDIUM (demo)',
        });
        toast.warning('Backend unavailable — showing a built-in demo AI draft.');
      } else {
        toast.error('Failed to generate AI summary. Check the ai-investigator service.');
      }
    } finally {
      setLoading(false);
    }
  };

  const currentStatus = (createdCase?.status?.toUpperCase() as CaseState) ?? 'OPEN';
  const nextMoves = useMemo(
    () => (createdCase ? ALLOWED_TRANSITIONS[currentStatus] ?? [] : []),
    [createdCase, currentStatus],
  );

  const rk = riskKey(risk.riskLevel);

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <span className="logo">ChainGuard CaseHub</span>
          <span className="tagline">{t('tagline')}</span>
        </div>
        <div className="topbar-right">
          <button className="lang-toggle" onClick={() => setLang(lang === 'en' ? 'zh' : 'en')}>
            {lang === 'en' ? '繁中' : 'EN'}
          </button>
          <span className={`auth-pill ${authed ? 'live' : 'demo'}`}>
            <span className="auth-dot" />
            {authed ? t('auth.live') : t('auth.demo')}
          </span>
        </div>
      </header>

      <Hero />
      <HowItWorks />

      <div className="subbar">
        <span className="chip">Spring Cloud backend</span>
        <span className="dot">·</span>
        <span className="chip">{authed ? 'live API' : 'offline demo'}</span>
        <span className="dot">·</span>
        <a className="link" href={REPO} target="_blank" rel="noreferrer">GitHub ↗</a>
      </div>

      {!authed && (
        <div className="banner">
          Not connected to the API. Start the backend and set <code>VITE_API_BASE</code> (default{' '}
          <code>{API_BASE}</code>). Until then you are exploring built-in demo data.
        </div>
      )}

      <section className="section" id="workbench">
        <div className="section-title">{t('sec.workbench')}</div>
        <div className="card" style={{ marginBottom: 14 }}>
          <div className="card-title">
            {t('wallet.title')}
            <TermHint term="aml" />
          </div>
          <div className="search-row">
            <input
              className="addr"
              value={wallet}
              onChange={(e) => setWallet(e.target.value)}
              placeholder={t('wallet.placeholder')}
              spellCheck={false}
            />
            <button className="primary" disabled={loading || !wallet.trim()} onClick={evaluateWallet}>
              {loading ? t('wallet.evaluating') : t('wallet.evaluate')}
            </button>
          </div>
        </div>

        {/* risk score */}
        <div className="section-title" style={{ marginTop: 4 }}>{t('sec.score')}</div>
        <div className="grid-3" style={{ marginBottom: 14 }}>
          <div className="stat">
            <div className="stat-label">
              {t('risk.score')}
              <TermHint term="riskScore" />
            </div>
            <div className={`risk-score-big risk-${rk}`}>
              {risk.riskScore}
              <span className="unit"> / 100</span>
            </div>
            <div className="gauge">
              <div
                className={`gauge-fill bg-risk-${rk}`}
                style={{ width: `${Math.min(100, Math.max(0, risk.riskScore))}%` }}
              />
            </div>
          </div>

          <div className="stat">
            <div className="stat-label">
              {t('risk.band')}
              <TermHint term="riskBand" />
            </div>
            <div className="band">
              <span className={`band-dot bg-risk-${rk}`} />
              <span className={`risk-${rk}`}>{risk.riskLevel?.toUpperCase()}</span>
            </div>
            <div className="stat-label" style={{ marginTop: 14 }}>
              {risk.cached ? t('risk.cached') : t('risk.fresh')}
            </div>
          </div>

          <div className="stat">
            <div className="stat-label">
              {t('risk.rules')}
              <TermHint term="rulesFired" />
            </div>
            <div className="stat-value">
              {risk.triggeredRules.length}
              <span className="unit">fired</span>
            </div>
            <div className="stat-label" style={{ marginTop: 14, wordBreak: 'break-all' }}>
              {risk.walletAddress}
            </div>
          </div>
        </div>

        {/* triggered rules */}
        <div className="card" style={{ marginBottom: 14 }}>
          <div className="card-title">
            {t('risk.triggered')}
            <TermHint term="blacklist" />
          </div>
          {risk.triggeredRules.length === 0 ? (
            <div className="empty">{t('risk.noRules')}</div>
          ) : (
            risk.triggeredRules.map((rule) => (
              <div className="rule" key={rule.code}>
                <SeverityBadge level={rule.severity} />
                <div className="rule-body">
                  <div className="rule-code">{rule.code}</div>
                  <div className="rule-desc">{rule.description}</div>
                </div>
                <div className="rule-impact">+{rule.scoreImpact}</div>
              </div>
            ))
          )}
        </div>
      </section>

      {/* case lifecycle + AI */}
      <section className="section">
        <div className="section-title">{t('sec.case')}</div>
        <div className="grid-2">
          <div className="card">
            <div className="card-title">
              {t('case.title')}
              <TermHint term="caseStatus" />
            </div>

            {!createdCase ? (
              <>
                <div className="empty" style={{ marginBottom: 14 }}>{t('case.none')}</div>
                <button className="cta" style={{ marginTop: 0 }} disabled={loading} onClick={createCase}>
                  {loading ? t('case.creating') : t('case.create')}
                </button>
              </>
            ) : (
              <>
                <Pipeline status={currentStatus} />

                <div style={{ marginTop: 16 }}>
                  <div className="meta-row">
                    <span className="meta-key">{t('case.id')}</span>
                    <span className="meta-val mono">{createdCase.id}</span>
                  </div>
                  <div className="meta-row">
                    <span className="meta-key">{t('case.wallet')}</span>
                    <span className="meta-val mono">{createdCase.walletAddress}</span>
                  </div>
                  <div className="meta-row">
                    <span className="meta-key">
                      {t('case.assignee')}
                      <TermHint term="assignee" />
                    </span>
                    <span className="meta-val">{createdCase.assignee}</span>
                  </div>
                  <div className="meta-row">
                    <span className="meta-key">{t('risk.band')}</span>
                    <span className="meta-val">
                      <SeverityBadge level={createdCase.riskLevel} />
                    </span>
                  </div>
                  <div className="meta-row">
                    <span className="meta-key">{t('case.status')}</span>
                    <span className="meta-val">
                      <span className="badge accent">{currentStatus}</span>
                    </span>
                  </div>
                </div>

                <div className="section-title" style={{ marginTop: 16 }}>{t('case.advance')}</div>
                <div className="pipeline-actions">
                  {nextMoves.length === 0 ? (
                    <span className="empty">Case is closed — no further transitions.</span>
                  ) : (
                    nextMoves.map((next) => (
                      <button key={next} disabled={loading} onClick={() => advanceStatus(next)}>
                        {t('case.moveTo')} {next}
                      </button>
                    ))
                  )}
                </div>
              </>
            )}
          </div>

          <div className={`card ${aiSummary ? 'ai-glow' : ''}`}>
            <div className="card-title">
              {t('ai.title')}
              <TermHint term="aiSummary" />
            </div>

            <div className="pipeline-actions" style={{ marginTop: 0, marginBottom: 14 }}>
              <button
                className="primary"
                style={{ padding: '7px 14px' }}
                disabled={loading || !createdCase}
                onClick={generateAiSummary}
              >
                {loading ? t('ai.generating') : t('ai.generate')}
              </button>
            </div>

            {aiSummary ? (
              <>
                <p className="ai-summary">{aiSummary.summary}</p>

                <div className="ai-block-title">{t('ai.factors')}</div>
                <ul className="ai-list">
                  {aiSummary.riskFactors.map((f, i) => (
                    <li key={i}>{f}</li>
                  ))}
                </ul>

                <div className="ai-block-title">{t('ai.actions')}</div>
                <ul className="ai-list">
                  {aiSummary.recommendedActions.map((a, i) => (
                    <li key={i}>{a}</li>
                  ))}
                </ul>

                <div style={{ marginTop: 16 }}>
                  <span className="badge accent">
                    {t('ai.confidence')}: {aiSummary.confidence}
                  </span>
                  <TermHint term="confidence" />
                </div>
              </>
            ) : (
              <div className="ai-empty">{t('ai.empty')}</div>
            )}
          </div>
        </div>
      </section>

      <footer className="foot">
        <p>{t('foot.note')}</p>
        <p className="foot-links">
          <a href={REPO} target="_blank" rel="noreferrer">source</a>
          <a href={`${API_BASE}/api/cases/health`} target="_blank" rel="noreferrer">case-service health</a>
        </p>
      </footer>
    </div>
  );
}
