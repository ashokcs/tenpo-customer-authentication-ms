package cl.tenpo.customerauthentication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class TenpoException extends RuntimeException {

    private HttpStatus code;
    private String[] reasons;
    private Integer errorCode;

    public TenpoException(String message) {
        super(message);
    }

    public TenpoException(String message, Throwable cause) {
        super(message, cause);
    }

    public TenpoException(HttpStatus code, Integer errorCode) {
        this.errorCode = errorCode;
        this.code = code;
    }

    public TenpoException(HttpStatus code, Integer errorCode, String message) {
        this(message);
        this.code = code;
        this.errorCode = errorCode;
    }

    public TenpoException(Exception cause, HttpStatus code, Integer errorCode) {
        super(cause);
        this.code = code;
        this.errorCode = errorCode;
    }

    public TenpoException(Exception cause, HttpStatus code, Integer errorCode, String message) {
        this(message, cause);
        this.code = code;
        this.errorCode = errorCode;
    }

    public TenpoException(HttpStatus code, String message, String... reasons) {
        this(message);
        this.code = code;
        this.reasons = reasons;
    }

    public TenpoException(int code, String message) {
        this(message);
        this.code = HttpStatus.valueOf(code);
    }

    public ErrorResponse getErrorResponse() {
        ErrorResponse response = new ErrorResponse();
        response.setMessage(this.getMessage());
        return response;
    }

    public ResponseEntity<ErrorResponse> getResponse() {
        return new ResponseEntity(this.getErrorResponse(), this.code);
    }

    public HttpStatus getCode() {
        return this.code;
    }

    public String[] getReasons() {
        return this.reasons;
    }

    public Integer getErrorCode() {
        return this.errorCode;
    }

    public void setCode(HttpStatus code) {
        this.code = code;
    }

    public void setReasons(String[] reasons) {
        this.reasons = reasons;
    }

    public TenpoException setReason(String ... reasons) {
        this.reasons = reasons;
        return this;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }
}
