package com.vsl.common.web;

import com.vsl.common.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Tự động nạp GlobalExceptionHandler cho mọi service web dùng common-lib.
 * Nhờ vậy service không cần component-scan package com.vsl.common thủ công.
 */
@AutoConfiguration
@ConditionalOnClass(DispatcherServlet.class)
@Import(GlobalExceptionHandler.class)
public class WebAutoConfiguration {
}
