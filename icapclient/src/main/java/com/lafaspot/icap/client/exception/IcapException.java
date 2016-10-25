/**
 *
 */
package com.lafaspot.icap.client.exception;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * IcapException - encapsulates failure reason.
 *
 * @author kraman
 *
 */
public class IcapException extends Exception {


    /** Failure type. */
    private FailureType failureType;

    /**
     * The map of validation errors. Specifies what input caused the invalid input error. Can be null.
     */
    @Nullable
    private List<String> errorDetail = null;

    public IcapException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        final StringBuffer buf = new StringBuffer(super.getMessage());
        if (null != errorDetail) {
            buf.append(", Details:");
            buf.append(errorDetail);
        }
        return buf.toString();
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

    /**
     * Constructor with failure type.
     *
     * @param failureType type of failure
     * @param errorDetail more info on the error
     */
    public IcapException(@Nonnull FailureType failureType, @Nullable List<String> errorDetail) {
        super(failureType.getMessage());
        this.failureType = failureType;
        this.errorDetail = errorDetail;
    }

    /**
     * Constructor with failure type.
     *
     * @param failureType type of failure
     * @param cause the wrapped exception
     */
    public IcapException(@Nonnull FailureType failureType, @Nullable Throwable cause) {
        super(failureType.getMessage(), cause);
    }

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
