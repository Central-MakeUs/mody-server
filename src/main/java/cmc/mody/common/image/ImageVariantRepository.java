package cmc.mody.common.image;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageVariantRepository extends JpaRepository<ImageVariant, Long> {
    boolean existsByImageKey(String imageKey);

    List<ImageVariant> findByImageKeyInAndVariantStatus(Collection<String> imageKeys, ImageVariantStatus variantStatus);

    /**
     * 처리할 작업을 선점한다. 여러 서버(블루그린 전환 중 두 컨테이너)가 같은 작업을 동시에 가져가지 않도록 SKIP LOCKED를 쓴다.
     * 처리 중 서버가 내려가 PROCESSING으로 남은 작업도 일정 시간이 지나면 다시 가져간다.
     */
    @Query(value = """
        select *
          from image_variant
         where deleted_at is null
           and (
                (variant_status = 'PENDING' and (next_attempt_at is null or next_attempt_at <= :now))
             or (variant_status = 'PROCESSING' and picked_at < :staleBefore)
           )
         order by id asc
         limit :batchSize
         for update skip locked
        """, nativeQuery = true)
    List<ImageVariant> findDueForUpdateSkipLocked(
        @Param("now") LocalDateTime now,
        @Param("staleBefore") LocalDateTime staleBefore,
        @Param("batchSize") int batchSize
    );

    /** 아직 생성 요청이 없는 기존 사진(활동 기록·챌린지 인증)을 최신순으로 찾는다. */
    @Query(value = """
        select candidate.image_key
          from (
                select record.image_key, record.uploaded_at
                  from activity_record record
                 where record.deleted_at is null
                union all
                select proof.image_key, proof.uploaded_at
                  from challenge_proof proof
                 where proof.deleted_at is null
               ) candidate
         where candidate.image_key not like 'http%'
           and not exists (
                select 1 from image_variant variant where variant.image_key = candidate.image_key
           )
         order by candidate.uploaded_at desc
         limit :limit
        """, nativeQuery = true)
    List<String> findImageKeysWithoutVariant(@Param("limit") int limit);
}
