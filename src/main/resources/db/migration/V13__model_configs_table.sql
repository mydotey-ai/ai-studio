-- Model configurations table
CREATE TABLE model_configs (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    model_type VARCHAR(50) NOT NULL, -- EMBEDDING, LLM, RERANK, etc.
    provider VARCHAR(50) NOT NULL, -- OPENAI, AZURE, ANTHROPIC, etc.
    model_name VARCHAR(100) NOT NULL,
    api_key VARCHAR(500),
    api_endpoint VARCHAR(500),
    model_params JSONB DEFAULT '{}',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(org_id, name)
);

-- Create indexes
CREATE INDEX idx_model_configs_org_id ON model_configs(org_id);
CREATE INDEX idx_model_configs_model_type ON model_configs(model_type);
CREATE INDEX idx_model_configs_provider ON model_configs(provider);
