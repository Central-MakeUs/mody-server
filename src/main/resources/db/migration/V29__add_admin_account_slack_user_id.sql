-- Slack에서 신고를 처리한 관리자를 알아보기 위한 연결(LLD-0040). NULL 허용이라 이전 버전에 영향이 없다.
ALTER TABLE admin_account ADD COLUMN slack_user_id VARCHAR(32) NULL;
CREATE UNIQUE INDEX uk_admin_account_slack_user_id ON admin_account (slack_user_id);
