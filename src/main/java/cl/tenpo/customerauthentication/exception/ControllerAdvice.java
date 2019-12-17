package cl.tenpo.customerauthentication.exception;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@RestControllerAdvice
@AllArgsConstructor
@Slf4j
public class ControllerAdvice {

    private MessageSource messageSource;

    @ExceptionHandler({TenpoException.class})
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleTenpoException(TenpoException ex) {
        String traceId = getTraceIdFromNowDate();
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setCode(this.getErrorCode(ex));
        errorResponse.setMessage(this.resolveMessage(ex));
        String message = String.format("%s [%s :: %s]", traceId, errorResponse.getCode(), errorResponse.getMessage());
        log.error(message, cause);

        return new ResponseEntity(errorResponse, ex.getCode());
    }

    private String getErrorCode(TenpoException exception) {
        return StringUtils.isBlank(exception.getErrorCode()) ? "SEMS-000" : exception.getErrorCode();
    }

    private String resolveMessage(TenpoException exception) {
        String message = null;
        String key = this.getErrorCode(exception);
        if (StringUtils.isBlank(exception.getMessage())) {
            String[] reasons = exception.getReasons();
            if (this.messageSource != null) {
                try {
                    message = this.messageSource.getMessage(key, reasons, Locale.ENGLISH);
                } catch (NoSuchMessageException var6) {
                    log.error("[resolveMessage] Unrecognized key {} to resolve message, returning key as message", key);
                }
            }
        } else {
            message = exception.getMessage();
        }

        return message;
    }

    private static String getTraceIdFromNowDate() {
        Format formatter = new SimpleDateFormat("yyyyMMddHHmmssSSSS");
        return formatter.format(new Date());
    }

}
