package cmc.mody.member.infrastructure.repository;

import cmc.mody.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
