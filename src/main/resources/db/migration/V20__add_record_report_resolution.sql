ALTER TABLE record_report ADD COLUMN resolution VARCHAR(20) NULL;
ALTER TABLE record_report ADD COLUMN resolved_at DATETIME(6) NULL;
ALTER TABLE record_report ADD COLUMN resolved_by VARCHAR(320) NULL;
CREATE INDEX idx_record_report_resolution ON record_report (resolution, record_id);
