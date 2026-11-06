-- Samples are collected per test, not per order: a CBC (EDTA tube) and a urinalysis (urine cup)
-- are separate draws, often at different times. One row per collected test.
CREATE TABLE lab_sample_collections (
    id            BIGSERIAL    PRIMARY KEY,
    order_id      BIGINT       NOT NULL REFERENCES lab_orders (id) ON DELETE CASCADE,
    test_code     VARCHAR(20)  NOT NULL,
    collected_by  VARCHAR(50)  NOT NULL,
    collected_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_sample_per_test UNIQUE (order_id, test_code),
    CONSTRAINT fk_sample_ordered_test FOREIGN KEY (order_id, test_code)
        REFERENCES lab_order_tests (order_id, test_code) ON DELETE CASCADE
);

-- Orders that were already past sample collection before this migration count as fully collected.
INSERT INTO lab_sample_collections (order_id, test_code, collected_by, collected_at)
SELECT t.order_id, t.test_code, 'system-migration', COALESCE(o.updated_at, o.ordered_at)
FROM lab_order_tests t
JOIN lab_orders o ON o.id = t.order_id
WHERE o.order_status IN ('SAMPLE_COLLECTED', 'COMPLETED');
