import React, { useEffect, useMemo, useState } from 'react';
import ReactDOM from 'react-dom/client';
import { Alert, Button, Card, Col, Input, Layout, List, message, Row, Space, Statistic, Tag, Typography } from 'antd';
import 'antd/dist/reset.css';
import './styles.css';

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

function riskColor(level: string) {
  switch (level) {
    case 'CRITICAL':
      return 'red';
    case 'HIGH':
      return 'orange';
    case 'MEDIUM':
      return 'gold';
    default:
      return 'green';
  }
}

async function apiRequest<T>(path: string, token: string | null, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set('Content-Type', 'application/json');
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    throw new Error(`API ${path} failed with status ${response.status}`);
  }

  return response.json();
}

function App() {
  const [wallet, setWallet] = useState('0x00new-blacklist-bad0');
  const [risk, setRisk] = useState<WalletRisk>(demoRisk);
  const [token, setToken] = useState<string | null>(null);
  const [createdCase, setCreatedCase] = useState<CaseResponse | null>(null);
  const [aiSummary, setAiSummary] = useState<AiSummary | null>(null);
  const [loading, setLoading] = useState(false);

  const authStatus = useMemo(() => token ? 'Demo analyst authenticated' : 'Using offline demo mode', [token]);

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
      message.success(response.cached ? 'Loaded risk result from Redis cache' : 'Wallet risk evaluated');
    } catch {
      setRisk({ ...demoRisk, walletAddress: wallet });
      message.warning('Backend unavailable. Showing built-in demo risk result.');
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
      message.success(`Case created: ${response.id}`);
    } catch {
      message.error('Failed to create case. Make sure backend services and PostgreSQL are running.');
    } finally {
      setLoading(false);
    }
  };

  const generateAiSummary = async () => {
    if (!createdCase) {
      message.info('Create a case before generating AI summary.');
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
      message.success('AI investigation draft generated');
    } catch {
      message.error('Failed to generate AI summary. Make sure AI Investigator service is running.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout className="layout">
      <Layout.Header className="header">
        <Typography.Title level={3} className="title">ChainGuard Analyst Console</Typography.Title>
      </Layout.Header>
      <Layout.Content className="content">
        <Space direction="vertical" size="large" className="full-width">
          <Alert message={authStatus} description={`API Base: ${API_BASE}`} type={token ? 'success' : 'warning'} showIcon />

          <Card>
            <Typography.Title level={4}>Wallet Investigation</Typography.Title>
            <Space.Compact className="search-bar">
              <Input value={wallet} onChange={(event) => setWallet(event.target.value)} placeholder="Enter wallet address" />
              <Button type="primary" loading={loading} onClick={evaluateWallet}>Evaluate Risk</Button>
            </Space.Compact>
          </Card>

          <Row gutter={[16, 16]}>
            <Col xs={24} md={8}>
              <Card>
                <Statistic title="Risk Score" value={risk.riskScore} suffix="/ 100" />
              </Card>
            </Col>
            <Col xs={24} md={8}>
              <Card>
                <Typography.Text type="secondary">Risk Level</Typography.Text>
                <div className="risk-level"><Tag color={riskColor(risk.riskLevel)}>{risk.riskLevel}</Tag></div>
              </Card>
            </Col>
            <Col xs={24} md={8}>
              <Card>
                <Statistic title={risk.cached ? 'Triggered Rules (cached)' : 'Triggered Rules'} value={risk.triggeredRules.length} />
              </Card>
            </Col>
          </Row>

          <Card title="Triggered AML Rules">
            <List
              dataSource={risk.triggeredRules}
              renderItem={(rule) => (
                <List.Item>
                  <List.Item.Meta
                    title={<Space><Tag color={riskColor(rule.severity)}>{rule.severity}</Tag>{rule.code}</Space>}
                    description={rule.description}
                  />
                  <Tag>+{rule.scoreImpact}</Tag>
                </List.Item>
              )}
            />
          </Card>

          <Card title="Case Workflow">
            <Space direction="vertical" className="full-width">
              <Space>
                <Button type="primary" loading={loading} onClick={createCase}>Create Case</Button>
                <Button loading={loading} onClick={generateAiSummary} disabled={!createdCase}>Generate AI Summary</Button>
              </Space>
              {createdCase && (
                <Alert
                  type="success"
                  message={`Case ${createdCase.id}`}
                  description={`Status: ${createdCase.status}, Assignee: ${createdCase.assignee}, Risk: ${createdCase.riskLevel}`}
                />
              )}
            </Space>
          </Card>

          <Card title="AI Investigation Draft">
            {aiSummary ? (
              <Space direction="vertical" className="full-width">
                <Typography.Paragraph>{aiSummary.summary}</Typography.Paragraph>
                <Typography.Text strong>Risk Factors</Typography.Text>
                <List size="small" dataSource={aiSummary.riskFactors} renderItem={(item) => <List.Item>{item}</List.Item>} />
                <Typography.Text strong>Recommended Actions</Typography.Text>
                <List size="small" dataSource={aiSummary.recommendedActions} renderItem={(item) => <List.Item>{item}</List.Item>} />
                <Tag color="blue">Confidence: {aiSummary.confidence}</Tag>
              </Space>
            ) : (
              <Typography.Paragraph>
                Create a case and click Generate AI Summary to produce an investigation draft.
              </Typography.Paragraph>
            )}
          </Card>
        </Space>
      </Layout.Content>
    </Layout>
  );
}

ReactDOM.createRoot(document.getElementById('root')!).render(<App />);
