package de.ids_mannheim.korap.util;

/**
 * Exception class for query processing problems.
 *
 * @author diewald
 */
public class QueryException extends Exception {
	
    private int errorCode = 0;
  
    /**
     * Construct a new QueryException.
     */
    public QueryException() {
        super();
    };


    /**
     * Construct a new QueryException.
     *
     * @param message Exception message.
     */
    public QueryException (String message) {
        super(message);
    };


    /**
     * Construct a new QueryException.
     *
     * @param code An integer value as an error code.
     * @param message Exception message.
     */
    public QueryException (int code, String message) {	  
        super(message);
        this.setErrorCode(code);      
    };


    /**
     * Construct a new QueryException.
     *
     * @param message Exception message.
     * @param cause A {@link Throwable} object.
     */
    public QueryException (String message, Throwable cause) {
        super(message, cause);
    };


    /**
     * Construct a new QueryException.
     *
     * @param cause A {@link Throwable} object.
     */    
    public QueryException (Throwable cause) {
        super(cause);
    };  
  

    /**
     * Get the error code of the exception.
     *
     * @return The error code of the exception as an integer.
     */
    public int getErrorCode() {
        return this.errorCode;
    };


    /**
     * Set the error code of the exception.
     *
     * @param code The error code of the exception as an integer.
     */
    public void setErrorCode (int code) {
        this.errorCode = code;
    };
};
