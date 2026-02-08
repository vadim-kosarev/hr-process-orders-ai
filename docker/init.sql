-- PostgreSQL initialization script for Orders Service
-- This script creates the initial database structure

-- Create application user
CREATE USER orders WITH PASSWORD 'orders';

-- Create orders schema
CREATE SCHEMA IF NOT EXISTS orders AUTHORIZATION orders;

-- Grant privileges
GRANT USAGE ON SCHEMA orders TO orders;
GRANT CREATE ON SCHEMA orders TO orders;

-- Create orders table
CREATE TABLE IF NOT EXISTS orders.orders (
    id SERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255),
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders.orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders.orders(created_at);

-- Grant permissions on table
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA orders TO orders;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA orders TO orders;

-- Insert sample data
INSERT INTO orders.orders (order_number, customer_name, customer_email, total_amount, status)
VALUES
    ('ORD-001', 'John Doe', 'john@example.com', 150.00, 'PENDING'),
    ('ORD-002', 'Jane Smith', 'jane@example.com', 250.00, 'COMPLETED'),
    ('ORD-003', 'Bob Johnson', 'bob@example.com', 100.00, 'PENDING')
ON CONFLICT (order_number) DO NOTHING;

