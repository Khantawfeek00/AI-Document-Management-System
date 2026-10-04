
CREATE TABLE IF NOT EXISTS file (
                                    id VARCHAR(255) PRIMARY KEY,
    owner_id VARCHAR(255),

    latest_filename VARCHAR(1024),
    latest_content_type VARCHAR(255),
    latest_checksum VARCHAR(255),
    latest_size BIGINT,

    sharing_rules_json TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                             current_version_id VARCHAR(255)
    );

CREATE TABLE IF NOT EXISTS file_version (
                                            id VARCHAR(255) PRIMARY KEY,
    file_id VARCHAR(255) NOT NULL REFERENCES file(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    filename VARCHAR(1024),
    content_type VARCHAR(255),
    size BIGINT,
    checksum VARCHAR(255),
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                                          bucket VARCHAR(255),
    object_key VARCHAR(1024)
    );

CREATE INDEX IF NOT EXISTS idx_file_version_file_id ON file_version(file_id);

CREATE TABLE IF NOT EXISTS file_shares (
                                           id VARCHAR(255) PRIMARY KEY,
    file_id VARCHAR(255) NOT NULL REFERENCES file(id) ON DELETE CASCADE,
    shared_with_user_id VARCHAR(255) NOT NULL,
    permission VARCHAR(10) NOT NULL CHECK (permission IN ('READ', 'WRITE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
                                                          );

CREATE INDEX IF NOT EXISTS idx_file_shares_file_id ON file_shares(file_id);
CREATE INDEX IF NOT EXISTS idx_file_shares_shared_with ON file_shares(shared_with_user_id);

ALTER TABLE file
    ADD CONSTRAINT fk_file_current_version
        FOREIGN KEY (current_version_id)
            REFERENCES file_version(id)
            ON DELETE SET NULL;
