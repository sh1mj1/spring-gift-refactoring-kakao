package gift.order;

class IllegalStatusException extends RuntimeException {
    IllegalStatusException(String message) {
        super(message);
    }

    IllegalStatusException(String message, Throwable cause) {
        super(message, cause);
    }
}
