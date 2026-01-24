-- 创建模型配置表
CREATE TABLE IF NOT EXISTS model_configs (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    endpoint VARCHAR(500),
    api_key VARCHAR(500),
    model VARCHAR(100),
    dimension INTEGER,
    temperature DECIMAL(3,2),
    max_tokens INTEGER,
    timeout INTEGER DEFAULT 30000,
    enable_streaming BOOLEAN DEFAULT true,
    is_default BOOLEAN DEFAULT false,
    status VARCHAR(20) DEFAULT 'active',
    description TEXT,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_type CHECK (type IN ('embedding', 'llm')),
    CONSTRAINT check_status CHECK (status IN ('active', 'inactive'))
);

-- 创建索引
CREATE INDEX idx_model_configs_type ON model_configs(type);
CREATE INDEX idx_model_configs_org_id ON model_configs(org_id);
CREATE INDEX idx_model_configs_is_default ON model_configs(is_default) WHERE is_default = true;

-- 添加注释
COMMENT ON TABLE model_configs IS '模型配置表';
COMMENT ON COLUMN model_configs.type IS '配置类型: embedding(向量模型) 或 llm(大语言模型)';
COMMENT ON COLUMN model_configs.is_default IS '是否为默认配置，每个类型只有一个默认配置';
