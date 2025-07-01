//package me.suhsaechan.suhprojectutility.util.deprecated;
//
//import static me.suhsaechan.suhprojectutility.util.deprecated.LogUtil.lineLog;
//import static me.suhsaechan.suhprojectutility.util.deprecated.LogUtil.superLog;
//
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import me.suhsaechan.suhprojectutility.util.exception.ErrorCode;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.reflect.MethodSignature;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Component;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.request.ServletRequestAttributes;
//
//@Deprecated
//@Aspect
//@Component
//@RequiredArgsConstructor
//@Slf4j
//class MethodInvocationLoggingAspect {
//
//	private final Logger LOGGER = LoggerFactory.getLogger(MethodInvocationLoggingAspect.class);
//
//	@Around("@annotation(me.suhsaechan.suhprojectutility.util.deprecated.LogMethodInvocation) || @annotation(me.suhsaechan.suhprojectutility.util.deprecated.LogMonitoringInvocation)")
//	public Object logMethod(ProceedingJoinPoint joinPoint) throws Throwable {
//		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
//
//		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//		HttpServletRequest request = attributes.getRequest();
//		String requestId = (String) request.getAttribute("RequestID");
//
////		// 기존
////		LOGGER.info("[{}] RequestID: {}, Parameter: {}", signature.getMethod().getName(), requestId,
////				Arrays.toString(joinPoint.getArgs()));
//
//		// 신규 (LogUtil 존재시)
//		lineLog(signature.getMethod().getName());
//		log.info("RequestId : {} ", requestId);
//		log.info("Parameter : ");
//		superLog(joinPoint.getArgs());
//		lineLog(null);
//
//
//		Object result = ErrorCode.INTERNAL_SERVER_ERROR;
//		try {
//			result = joinPoint.proceed();
//		} finally {
////			// 기존
////			LOGGER.info("[{}] RequestID: {}, Result: {}", signature.getMethod().getName(), requestId, result);
//
//			// 신규
//			lineLog(signature.getMethod().getName());
//			log.info("RequestId : {} ", requestId);
//			log.info("Parameter : ");
//			superLog(result);
//			lineLog(null);
//		}
//
//		return result;
//	}
//}
