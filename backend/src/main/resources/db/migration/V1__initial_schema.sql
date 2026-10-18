CREATE TABLE app_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_users_email UNIQUE (email)
);

CREATE TABLE vehicles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    manufacturer VARCHAR(80) NOT NULL,
    model VARCHAR(100) NOT NULL,
    model_year INT NULL,
    current_odometer_km INT NOT NULL,
    manual_monthly_average_km INT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_vehicles_owner FOREIGN KEY (owner_id) REFERENCES app_users (id)
);

CREATE TABLE odometer_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vehicle_id BIGINT NOT NULL,
    recorded_on DATE NOT NULL,
    odometer_km INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_odometer_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id),
    CONSTRAINT uk_odometer_vehicle_date UNIQUE (vehicle_id, recorded_on)
);

CREATE TABLE maintenance_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vehicle_id BIGINT NOT NULL,
    category VARCHAR(40) NOT NULL,
    product_name VARCHAR(160) NULL,
    last_replaced_on DATE NOT NULL,
    last_replaced_odometer_km INT NOT NULL,
    interval_km INT NULL,
    interval_months INT NULL,
    active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_maintenance_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id)
);

CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    maintenance_item_id BIGINT NOT NULL,
    notification_type VARCHAR(30) NOT NULL,
    title VARCHAR(120) NOT NULL,
    message VARCHAR(500) NOT NULL,
    dedupe_key VARCHAR(180) NOT NULL,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notifications_dedupe UNIQUE (dedupe_key),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT fk_notifications_item FOREIGN KEY (maintenance_item_id) REFERENCES maintenance_items (id)
);

CREATE INDEX idx_vehicles_owner ON vehicles (owner_id);
CREATE INDEX idx_odometer_vehicle_date ON odometer_entries (vehicle_id, recorded_on);
CREATE INDEX idx_maintenance_vehicle ON maintenance_items (vehicle_id);
CREATE INDEX idx_notifications_user_read ON notifications (user_id, read_at);
