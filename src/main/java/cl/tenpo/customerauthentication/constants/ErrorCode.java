package cl.tenpo.customerauthentication.constants;

public final class ErrorCode {

    private ErrorCode() {
        throw new UnsupportedOperationException("Constant class");
    }

    public static final String CHALLENGE_EXPIRED = "1200";
    public static final String CHALLENGE_CANCELED = "1201";
    public static final String USER_NOT_FOUND_OR_LOCKED = "1150";

    public static final String PUSH_NOTIFICATION_ERROR = "1300";
    public static final String ERROR_EMAIL_NOTIFICATION = "1301";
}
