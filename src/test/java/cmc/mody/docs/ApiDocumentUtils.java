package cmc.mody.docs;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

public final class ApiDocumentUtils {
    public static FieldDescriptor[] commonResponseFields(FieldDescriptor... resultFields) {
        FieldDescriptor[] descriptors = new FieldDescriptor[resultFields.length + 3];
        descriptors[0] = fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("요청 성공 여부");
        descriptors[1] = fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드");
        descriptors[2] = fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지");
        System.arraycopy(resultFields, 0, descriptors, 3, resultFields.length);
        return descriptors;
    }

    private ApiDocumentUtils() {
    }
}
