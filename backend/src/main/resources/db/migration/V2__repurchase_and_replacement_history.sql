ALTER TABLE maintenance_items ADD COLUMN purchase_url VARCHAR(2048) NULL;
ALTER TABLE maintenance_items ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;

CREATE TABLE maintenance_replacements (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    maintenance_item_id BIGINT NOT NULL,
    product_name VARCHAR(160) NULL,
    purchase_url VARCHAR(2048) NULL,
    replaced_on DATE NOT NULL,
    odometer_km INT NOT NULL,
    interval_km INT NULL,
    interval_months INT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_replacement_item FOREIGN KEY (maintenance_item_id) REFERENCES maintenance_items(id),
    CONSTRAINT uk_replacement_item_date_km UNIQUE (maintenance_item_id, replaced_on, odometer_km)
);

-- Preserve the original replacement record when introducing the history feature.
INSERT INTO maintenance_replacements
    (maintenance_item_id, product_name, replaced_on, odometer_km, interval_km, interval_months, created_at)
SELECT id, product_name, last_replaced_on, last_replaced_odometer_km, interval_km, interval_months, created_at
FROM maintenance_items;

CREATE TABLE repurchase_clicks (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    maintenance_item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    destination_host VARCHAR(255) NOT NULL,
    maintenance_state VARCHAR(30) NOT NULL,
    clicked_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_click_item FOREIGN KEY (maintenance_item_id) REFERENCES maintenance_items(id),
    CONSTRAINT fk_click_user FOREIGN KEY (user_id) REFERENCES app_users(id)
);
CREATE INDEX idx_repurchase_click_time ON repurchase_clicks(clicked_at);

CREATE TABLE usage_visits (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    visited_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_visit_user FOREIGN KEY (user_id) REFERENCES app_users(id)
);
CREATE INDEX idx_usage_visit_user_time ON usage_visits(user_id, visited_at);
