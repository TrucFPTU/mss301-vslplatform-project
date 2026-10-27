package com.vsl.common.web;

import com.vsl.common.exception.DataExceptionHandler;
import com.vsl.common.exception.GlobalExceptionHandler;
import com.vsl.common.exception.SecurityExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Tự động nạp các exception handler cho mọi service web dùng common-lib.
 * Nhờ vậy service không cần component-scan package com.vsl.common thủ công.
 */
@AutoConfiguration
@ConditionalOnClass(DispatcherServlet.class)
@Import(GlobalExceptionHandler.class)
public class WebAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.security.access.AccessDeniedException")
    @Import(SecurityExceptionHandler.class)
    static class SecurityHandlerConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.dao.DataIntegrityViolationException")
    @Import(DataExceptionHandler.class)
    static class DataHandlerConfiguration {
    }
}
