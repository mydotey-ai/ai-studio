-- Add description and createdBy fields to file_storage_config table
ALTER TABLE file_storage_config ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE file_storage_config ADD COLUMN IF NOT EXISTS created_by BIGINT;

-- Add foreign key constraint for created_by
ALTER TABLE file_storage_config ADD CONSTRAINT fk_file_storage_config_created_by
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

-- Create index for created_by
CREATE INDEX IF NOT EXISTS idx_file_storage_config_created_by ON file_storage_config(created_by);
