-- 向 knowledge_bases 表添加模型配置字段
ALTER TABLE knowledge_bases ADD COLUMN IF NOT EXISTS embedding_model_id BIGINT REFERENCES model_configs(id) ON DELETE SET NULL;
ALTER TABLE knowledge_bases ADD COLUMN IF NOT EXISTS llm_model_id BIGINT REFERENCES model_configs(id) ON DELETE SET NULL;

-- 添加注释
COMMENT ON COLUMN knowledge_bases.embedding_model_id IS '关联的向量模型配置ID';
COMMENT ON COLUMN knowledge_bases.llm_model_id IS '关联的LLM模型配置ID';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_knowledge_bases_embedding_model_id ON knowledge_bases(embedding_model_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_bases_llm_model_id ON knowledge_bases(llm_model_id);
