-- QSO Log Database Schema V1
-- Ham Radio contact logging application

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'OPERATOR',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_role CHECK (role IN ('ADMIN', 'OPERATOR'))
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);

-- QSO (contact) table
CREATE TABLE qso (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,

    -- Required ADIF/LoTW fields
    their_callsign VARCHAR(50) NOT NULL,
    qso_date DATE NOT NULL,
    time_on TIME NOT NULL,
    band VARCHAR(20) NOT NULL,
    frequency_khz NUMERIC(10, 3),
    mode VARCHAR(50) NOT NULL,

    -- Mode/submode extensions for ADIF export
    submode VARCHAR(50),
    custom_mode VARCHAR(100),

    -- Optional QSO details
    rst_sent VARCHAR(10),
    rst_recv VARCHAR(10),
    qth VARCHAR(255),
    grid_square VARCHAR(20),
    notes TEXT,

    -- QSL tracking
    qsl_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
    lotw_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    eqsl_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT fk_qso_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_qsl_status CHECK (qsl_status IN ('NONE', 'SENT', 'CONFIRMED')),
    CONSTRAINT chk_lotw_status CHECK (lotw_status IN ('UNKNOWN', 'SENT', 'CONFIRMED')),
    CONSTRAINT chk_eqsl_status CHECK (eqsl_status IN ('UNKNOWN', 'SENT', 'CONFIRMED'))
);

-- QSO indexes for performance (user-scoped queries)
CREATE INDEX idx_qso_user_date ON qso(user_id, qso_date DESC);
CREATE INDEX idx_qso_user_callsign ON qso(user_id, their_callsign);
CREATE INDEX idx_qso_user_band ON qso(user_id, band);

-- AI report history table
CREATE TABLE ai_report_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    from_date DATE,
    to_date DATE,
    language VARCHAR(2) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_ai_report_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_language CHECK (language IN ('PL', 'EN'))
);

CREATE INDEX idx_ai_report_user ON ai_report_history(user_id, created_at DESC);

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at trigger to users table
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Apply updated_at trigger to qso table
CREATE TRIGGER trg_qso_updated_at
    BEFORE UPDATE ON qso
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
