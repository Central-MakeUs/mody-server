package cmc.mody.common.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UploadController.class)
@AutoConfigureRestDocs
class UploadControllerDocsTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createPresignedUrl() throws Exception {
        mockMvc.perform(post("/api/v1/uploads/presigned-url?domain=record&fileName=meal.jpg"))
            .andExpect(status().isOk())
            .andDo(document("upload-presigned-url",
                resource(ResourceSnippetParameters.builder()
                    .tag("Upload")
                    .summary("Presigned URL 생성")
                    .description("이미지 업로드용 presigned URL과 저장할 imageKey를 발급한다.")
                    .queryParameters(
                        parameterWithName("domain").description("업로드 도메인: record, weekly-challenge 등"),
                        parameterWithName("fileName").description("원본 파일명")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.presignedUrl").type(JsonFieldType.STRING).description("업로드 URL"),
                        fieldWithPath("result.imageKey").type(JsonFieldType.STRING).description("서버 API에 전달할 이미지 key"),
                        fieldWithPath("result.expiresInSeconds").type(JsonFieldType.NUMBER).description("만료 시간 초")
                    ))
                    .build())
            ));
    }
}
