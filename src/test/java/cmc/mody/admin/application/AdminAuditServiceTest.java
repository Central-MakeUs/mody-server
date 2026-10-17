package cmc.mody.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cmc.mody.admin.domain.AdminAccount;
import cmc.mody.admin.domain.AdminRole;
import cmc.mody.common.api.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminAuditServiceTest {
    @Autowired private AdminAuditService audit;

    @Test
    void recordsActorsAndPagesNewestFirst() {
        AdminAccount operator = new AdminAccount(1L, "google-sub", "ops@example.com", AdminRole.OPERATOR);
        audit.record(operator, "POST", "/api/v1/admin/groups/1/name", 200);
        audit.record(null, "DELETE", "/api/v1/admin/records/2", 204);
        audit.record(operator, "PATCH", "/x".repeat(400), 400);

        var first = audit.list(null, null, 2);
        assertThat(first.items()).hasSize(2);
        assertThat(first.hasMore()).isTrue();
        var rest = audit.list(null, first.nextCursor(), 2);
        assertThat(rest.items()).hasSize(1);

        assertThat(audit.list(AdminAuditService.MACHINE, null, 10).items())
            .extracting(AdminAuditService.Item::method).containsExactly("DELETE");
        assertThat(audit.list("ops@example.com", null, 10).items()).hasSize(2)
            .allSatisfy(item -> assertThat(item.path().length()).isLessThanOrEqualTo(500));
        assertThatThrownBy(() -> audit.list(null, "bad", 10)).isInstanceOf(GeneralException.class);
    }
}
