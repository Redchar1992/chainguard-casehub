// Seed wallet transaction documents consumed by the Risk Engine Service.
// Each document is one wallet with an embedded list of transactions. The risk
// engine reads these to evaluate AML rules (frequency, amount, blacklist
// exposure, new-address large withdrawal). Timestamps are relative to the
// demo "now" of 2026-06-08.
db = db.getSiblingDB('chainguard_casehub');

db.wallet_transactions.deleteMany({});

db.wallet_transactions.insertMany([
  {
    // High-risk demo wallet: blacklist counterparty + large in/out shortly
    // after first-seen + tight time window between transfers.
    walletAddress: '0x00new-blacklist-bad0',
    firstSeen: '2026-06-05T09:00:00Z',
    transactions: [
      {
        txHash: '0xabc001',
        direction: 'IN',
        counterparty: '0xblacklist001',
        counterpartyTags: ['blacklist', 'sanctioned'],
        amountUsd: 12500.50,
        timestamp: '2026-06-08T10:00:00Z',
        tags: ['blacklist_exposure', 'new_address']
      },
      {
        txHash: '0xabc002',
        direction: 'OUT',
        counterparty: '0xhop001',
        counterpartyTags: [],
        amountUsd: 12400.00,
        timestamp: '2026-06-08T10:08:00Z',
        tags: ['rapid_movement']
      },
      {
        txHash: '0xabc003',
        direction: 'OUT',
        counterparty: '0xhop002',
        counterpartyTags: [],
        amountUsd: 11800.00,
        timestamp: '2026-06-08T10:14:00Z',
        tags: ['rapid_movement']
      }
    ]
  },
  {
    // High-frequency wallet: many small transfers inside a 30-minute window.
    walletAddress: '0xhotwallet-frequent',
    firstSeen: '2025-01-10T00:00:00Z',
    transactions: Array.from({ length: 24 }, function (_, i) {
      var minute = i;
      var mm = (minute < 10 ? '0' : '') + minute;
      return {
        txHash: '0xfreq' + (i + 1),
        direction: i % 2 === 0 ? 'IN' : 'OUT',
        counterparty: '0xpeer' + (i % 6),
        counterpartyTags: [],
        amountUsd: 400 + i * 5,
        timestamp: '2026-06-08T11:' + mm + ':00Z',
        tags: ['micro_transfer']
      };
    })
  },
  {
    // Clean, low-risk wallet: a couple of modest, well-spaced transfers and
    // no blacklist exposure. Should score LOW.
    walletAddress: '0xcleanwallet-lowrisk',
    firstSeen: '2024-03-01T00:00:00Z',
    transactions: [
      {
        txHash: '0xclean001',
        direction: 'IN',
        counterparty: '0xexchange-hot',
        counterpartyTags: ['exchange'],
        amountUsd: 250.00,
        timestamp: '2026-05-20T08:00:00Z',
        tags: []
      },
      {
        txHash: '0xclean002',
        direction: 'OUT',
        counterparty: '0xmerchant-01',
        counterpartyTags: ['merchant'],
        amountUsd: 120.00,
        timestamp: '2026-06-01T16:30:00Z',
        tags: []
      }
    ]
  }
]);

db.wallet_transactions.createIndex({ walletAddress: 1 }, { unique: true });
