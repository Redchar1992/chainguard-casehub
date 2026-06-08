import React, { useState } from 'react';
import ReactDOM from 'react-dom/client';
import { Button, Card, Col, Input, Layout, List, Row, Space, Statistic, Tag, Typography } from 'antd';
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

const demoRisk: WalletRisk = {
  walletAddress: '0x00new-blacklist-bad',
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

function App() {
  const [wallet, setWallet] = useState('0x00new-blacklist-bad');
  const [risk, setRisk] = useState<WalletRisk>(demoRisk);

  const evaluateWallet = async () => {
    try {
      const response = await fetch(`/api/risk/wallets/${wallet}`);
      if (!response.ok) throw new Error('API unavailable');
      setRisk(await response.json());
    } catch {
      setRisk({ ...demoRisk, walletAddress: wallet });
    }
  };

  return (
    <Layout className="layout">
      <Layout.Header className="header">
        <Typography.Title level={3} className="title">ChainGuard Analyst Console</Typography.Title>
      </Layout.Header>
      <Layout.Content className="content">
        <Space direction="vertical" size="large" className="full-width">
          <Card>
            <Typography.Title level={4}>Wallet Investigation</Typography.Title>
            <Space.Compact className="search-bar">
              <Input value={wallet} onChange={(event) => setWallet(event.target.value)} placeholder="Enter wallet address" />
              <Button type="primary" onClick={evaluateWallet}>Evaluate Risk</Button>
            </Space.Compact>
          </Card>

          <Row gutter={16}>
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
                <Statistic title="Triggered Rules" value={risk.triggeredRules.length} />
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

          <Card title="AI Investigation Draft">
            <Typography.Paragraph>
              Wallet {risk.walletAddress} is classified as {risk.riskLevel} with risk score {risk.riskScore}. The analyst should review triggered AML rules, transaction timeline, counterparties, and source-of-funds evidence before closing the case.
            </Typography.Paragraph>
            <Button>Create Case</Button>
          </Card>
        </Space>
      </Layout.Content>
    </Layout>
  );
}

ReactDOM.createRoot(document.getElementById('root')!).render(<App />);
