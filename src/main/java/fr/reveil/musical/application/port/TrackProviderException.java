package fr.reveil.musical.application.port;

public class TrackProviderException extends RuntimeException {

    public TrackProviderException(String provider, String message) {
        super(provider + ": " + message);
    }

    public TrackProviderException(String provider, String message, Throwable cause) {
        super(provider + ": " + message, cause);
    }
}
