package cl.tenpo.customerauthentication.constants;

public final class ErrorCode {

    private ErrorCode() {
        throw new UnsupportedOperationException("Constant class");
    }

    public static final String BLOCKED_PASSWORD = "1151";
    public static final String MISSING_PARAMETERS = "1400";
    public static final String INVALID_CREDENTIALS = "1410";
    public static final String INVALID_PAN = "1411";
    public static final String INVALID_TOKEN = "1412";

    public static final String EXTERNAL_ID_NOT_FOUND = "1100";
    public static final String TRANSACTION_CONTEXT_EXPIRED = "1200";
    public static final String TRANSACTION_CONTEXT_CANCELED = "1201";
    public static final String TRANSACTION_CONTEXT_CLOSED = "1202";

    public static final String INTERNAL_ERROR = "500";
}
