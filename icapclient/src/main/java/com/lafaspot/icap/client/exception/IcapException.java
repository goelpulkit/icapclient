/**
 *
 */
package com.lafaspot.icap.client.exception;

import javax.annotation.Nonnull;

/**
 * IcapException - encapsulates failure reason.
 *
 * @author kraman
 *
 */
public class IcapException extends Exception {

    public IcapException(String message) {
        super(message);
    }

    /**
     * Constructor with failure type.
     *
     * @param failureType type of failure
     */
    public IcapException(@Nonnull FailureType failureType) {
        super(failureType.getMessage());
        this.failureType = failureType;
    }

    private FailureType failureType;

    /**
     * Types of failures.
     *
     * @author kraman
     *
     */
    public static enum FailureType {
        /** Message parsing failed/ */
        PARSE_ERROR("Parse error - failed to parse ICAP message"),
        /** Not connected to Symantec AV Server. */
        NOT_CONNECTED("Not connected to server"),
        SESSION_IN_USE("Session in use.");

        @Nonnull
        private final String message;

        @Nonnull
        private FailureType(@Nonnull final String message) {
            this.message = message;
        }

        @Nonnull
        public String getMessage() {
            return message;
        }
    }
}
