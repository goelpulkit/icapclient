/**
 *
 */
package com.lafaspot.icap.client;

import javax.annotation.Nonnull;

import com.lafaspot.icap.client.exception.IcapException;

/**
 * @author kraman
 *
 */
public class IcapResult {

    /** violation filename. */
    private String violationFilename;

    /** Symantec violation id. */
    private String violationId;

    /** Symantec violation name. */
    private String violationName;

    /** Number of violations found. */
    private int numViolations;

    /** Disposition returned by Symantec server. */
    private Disposition disposition;

    /**
     * @return the violationFilename
     */
    public String getViolationFilename() {
        return violationFilename;
    }




    /**
     * @param violationFilename the violationFilename to set
     */
    public void setViolationFilename(String violationFilename) {
        this.violationFilename = violationFilename;
    }




    /**
     * @return the violationId
     */
    public String getViolationId() {
        return violationId;
    }




    /**
     * @param violationId the violationId to set
     */
    public void setViolationId(String violationId) {
        this.violationId = violationId;
    }




    /**
     * @return the violationName
     */
    public String getViolationName() {
        return violationName;
    }




    /**
     * @param violationName the violationName to set
     */
    public void setViolationName(String violationName) {
        this.violationName = violationName;
    }




    /**
     * @return the numViolations
     */
    public int getNumViolations() {
        return numViolations;
    }




    /**
     * @param numViolations the numViolations to set
     */
    public void setNumViolations(int numViolations) {
        this.numViolations = numViolations;
    }




    /**
     * @return the disposition
     */
    public Disposition getDisposition() {
        return disposition;
    }




    /**
     * @param disposition the disposition to set
     * @throws IcapException on failure
     */
    public void setDispositionAsStr(String dispositionStr) throws IcapException {
        this.disposition = Disposition.fromStrng(dispositionStr.trim());
    }

    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer();
        buf.append("NumberOfViolations: ");
        buf.append(numViolations);
        if (null != violationFilename) {
            buf.append(", ViolationFilename: ");
            buf.append(violationFilename);
        }
        if (null != violationId) {
            buf.append(", ViolationId: ");
            buf.append(violationId);
        }
        if (null != violationName) {
            buf.append(", ViolationName: ");
            buf.append(violationName);
        }

        if (null != disposition) {

            buf.append(", Disposition: ");
            buf.append(disposition);
        }
        return buf.toString();
    }


    public enum Disposition {
        NOT_FIXED(0), REPAIRED(1), DELETED(2);

        private Disposition(int v) {
            intVal = v;
        }

        public static Disposition fromStrng(@Nonnull final String val) throws IcapException {
            try {
                int intVal = Integer.parseInt(val);
                switch (intVal) {
                case 0:
                    return NOT_FIXED;
                case 1:
                    return REPAIRED;
                case 2:
                    return DELETED;
                default:
                    throw new IcapException("parse error");
                }
            } catch (NumberFormatException e) {
                throw new IcapException("parse error");
            }

        }

        private final int intVal;
    }

}
