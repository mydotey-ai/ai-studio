-- Add llm_model_config_id column to agents table
ALTER TABLE agents ADD COLUMN IF NOT EXISTS llm_model_config_id BIGINT;

-- Create index on llm_model_config_id for better query performance
CREATE INDEX IF NOT EXISTS idx_agents_llm_model_config_id ON agents(llm_model_config_id);

-- Add comment to the column
COMMENT ON COLUMN agents.llm_model_config_id IS '关联的LLM模型配置ID';
