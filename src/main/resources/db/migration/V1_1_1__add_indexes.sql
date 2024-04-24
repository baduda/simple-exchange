-- Indexes for better performance
CREATE INDEX idx_wallets_user_id ON wallets (user_id);
CREATE UNIQUE INDEX idx_balances_wallet_id_currency ON wallet_balances (wallet_id, currency);
CREATE INDEX idx_orders_matcher ON orders (status, type, price, created_at);
