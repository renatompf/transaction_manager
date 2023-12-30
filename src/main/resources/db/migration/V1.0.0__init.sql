BEGIN;

CREATE TABLE account (
     id SERIAL PRIMARY KEY,
     first_name VARCHAR(255) NOT NULL,
     last_name VARCHAR(255) NOT NULL,
     email VARCHAR(255) UNIQUE NOT NULL,
     date_of_birth DATE NOT NULL,
     deleted BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS bank_account(
    id SERIAL PRIMARY KEY,
    owner_id INT,
    currency VARCHAR(3),
    balance DECIMAL,
    deleted BOOLEAN NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES account(id)
);

CREATE TABLE IF NOT EXISTS transaction_logs (
    id SERIAL PRIMARY KEY,
    from_account_id INT,
    to_account_id INT,
    from_currency VARCHAR(3),
    to_currency VARCHAR(3),
    original_amount DECIMAL,
    exchange_rate DECIMAL,
    timestamp timestamptz,
    FOREIGN KEY (from_account_id) REFERENCES bank_account(id) ON DELETE NO ACTION ,
    FOREIGN KEY (to_account_id) REFERENCES bank_account(id) ON DELETE NO ACTION
);

COMMIT;