-- Optional: Indexes for better performance
CREATE INDEX idx_wallets_user_id ON wallets (user_id);
CREATE INDEX idx_orders_user_id_status ON orders (user_id, status);
CREATE INDEX idx_trades_timestamp ON trades (created_at);
CREATE UNIQUE INDEX idx_balances_wallet_id_currency ON wallet_balances (wallet_id, currency);


--
-- WITH OrderedOrders AS (
--     SELECT *,
--            SUM(amount) OVER (ORDER BY price ASC, created_at ASC) AS cumulative_amount,
--            LEAD(order_id) OVER (ORDER BY created_at ASC) AS next_order_id
--     FROM orders
--     WHERE from_currency_id = @FromCurrencyId
--       AND to_currency_id = @ToCurrencyId
--       AND type = 'SELL'
--       AND status = 'OPEN'
-- ),
--      FilteredOrders AS (
--          SELECT *
--          FROM OrderedOrders
--          WHERE cumulative_amount <= @Threshold
--      ),
--      NextOrder AS (
--          SELECT *
--          FROM OrderedOrders
--          WHERE order_id = (
--              SELECT next_order_id
--              FROM FilteredOrders
--              ORDER BY created_at DESC
--              LIMIT 1
--              )
--      )
-- SELECT * FROM FilteredOrders
-- UNION ALL
-- SELECT * FROM NextOrder;
