package error;

public class DimeWsException extends RuntimeException {

    public int status;

    public DimeWsException(int status, String message) {
        super(message);
        this.status=status;
    }

}
