db = db.getSiblingDB('chainguard_casehub');

db.wallet_transactions.insertOne({
  walletAddress: '0x00new-blacklist-bad0',
  transactions: [
    {
      txHash: '0xabc001',
      direction: 'IN',
      counterparty: '0xblacklist001',
      amountUsd: 12500.50,
      timestamp: '2026-06-08T10:00:00Z',
      tags: ['blacklist_exposure', 'new_address']
    },
    {
      txHash: '0xabc002',
      direction: 'OUT',
      counterparty: '0xhop001',
      amountUsd: 12400.00,
      timestamp: '2026-06-08T10:08:00Z',
      tags: ['rapid_movement']
    }
  ]
});
