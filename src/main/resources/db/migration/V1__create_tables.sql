CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    telegram_id VARCHAR(255) UNIQUE,
    chat_id BIGINT,
    phone_number VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    is_admin BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS category_type (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sort_order INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_id BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INTEGER DEFAULT 0,
    category_type_id BIGINT,
    CONSTRAINT fk_category_parent
        FOREIGN KEY (parent_id) REFERENCES category (id),
    CONSTRAINT fk_category_type
        FOREIGN KEY (category_type_id) REFERENCES category_type (id)
);

CREATE TABLE IF NOT EXISTS product (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    amount BIGINT,
    size VARCHAR(255),
    count INTEGER DEFAULT 0,
    available BOOLEAN DEFAULT TRUE,
    photo VARCHAR(255),
    description TEXT,
    category_id BIGINT,
    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id) REFERENCES category (id)
);

CREATE TABLE IF NOT EXISTS addon_type (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_addon (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    addon_product_id BIGINT NOT NULL,
    additional_price BIGINT DEFAULT 0,
    price_for_small_volume BIGINT,
    price_for_large_volume BIGINT,
    small_volume_threshold INTEGER DEFAULT 200,
    volume_dependent BOOLEAN DEFAULT FALSE,
    is_required BOOLEAN DEFAULT FALSE,
    max_quantity INTEGER DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_product_addon_pair UNIQUE (product_id, addon_product_id),
    CONSTRAINT fk_product_addon_product
        FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_product_addon_addon_product
        FOREIGN KEY (addon_product_id) REFERENCES product (id)
);

CREATE TABLE IF NOT EXISTS cart (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_cart_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS cart_item (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    count_product INTEGER DEFAULT 1,
    special_instructions VARCHAR(255),
    added_at TIMESTAMP,
    CONSTRAINT fk_cart_item_cart
        FOREIGN KEY (cart_id) REFERENCES cart (id),
    CONSTRAINT fk_cart_item_product
        FOREIGN KEY (product_id) REFERENCES product (id)
);

CREATE TABLE IF NOT EXISTS cart_item_addon (
    id BIGSERIAL PRIMARY KEY,
    cart_item_id BIGINT NOT NULL,
    addon_product_id BIGINT NOT NULL,
    quantity INTEGER DEFAULT 1,
    price_at_selection BIGINT NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT uk_cart_item_addon_pair UNIQUE (cart_item_id, addon_product_id),
    CONSTRAINT fk_cart_item_addon_cart_item
        FOREIGN KEY (cart_item_id) REFERENCES cart_item (id),
    CONSTRAINT fk_cart_item_addon_product
        FOREIGN KEY (addon_product_id) REFERENCES product (id)
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_amount BIGINT NOT NULL,
    status VARCHAR(50),
    delivery_type VARCHAR(50) NOT NULL,
    phone_number VARCHAR(255),
    address VARCHAR(255),
    comments VARCHAR(255),
    created_at TIMESTAMP,
    order_number VARCHAR(255) UNIQUE,
    updated_at TIMESTAMP,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS order_item (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    count_product INTEGER NOT NULL,
    price_at_time BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_item_product
        FOREIGN KEY (product_id) REFERENCES product (id)
);

CREATE TABLE IF NOT EXISTS order_item_addon (
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    addon_product_id BIGINT,
    quantity INTEGER,
    price_at_order BIGINT NOT NULL,
    addon_product_name VARCHAR(255),
    created_at TIMESTAMP,
    CONSTRAINT fk_order_item_addon_order_item
        FOREIGN KEY (order_item_id) REFERENCES order_item (id),
    CONSTRAINT fk_order_item_addon_product
        FOREIGN KEY (addon_product_id) REFERENCES product (id)
);

CREATE TABLE IF NOT EXISTS order_sessions (
    chat_id BIGINT PRIMARY KEY,
    delivery_type VARCHAR(32),
    address VARCHAR(512),
    comments VARCHAR(512),
    phone_number VARCHAR(32),
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS admin_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_created_at ON users (created_at);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders (created_at);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders (user_id);
CREATE INDEX IF NOT EXISTS idx_product_category_id ON product (category_id);
CREATE INDEX IF NOT EXISTS idx_category_parent_id ON category (parent_id);
CREATE INDEX IF NOT EXISTS idx_category_type_id ON category (category_type_id);
CREATE INDEX IF NOT EXISTS idx_order_item_order_id ON order_item (order_id);
CREATE INDEX IF NOT EXISTS idx_cart_item_cart_id ON cart_item (cart_id);

