CREATE TABLE donor (
                       id SERIAL PRIMARY KEY,
                       full_name VARCHAR(255),
                       email VARCHAR(255)
);

CREATE TABLE beneficiary (
                             id SERIAL PRIMARY KEY,
                             full_name VARCHAR(255),
                             email VARCHAR(255)
);

CREATE TABLE payment (
                         id VARCHAR(255) PRIMARY KEY,
                         amount NUMERIC,
                         method VARCHAR(255),
                         status VARCHAR(255),
                         date TIMESTAMP
);

CREATE TABLE donation (
                          id SERIAL PRIMARY KEY,
                          donor_id BIGINT,
                          payment_id VARCHAR(255),
                          created_at TIMESTAMP,
                          FOREIGN KEY (donor_id) REFERENCES donor(id),
                          FOREIGN KEY (payment_id) REFERENCES payment(id)
);

CREATE TABLE help (
                      id SERIAL PRIMARY KEY,
                      beneficiary_id BIGINT,
                      payment_id VARCHAR(255),
                      description TEXT,
                      created_at TIMESTAMP,
                      FOREIGN KEY (beneficiary_id) REFERENCES beneficiary(id),
                      FOREIGN KEY (payment_id) REFERENCES payment(id)
);
