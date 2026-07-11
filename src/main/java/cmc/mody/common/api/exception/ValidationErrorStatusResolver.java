package cmc.mody.common.api.exception;

import cmc.mody.common.api.status.ErrorStatus;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

final class ValidationErrorStatusResolver {
    private static final List<ErrorStatus> STATUS_PRIORITY = List.of(
        ErrorStatus.MEMBER_NICKNAME_INVALID,
        ErrorStatus.MEMBER_BIRTH_DATE_INVALID,
        ErrorStatus.MEMBER_WEIGHT_INVALID,
        ErrorStatus.MEMBER_MEAL_SCHEDULE_INVALID,
        ErrorStatus.MEMBER_MEAL_TIME_INVALID,
        ErrorStatus.MEMBER_EXERCISE_SCHEDULE_INVALID,
        ErrorStatus.RECORD_GROUP_ID_INVALID,
        ErrorStatus.RECORD_TYPE_INVALID,
        ErrorStatus.RECORD_IMAGE_INVALID,
        ErrorStatus.RECORD_MEAL_PAYLOAD_INVALID,
        ErrorStatus.RECORD_EXERCISE_PAYLOAD_INVALID,
        ErrorStatus.RECORD_EXERCISE_DURATION_INVALID,
        ErrorStatus.RECORD_COMMENT_INVALID
    );

    private static final Map<String, ErrorStatus> STATUS_BY_MESSAGE = Map.ofEntries(
        Map.entry("닉네임은 필수입니다.", ErrorStatus.MEMBER_NICKNAME_INVALID),
        Map.entry("닉네임은 14자 이하로 입력해주세요.", ErrorStatus.MEMBER_NICKNAME_INVALID),
        Map.entry("생년월일은 필수입니다.", ErrorStatus.MEMBER_BIRTH_DATE_INVALID),
        Map.entry("생년월일은 과거 날짜여야 합니다.", ErrorStatus.MEMBER_BIRTH_DATE_INVALID),
        Map.entry("현재 체중은 필수입니다.", ErrorStatus.MEMBER_WEIGHT_INVALID),
        Map.entry("현재 체중은 20kg 이상이어야 합니다.", ErrorStatus.MEMBER_WEIGHT_INVALID),
        Map.entry("현재 체중은 300kg 이하여야 합니다.", ErrorStatus.MEMBER_WEIGHT_INVALID),
        Map.entry("현재 체중은 소수점 둘째 자리까지 입력해주세요.", ErrorStatus.MEMBER_WEIGHT_INVALID),
        Map.entry("목표 체중은 필수입니다.", ErrorStatus.MEMBER_WEIGHT_INVALID),
        Map.entry("목표 체중은 20kg 이상이어야 합니다.", ErrorStatus.MEMBER_WEIGHT_INVALID),
        Map.entry("목표 체중은 300kg 이하여야 합니다.", ErrorStatus.MEMBER_WEIGHT_INVALID),
        Map.entry("목표 체중은 소수점 둘째 자리까지 입력해주세요.", ErrorStatus.MEMBER_WEIGHT_INVALID),
        Map.entry("식사 설정은 필수입니다.", ErrorStatus.MEMBER_MEAL_SCHEDULE_INVALID),
        Map.entry("식사 설정은 아침, 점심, 저녁 3개를 입력해주세요.", ErrorStatus.MEMBER_MEAL_SCHEDULE_INVALID),
        Map.entry("식사 타입은 필수입니다.", ErrorStatus.MEMBER_MEAL_SCHEDULE_INVALID),
        Map.entry("식사 설정은 아침, 점심, 저녁을 각각 1개씩 입력해주세요.", ErrorStatus.MEMBER_MEAL_SCHEDULE_INVALID),
        Map.entry("먹지 않음이면 시간은 비워두고, 먹는 식사는 시간을 입력해주세요.", ErrorStatus.MEMBER_MEAL_TIME_INVALID),
        Map.entry("운동 일정은 필수입니다.", ErrorStatus.MEMBER_EXERCISE_SCHEDULE_INVALID),
        Map.entry("운동 일정은 주 3회 이상 입력해주세요.", ErrorStatus.MEMBER_EXERCISE_SCHEDULE_INVALID),
        Map.entry("운동 요일은 필수입니다.", ErrorStatus.MEMBER_EXERCISE_SCHEDULE_INVALID),
        Map.entry("운동 시간은 필수입니다.", ErrorStatus.MEMBER_EXERCISE_SCHEDULE_INVALID),
        Map.entry("그룹 id는 양수여야 합니다.", ErrorStatus.RECORD_GROUP_ID_INVALID),
        Map.entry("기록 타입은 필수입니다.", ErrorStatus.RECORD_TYPE_INVALID),
        Map.entry("이미지 키는 필수입니다.", ErrorStatus.RECORD_IMAGE_INVALID),
        Map.entry("이미지 키는 500자 이하로 입력해주세요.", ErrorStatus.RECORD_IMAGE_INVALID),
        Map.entry("기록 이미지는 records 도메인으로 발급된 이미지 키여야 합니다.", ErrorStatus.RECORD_IMAGE_INVALID),
        Map.entry("식사 기록은 식사 시간과 메뉴를 입력하고 운동 정보는 비워주세요.", ErrorStatus.RECORD_MEAL_PAYLOAD_INVALID),
        Map.entry("운동 기록은 운동 시간과 운동명을 입력하고 식사 정보는 비워주세요.", ErrorStatus.RECORD_EXERCISE_PAYLOAD_INVALID),
        Map.entry("메뉴는 100자 이하로 입력해주세요.", ErrorStatus.RECORD_MEAL_PAYLOAD_INVALID),
        Map.entry("운동 시간은 0시간 이상이어야 합니다.", ErrorStatus.RECORD_EXERCISE_DURATION_INVALID),
        Map.entry("운동 시간은 24시간 이하로 입력해주세요.", ErrorStatus.RECORD_EXERCISE_DURATION_INVALID),
        Map.entry("운동 분은 0분 이상이어야 합니다.", ErrorStatus.RECORD_EXERCISE_DURATION_INVALID),
        Map.entry("운동 분은 59분 이하로 입력해주세요.", ErrorStatus.RECORD_EXERCISE_DURATION_INVALID),
        Map.entry("운동명은 30자 이하로 입력해주세요.", ErrorStatus.RECORD_EXERCISE_PAYLOAD_INVALID),
        Map.entry("댓글 내용은 필수입니다.", ErrorStatus.RECORD_COMMENT_INVALID),
        Map.entry("댓글은 100자 이하로 입력해주세요.", ErrorStatus.RECORD_COMMENT_INVALID)
    );

    private ValidationErrorStatusResolver() {
    }

    static ErrorStatus resolve(MethodArgumentNotValidException exception, ErrorStatus fallback) {
        Set<ErrorStatus> statuses = exception.getBindingResult()
            .getAllErrors()
            .stream()
            .map(ObjectError::getDefaultMessage)
            .map(STATUS_BY_MESSAGE::get)
            .filter(status -> status != null)
            .collect(Collectors.toSet());

        return STATUS_PRIORITY.stream()
            .filter(statuses::contains)
            .findFirst()
            .orElse(fallback);
    }
}
