CREATE TABLE lab_order_tests (
    order_id  BIGINT      NOT NULL REFERENCES lab_orders(id) ON DELETE CASCADE,
    test_code VARCHAR(20) NOT NULL,
    PRIMARY KEY (order_id, test_code)
);