package cmc.mody.report.application.alert;

/**
 * 알림을 보낼 때의 대상 상태.
 *
 * @param totalReports   대상의 누적 신고 수
 * @param pendingReports 대상의 처리 대기 신고 수
 * @param authorNickname 대상 작성자 닉네임. 탈퇴했으면 "탈퇴한 회원", 대상이 없으면 null
 */
public record ReportAlertSnapshot(long totalReports, long pendingReports, String authorNickname) {
}
