
CREATE TABLE IF NOT EXISTS uploads (
                                       id VARCHAR(255) PRIMARY KEY,
    file_id VARCHAR(255),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,

                             upload_length BIGINT,
                             current_offset BIGINT NOT NULL,

                             filename VARCHAR(1024),
    content_type VARCHAR(255),
    checksum VARCHAR(255),

    bucket VARCHAR(255),
    object_key VARCHAR(1024),
    final_checksum VARCHAR(255),

    next_part_index INT NOT NULL,

    status VARCHAR(50),
    merge_mode VARCHAR(50),

    version BIGINT
    );

CREATE INDEX IF NOT EXISTS idx_uploads_file_id ON uploads(file_id);

CREATE TABLE IF NOT EXISTS upload_part_sizes (
                                                 upload_id VARCHAR(255) NOT NULL,
    part_index INT NOT NULL,
    part_sizes BIGINT,

    CONSTRAINT pk_upload_part_sizes PRIMARY KEY (upload_id, part_index),
    CONSTRAINT fk_upload_part_sizes_upload FOREIGN KEY (upload_id) REFERENCES uploads(id) ON DELETE CASCADE
    );

ALTER TABLE uploads
    ADD CONSTRAINT chk_uploads_status CHECK (status IN ('PENDING','UPLOADING','COMPLETED','FAILED','DELETED','EXPIRED') OR status IS NULL);

ALTER TABLE uploads
    ADD CONSTRAINT chk_uploads_merge_mode CHECK (merge_mode IN ('SERVER_SIDE','CLIENT_SIDE') OR merge_mode IS NULL);
);