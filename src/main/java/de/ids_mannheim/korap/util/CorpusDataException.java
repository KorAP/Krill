package de.ids_mannheim.korap.util;

/**
 * Exception class for corpus data processing problems.
 * 
 * @author diewald
 */
public class CorpusDataException extends Exception {

    private int errorCode = 0;


    /**
     * Construct a new CorpusDataException.
     */
    public CorpusDataException () {
        super();
    };


    /**
     * Construct a new CorpusDataException.
     * 
     * @param message
     *            Exception message.
     */
    public CorpusDataException (String message) {
        super(message);
    };


    /**
     * Construct a new CorpusDataException.
     * 
     * @param code
     *            An integer value as an error code.
     * @param message
     *            Exception message.
     */
    public CorpusDataException (int code, String message) {
        super(message);
        this.setErrorCode(code);
    };


    /**
     * Construct a new CorpusDataException.
     * 
     * @param message
     *            Exception message.
     * @param cause
     *            A {@link Throwable} object.
     */
    public CorpusDataException (String message, Throwable cause) {
        super(message, cause);
    };


    /**
     * Construct a new CorpusDataException.
     * 
     * @param cause
     *            A {@link Throwable} object.
     */
    public CorpusDataException (Throwable cause) {
        super(cause);
    };


    /**
     * Get the error code of the exception.
     * 
     * @return The error code of the exception as an integer.
     */
    public int getErrorCode () {
        return this.errorCode;
    };


    /**
     * Set the error code of the exception.
     * 
     * @param code
     *            The error code of the exception as an integer.
     */
    public void setErrorCode (int code) {
        this.errorCode = code;
    };
};
