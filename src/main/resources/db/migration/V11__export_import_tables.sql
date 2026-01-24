-- 导出任务表
CREATE TABLE IF NOT EXISTS export_tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    scope VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    file_id BIGINT,
    file_size BIGINT,
    error_message TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_export_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_export_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_export_file FOREIGN KEY (file_id) REFERENCES file_metadata(id) ON DELETE SET NULL
);

CREATE INDEX idx_export_tasks_user ON export_tasks(user_id);
CREATE INDEX idx_export_tasks_org ON export_tasks(organization_id);
CREATE INDEX idx_export_tasks_status ON export_tasks(status);

-- 导入任务表
CREATE TABLE IF NOT EXISTS import_tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VALIDATING',
    strategy VARCHAR(20) NOT NULL DEFAULT 'SKIP_EXISTING',
    stats JSONB,
    error_message TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_import_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_import_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_import_file FOREIGN KEY (file_id) REFERENCES file_metadata(id) ON DELETE CASCADE
);

CREATE INDEX idx_import_tasks_user ON import_tasks(user_id);
CREATE INDEX idx_import_tasks_org ON import_tasks(organization_id);
CREATE INDEX idx_import_tasks_status ON import_tasks(status);
