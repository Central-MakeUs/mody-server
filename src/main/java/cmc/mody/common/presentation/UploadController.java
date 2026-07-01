package cmc.mody.common.presentation;

import cmc.mody.auth.presentation.support.CurrentMember;
import cmc.mody.common.api.ApiResponse;
import cmc.mody.common.api.dto.PresignedUrlResponse;
import cmc.mody.common.upload.UploadService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/uploads")
public class UploadController {
    private final UploadService uploadService;

    @PostMapping("/presigned-url")
    public ApiResponse<PresignedUrlResponse> createPresignedUrl(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @RequestParam String domain,
        @RequestParam String fileName
    ) {
        UploadService.PresignedUrlResult result = uploadService.createPresignedUrl(memberId, domain, fileName);
        return ApiResponse.ok(PresignedUrlResponse.from(result));
    }
}
