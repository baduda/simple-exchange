-- Creating the tables for a basic cryptocurrency exchange system in an H2 Database

-- Drop tables if they exist to start fresh
DROP TABLE IF EXISTS trades;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS walletBalances;
DROP TABLE IF EXISTS wallets;

-- Create Wallets table
CREATE TABLE wallets
(
    wallet_id  INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT       NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

-- Create WalletBalances table
CREATE TABLE wallet_balances
(
    wallet_balance_id INT AUTO_INCREMENT PRIMARY KEY,
    wallet_id         INT            NOT NULL,
    currency          VARCHAR(32)    NOT NULL,
    balance           DECIMAL(15, 8) NOT NULL CHECK (balance >= 0.0),
    FOREIGN KEY (wallet_id) REFERENCES wallets (wallet_id)
);

-- Create Transactions table
CREATE TABLE transactions
(
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    wallet_id      INT            NOT NULL,
    type           VARCHAR(32)    NOT NULL,
    status         VARCHAR(32)    NOT NULL,
    amount         DECIMAL(15, 8) NOT NULL,
    currency       VARCHAR(32)    NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    FOREIGN KEY (wallet_id) REFERENCES wallets (wallet_id)
);

-- Create Orders table
CREATE TABLE orders
(
    order_id                    INT AUTO_INCREMENT PRIMARY KEY,
    wallet_id                   INT            NOT NULL,
    type                        VARCHAR(32)    NOT NULL,
    base_currency               VARCHAR(32)    NOT NULL,
    quote_currency              VARCHAR(32)    NOT NULL,
    original_amount             DECIMAL(15, 8) NOT NULL,
    amount                      DECIMAL(15, 8) NOT NULL,
    status                      VARCHAR(32)    NOT NULL,
    price                       DECIMAL(15, 8) NOT NULL,
    created_at                  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    spot_deposit_transaction_id INT            NOT NULL,
    CHECK (base_currency <> quote_currency)
);

-- Create Trades table
CREATE TABLE trades
(
    trade_id            INT AUTO_INCREMENT PRIMARY KEY,
    buy_order_id        INT            NOT NULL,
    sell_order_id       INT            NOT NULL,
    buy_transaction_id  INT            NOT NULL,
    sell_transaction_id INT            NOT NULL,
    amount              DECIMAL(15, 8) NOT NULL,
    price               DECIMAL(15, 8) NOT NULL,
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    FOREIGN KEY (buy_order_id) REFERENCES orders (order_id),
    FOREIGN KEY (sell_order_id) REFERENCES orders (order_id)
);
