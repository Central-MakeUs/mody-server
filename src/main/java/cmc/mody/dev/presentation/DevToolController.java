package cmc.mody.dev.presentation;

import cmc.mody.common.api.ApiResponse;
import cmc.mody.dev.application.DevToolService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile({"local", "dev"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dev")
public class DevToolController {
    private final DevToolService devToolService;

    @PostMapping("/members/mock")
    public ApiResponse<MockMemberResponse> createMockMember(@Valid @RequestBody CreateMockMemberRequest request) {
        DevToolService.MockMemberResult result = devToolService.createMockMember(request.toCommand());
        return ApiResponse.ok(MockMemberResponse.from(result));
    }

    @GetMapping("/members")
    public ApiResponse<MemberListResponse> getMembers() {
        DevToolService.MemberListResult result = devToolService.getMembers();
        return ApiResponse.ok(MemberListResponse.from(result));
    }

    @PostMapping("/auth/tokens")
    public ApiResponse<DevTokenResponse> issueToken(@Valid @RequestBody IssueTokenRequest request) {
        DevToolService.DevTokenResult result = devToolService.issueToken(request.toCommand());
        return ApiResponse.ok(DevTokenResponse.from(result));
    }

    @PostMapping("/notifications/test-push")
    public ApiResponse<TestPushResponse> sendTestPush(@Valid @RequestBody TestPushRequest request) {
        DevToolService.TestPushResult result = devToolService.sendTestPush(request.toCommand());
        return ApiResponse.ok(TestPushResponse.from(result));
    }

    @PostMapping("/notifications/inbox-test")
    public ApiResponse<InboxNotificationResponse> createInboxNotification(
        @Valid @RequestBody InboxNotificationRequest request
    ) {
        DevToolService.InboxNotificationResult result = devToolService.createInboxNotification(
            request.memberId(),
            request.toCommand()
        );
        return ApiResponse.ok(InboxNotificationResponse.from(result));
    }

    @GetMapping("/members/{memberId}/state")
    public ApiResponse<DevMeResponse> getMemberState(@PathVariable Long memberId) {
        DevToolService.DevMeResult result = devToolService.getMe(memberId);
        return ApiResponse.ok(DevMeResponse.from(result));
    }

    @GetMapping("/groups")
    public ApiResponse<DevGroupListResponse> getGroups() {
        DevToolService.DevGroupListResult result = devToolService.getGroups();
        return ApiResponse.ok(DevGroupListResponse.from(result));
    }

    @GetMapping("/groups/{groupId}")
    public ApiResponse<DevGroupDetailResponse> getGroup(@PathVariable Long groupId) {
        DevToolService.DevGroupDetailResult result = devToolService.getGroup(groupId);
        return ApiResponse.ok(DevGroupDetailResponse.from(result));
    }

    public record CreateMockMemberRequest(
        @Size(max = 14, message = "닉네임은 14자 이하로 입력해주세요.")
        String nickname,
        LocalDate birthDate,
        @DecimalMin(value = "0.1", message = "목표 체중은 0.1kg 이상이어야 합니다.")
        BigDecimal targetWeightKg,
        Boolean personalInfoCompleted,
        Boolean groupOnboardingCompleted
    ) {
        DevToolService.CreateMockMemberCommand toCommand() {
            return new DevToolService.CreateMockMemberCommand(
                nickname,
                birthDate,
                targetWeightKg,
                personalInfoCompleted == null || personalInfoCompleted,
                groupOnboardingCompleted != null && groupOnboardingCompleted
            );
        }
    }

    public record MockMemberResponse(
        Long memberId,
        String nickname,
        LocalDate birthDate,
        BigDecimal targetWeightKg,
        boolean personalInfoCompleted,
        boolean groupOnboardingCompleted,
        long joinedGroupCount
    ) {
        static MockMemberResponse from(DevToolService.MockMemberResult result) {
            return new MockMemberResponse(
                result.memberId(),
                result.nickname(),
                result.birthDate(),
                result.targetWeightKg(),
                result.personalInfoCompleted(),
                result.groupOnboardingCompleted(),
                result.joinedGroupCount()
            );
        }
    }

    public record MemberListResponse(List<MockMemberResponse> members) {
        static MemberListResponse from(DevToolService.MemberListResult result) {
            return new MemberListResponse(result.members().stream()
                .map(MockMemberResponse::from)
                .toList());
        }
    }

    public record IssueTokenRequest(
        @NotNull(message = "회원 id는 필수입니다.")
        Long memberId
    ) {
        DevToolService.IssueTokenCommand toCommand() {
            return new DevToolService.IssueTokenCommand(memberId);
        }
    }

    public record DevTokenResponse(
        Long memberId,
        String accessToken,
        String refreshToken,
        boolean personalInfoCompleted,
        boolean groupOnboardingCompleted,
        boolean mainAccessible,
        int joinedGroupCount
    ) {
        static DevTokenResponse from(DevToolService.DevTokenResult result) {
            return new DevTokenResponse(
                result.memberId(),
                result.accessToken(),
                result.refreshToken(),
                result.personalInfoCompleted(),
                result.groupOnboardingCompleted(),
                result.mainAccessible(),
                result.joinedGroupCount()
            );
        }
    }

    public record TestPushRequest(
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        @Size(max = 500, message = "FCM 토큰은 500자 이하로 입력해주세요.")
        String fcmToken,
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
        String title,
        @NotBlank(message = "본문은 필수입니다.")
        @Size(max = 500, message = "본문은 500자 이하로 입력해주세요.")
        String body
    ) {
        DevToolService.TestPushCommand toCommand() {
            return new DevToolService.TestPushCommand(fcmToken, title, body);
        }
    }

    public record TestPushResponse(boolean fcmEnabled, boolean invalidToken) {
        static TestPushResponse from(DevToolService.TestPushResult result) {
            return new TestPushResponse(result.fcmEnabled(), result.invalidToken());
        }
    }

    public record InboxNotificationRequest(
        @NotNull(message = "회원 id는 필수입니다.")
        Long memberId,
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
        String title,
        @NotBlank(message = "본문은 필수입니다.")
        @Size(max = 500, message = "본문은 500자 이하로 입력해주세요.")
        String body
    ) {
        DevToolService.InboxNotificationCommand toCommand() {
            return new DevToolService.InboxNotificationCommand(title, body);
        }
    }

    public record InboxNotificationResponse(Long notificationId) {
        static InboxNotificationResponse from(DevToolService.InboxNotificationResult result) {
            return new InboxNotificationResponse(result.notificationId());
        }
    }

    public record DevMeResponse(
        Long memberId,
        String nickname,
        boolean personalInfoCompleted,
        boolean groupOnboardingCompleted,
        boolean mainAccessible,
        int joinedGroupCount,
        List<DevGroupResponse> groups
    ) {
        static DevMeResponse from(DevToolService.DevMeResult result) {
            return new DevMeResponse(
                result.memberId(),
                result.nickname(),
                result.personalInfoCompleted(),
                result.groupOnboardingCompleted(),
                result.mainAccessible(),
                result.joinedGroupCount(),
                result.groups().stream()
                    .map(DevGroupResponse::from)
                    .toList()
            );
        }
    }

    public record DevGroupResponse(Long groupId, String name, String code) {
        static DevGroupResponse from(DevToolService.DevGroupResult result) {
            return new DevGroupResponse(result.groupId(), result.name(), result.code());
        }
    }

    public record DevGroupListResponse(List<DevGroupSummaryResponse> groups) {
        static DevGroupListResponse from(DevToolService.DevGroupListResult result) {
            return new DevGroupListResponse(result.groups().stream()
                .map(DevGroupSummaryResponse::from)
                .toList());
        }
    }

    public record DevGroupSummaryResponse(Long groupId, String name, String code, long memberCount) {
        static DevGroupSummaryResponse from(DevToolService.DevGroupSummaryResult result) {
            return new DevGroupSummaryResponse(
                result.groupId(),
                result.name(),
                result.code(),
                result.memberCount()
            );
        }
    }

    public record DevGroupDetailResponse(
        Long groupId,
        String name,
        String code,
        int memberCount,
        List<DevGroupMemberResponse> members
    ) {
        static DevGroupDetailResponse from(DevToolService.DevGroupDetailResult result) {
            return new DevGroupDetailResponse(
                result.groupId(),
                result.name(),
                result.code(),
                result.memberCount(),
                result.members().stream()
                    .map(DevGroupMemberResponse::from)
                    .toList()
            );
        }
    }

    public record DevGroupMemberResponse(
        Long memberId,
        String nickname,
        String profileImageKey,
        LocalDateTime joinedAt
    ) {
        static DevGroupMemberResponse from(DevToolService.DevGroupMemberResult result) {
            return new DevGroupMemberResponse(
                result.memberId(),
                result.nickname(),
                result.profileImageKey(),
                result.joinedAt()
            );
        }
    }
}
