package cmc.mody.common.presentation;

import cmc.mody.common.api.ApiResponse;
import cmc.mody.common.api.dto.PresignedUrlResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {
    @PostMapping("/presigned-url")
    public ApiResponse<PresignedUrlResponse> createPresignedUrl(
        @RequestParam String domain,
        @RequestParam String fileName
    ) {
        return ApiResponse.ok(new PresignedUrlResponse(
            "https://storage.example.com/upload",
            domain + "/2026/06/" + fileName,
            300L
        ));
    }
}
