package cl.tenpo.customerauthentication.constants;

public final class ErrorCode {

    private ErrorCode() {
        throw new UnsupportedOperationException("Constant class");
    }

    public static final Integer BLOCKED_PASSWORD = 1151;
    public static final Integer MISSING_PARAMETERS = 1400;
    public static final Integer INVALID_CREDENTIALS = 1410;
    public static final Integer INVALID_PAN = 1411;
    public static final Integer INVALID_TOKEN = 1412;
    public static final Integer EXTERNAL_ID_NOT_FOUND = 1100;
    public static final Integer TRANSACTION_CONTEXT_EXPIRED = 1200;
    public static final Integer TRANSACTION_CONTEXT_CANCELED = 1201;
    public static final Integer TRANSACTION_CONTEXT_CLOSED = 1202;
    public static final Integer CONNECTION_ERROR = 501;
    public static final Integer INTERNAL_ERROR = 500;
}
