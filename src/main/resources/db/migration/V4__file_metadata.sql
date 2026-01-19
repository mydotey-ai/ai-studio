-- File Metadata Table
CREATE TABLE IF NOT EXISTS file_metadata (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    file_size BIGINT,
    content_type VARCHAR(100),
    storage_type VARCHAR(20) NOT NULL,
    storage_config_id BIGINT REFERENCES file_storage_config(id) ON DELETE SET NULL,
    bucket_name VARCHAR(255),
    file_key VARCHAR(500),
    uploaded_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    related_entity_type VARCHAR(50),
    related_entity_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_file_metadata_uploaded_by ON file_metadata(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_file_metadata_related_entity ON file_metadata(related_entity_type, related_entity_id);
CREATE INDEX IF NOT EXISTS idx_file_metadata_storage_type ON file_metadata(storage_type);
