package me.suhsaechan.translate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.suhsaechan.common.constant.CommonStatus;
import me.suhsaechan.common.constant.TranslatorLanguage;
import me.suhsaechan.common.constant.TranslatorType;

/**
 * 번역 관련 응답을 처리하는 통합 응답 DTO 클래스
 * 모듈의 모든 API 엔드포인트는 이 단일 응답 클래스를 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationResponse {
    
    // 번역된 텍스트
    private String translatedText;
    
    // 원본 텍스트
    private String originalText;
    
    // 사용된 번역기
    private TranslatorType translatorType;
    
    // 감지된/원본 언어 (기존 코드 호환성을 위해 detectedLang 유지)
    private TranslatorLanguage detectedLang;
    
    // 원본 언어 (sourceLang과 detectedLang 둘 다 지원)
    private TranslatorLanguage sourceLang;
    
    // 목표 언어
    private TranslatorLanguage targetLang;
    
    // 처리 결과 상태 (기존 코드 호환성)
    private CommonStatus result;
}