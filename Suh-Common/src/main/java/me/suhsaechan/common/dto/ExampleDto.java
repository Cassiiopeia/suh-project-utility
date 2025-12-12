package me.suhsaechan.common.dto;

import kr.suhsaechan.ai.annotation.AiClass;
import kr.suhsaechan.ai.annotation.AiSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 구조화된 응답을 위한 예제 DTO
 * JsonSchema.fromClass() 메서드로 스키마를 자동 생성하여 AI에게 응답 형식을 지정합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@AiClass(
    title = "Example Response",
    description = "Structured data format for AI response"
)
public class ExampleDto {

    @AiSchema(
        description = "Title of the content",
        required = true,
        minLength = 1,
        maxLength = 100,
        example = "Spring Boot Performance Optimization"
    )
    private String title;

    @AiSchema(
        description = "Main content description",
        required = true,
        minLength = 10,
        example = "This article covers essential techniques for optimizing Spring Boot applications."
    )
    private String content;

    @AiSchema(
        description = "Category of the content",
        allowableValues = {"Technology", "Business", "Lifestyle", "Other"}
    )
    private String category;

    @AiSchema(
        description = "Priority level from 1 to 5",
        minimum = "1",
        maximum = "5",
        example = "3"
    )
    private Integer priority;

    @AiSchema(
        description = "List of relevant tags"
    )
    private List<String> tags;
}
