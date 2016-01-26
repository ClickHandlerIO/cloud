package ses.data;

public enum EmailSendErrorReason {
    CONNECTION_ERROR,
    ERROR_BUILDING_MESSAGE,
    ERROR_401_422,
    ERROR_500,
    UNKOWN_CONNECTION_ERROR,
}
