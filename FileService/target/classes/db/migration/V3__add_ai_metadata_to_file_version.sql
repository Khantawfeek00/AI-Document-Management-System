ALTER TABLE file_version ADD COLUMN summary TEXT;
ALTER TABLE file_version ADD COLUMN sensitivity VARCHAR(255);

CREATE TABLE file_version_tags (
                                   file_version_id VARCHAR(255) NOT NULL,
                                   tags VARCHAR(255)
);

ALTER TABLE file_version_tags
    ADD CONSTRAINT fk_file_version_tags_file_version
        FOREIGN KEY (file_version_id)
            REFERENCES file_version(id)
            ON DELETE CASCADE;