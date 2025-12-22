package me.suhsaechan.web.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 에러 페이지 처리 컨트롤러
 * Spring Boot의 기본 에러 페이지를 커스터마이징하여 HTML 에러 페이지를 반환합니다.
 */
@Controller
@Slf4j
public class CustomErrorController implements ErrorController {

	@RequestMapping("/error")
	public String handleError(HttpServletRequest request, Model model) {
		Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		
		if (status != null) {
			int statusCode = Integer.parseInt(status.toString());
			
			// 에러 상태 코드를 모델에 추가
			model.addAttribute("statusCode", statusCode);
			
			// 에러 메시지 추가
			Object errorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
			if (errorMessage != null) {
				model.addAttribute("errorMessage", errorMessage.toString());
			}
			
			// 요청 URI 추가
			Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
			if (requestUri != null) {
				model.addAttribute("requestUri", requestUri.toString());
			}
			
			log.warn("에러 발생 - 상태 코드: {}, 요청 URI: {}, 메시지: {}", 
					statusCode, requestUri, errorMessage);
			
			// 상태 코드별 에러 페이지 반환
			if (statusCode == HttpStatus.NOT_FOUND.value()) {
				return "error/404";
			} else if (statusCode == HttpStatus.FORBIDDEN.value()) {
				return "error/403";
			} else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
				return "error/500";
			}
		}
		
		// 기본 에러 페이지 (500)
		return "error/500";
	}
}

