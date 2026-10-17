package cmc.mody.report.domain;

public enum RecordReportResolution {
    /** 신고를 받아들여 기록을 숨겼다(삭제 처리). */
    HIDDEN,
    /** 문제가 없다고 보고 기록을 유지했다. */
    DISMISSED
}
