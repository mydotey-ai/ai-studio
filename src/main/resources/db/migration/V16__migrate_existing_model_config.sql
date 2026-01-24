-- 将配置文件中的默认配置导入到数据库中
-- 注意：这个脚本需要手动执行或者在应用启动时由代码执行
-- 这里只是示例，实际需要根据当前配置文件的值来设置

-- 插入默认的向量模型配置
INSERT INTO model_configs (org_id, type, name, endpoint, api_key, model, dimension, timeout, is_default, status, created_at, updated_at)
VALUES (
    1,
    'embedding',
    '默认向量模型',
    'https://api.openai.com/v1',
    'your-api-key-here',
    'text-embedding-ada-002',
    1536,
    30000,
    true,
    'active',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

-- 插入默认的LLM模型配置
INSERT INTO model_configs (org_id, type, name, endpoint, api_key, model, temperature, max_tokens, timeout, enable_streaming, is_default, status, created_at, updated_at)
VALUES (
    1,
    'llm',
    '默认LLM模型',
    'https://api.openai.com/v1',
    'your-api-key-here',
    'gpt-3.5-turbo',
    0.3,
    1000,
    60000,
    true,
    'active',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

-- 添加注释
COMMENT ON TABLE model_configs IS '迁移配置文件中的模型配置到数据库';
