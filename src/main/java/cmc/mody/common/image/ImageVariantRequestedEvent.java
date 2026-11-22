package cmc.mody.common.image;

/** 새 사진이 기록·인증에 연결됐다는 이벤트. 커밋된 뒤에만 축소본 생성 작업을 만든다. */
public record ImageVariantRequestedEvent(String imageKey) {
}
