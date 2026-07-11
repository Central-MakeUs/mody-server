package cmc.mody.docs;

public final class ApiDocumentDescriptions {
    public static final String AUTHENTICATED_API = """
        Swagger 상단 Authorize에 `Bearer {accessToken}` 형식으로 access token을 한 번 입력해 호출한다.
        개별 API 요청 필드에는 Authorization 헤더를 반복해서 작성하지 않는다.
        """;

    public static final String CURSOR_PAGING = """
        커서 기반 목록은 최초 조회 시 cursor를 생략한다.
        응답의 nextCursor가 null이고 hasNext가 false이면 마지막 페이지다.
        다음 페이지 조회 시 직전 응답의 nextCursor를 cursor로 전달한다.
        """;

    public static final String IMAGE_UPLOAD_FLOW = """
        이미지는 Upload API로 presignedUrl과 imageKey를 먼저 발급받는다.
        클라이언트는 presignedUrl로 스토리지에 직접 업로드하고, 서버 API에는 imageKey만 전달한다.
        서버는 imageKey를 기준으로 접근 가능한 이미지 URL을 응답한다.
        """;

    public static final String RECORD_CREATE_RULES = """
        MEAL 기록은 mealTime, menu가 필요하고 운동 필드는 null로 전달한다.
        EXERCISE 기록은 exerciseDurationHours, exerciseDurationMinutes, exerciseName이 필요하고 식사 필드는 null로 전달한다.
        운동 시간은 시/분을 합산해 1분 이상이어야 하며, minutes는 0~59 범위만 허용한다.
        """;

    public static final String GROUP_ACCESS_RULES = """
        그룹 API는 현재 로그인한 회원이 참여 중인 그룹을 기준으로 동작한다.
        회원은 최대 4개 그룹에 참여할 수 있고, 하나의 그룹은 최대 12명까지 참여할 수 있다.
        그룹을 나가면 해당 그룹 기준으로 더 이상 피드/구성원 정보에 접근할 수 없다.
        """;

    public static final String ONBOARDING_FLOW_RULES = """
        온보딩 API는 소셜 로그인 직후 발급받은 access token으로 호출한다.
        personalInfoCompleted가 true가 되면 개인 정보 입력 단계는 다시 수행할 수 없다.
        mainAccessible은 개인 정보 입력 완료와 참여 그룹 1개 이상을 모두 만족할 때 true가 된다.
        """;

    public static final String ONBOARDING_PROFILE_REQUEST_RULES = """
        개인 정보 입력 요청 규칙:
        - nickname: 필수, 14자 이하. 그룹 내 중복 닉네임은 허용한다.
        - birthDate: 필수, yyyy-MM-dd 형식, 과거 날짜만 허용한다.
        - currentWeightKg, targetWeightKg: 필수, 20.0~300.0kg, 소수점 둘째 자리까지 허용한다.
        - mealSchedules: 필수, 정확히 3개. BREAKFAST, LUNCH, DINNER를 각각 1개씩 보내야 한다.
        - mealSchedules[].skipped=false이면 time은 필수이고 HH:mm 형식으로 보낸다.
        - mealSchedules[].skipped=true이면 time은 null이어야 한다.
        - exerciseSchedules: 필수, 최소 개수 제한 없음. 일정이 없으면 빈 배열을 보낼 수 있고, 각 항목은 dayOfWeek와 time이 모두 필요하다.
        - exerciseSchedules[].dayOfWeek: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY 중 하나.
        - exerciseSchedules[].time: HH:mm 형식.
        """;

    public static final String SCHEDULE_OWNERSHIP_RULES = """
        식사 시간과 운동 일정은 온보딩 개인 정보 입력 또는 마이페이지 시간표 API에서만 관리한다.
        알림 설정 API는 수신 여부만 수정하며 식사 시간/운동 일정 값은 변경하지 않는다.
        """;

    private ApiDocumentDescriptions() {
    }
}
