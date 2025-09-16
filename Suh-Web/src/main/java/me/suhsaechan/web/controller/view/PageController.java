package me.suhsaechan.web.controller.view;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.service.UserAuthority;
import me.suhsaechan.notice.dto.NoticeResponse;
import me.suhsaechan.notice.service.NoticeService;
import me.suhsaechan.grassplanter.dto.GrassRequest;
import me.suhsaechan.grassplanter.dto.GrassResponse;
import me.suhsaechan.grassplanter.service.GrassService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
@RequiredArgsConstructor
public class PageController {
	
	private final UserAuthority userAuthority;
	private final NoticeService noticeService;
	private final GrassService grassService;

	@GetMapping("/")
	public String indexPage(Model model) {
		NoticeResponse noticeResponse = noticeService.getActiveNotices();
		model.addAttribute("notices", noticeResponse.getNotices());
		return "pages/dashboard";
	}

	@GetMapping("/login")
	public String loginPage() {
		return "pages/login";
	}

	@GetMapping("/dashboard")
	public String dashboardPage(Model model){
		NoticeResponse noticeResponse = noticeService.getActiveNotices();
		model.addAttribute("notices", noticeResponse.getNotices());
		return "pages/dashboard";
	}

	@GetMapping("/issue-helper")
	public String issueHelperPage(Model model){
		return "pages/githubIssueHelper";
	}

	@GetMapping("/openai-chat")
	public String openAiChatPage(Model model){
		return "pages/openAiChat";
	}

	@GetMapping("/translator")
	public String translatorPage(Model model){
		return "pages/translator";
	}
	
	@GetMapping("/notice-management")
	public String noticeManagementPage(HttpSession session, Model model){
		// 슈퍼관리자 권한 확인
		if (!userAuthority.isSuperAdmin(session)) {
			log.warn("권한 없는 사용자의 공지사항 관리 페이지 접근 시도");
			return "redirect:/error/403";
		}
		
		return "pages/noticeManagement";
	}

	/**
	 * 컨테이너 로그 페이지 매핑
	 *
	 * @return 뷰 이름
	 */
	@GetMapping("/docker-logs")
	public String dockerLogsPage() {
		return "pages/dockerLogs";
	}

	/**
	 * GrassPlanter 페이지 매핑
	 *
	 * @param model 모델
	 * @return 뷰 이름
	 */
	@GetMapping("/grass")
	public String grassPlanterPage(Model model) {
		// 프로필 목록 조회
		GrassRequest request = GrassRequest.builder().build();
		GrassResponse response = grassService.getProfiles(request);
		
		model.addAttribute("profiles", response.getProfiles());
		model.addAttribute("totalProfiles", response.getTotalElements());
		
		return "pages/grassPlanter";
	}
}
