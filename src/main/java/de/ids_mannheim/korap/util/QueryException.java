package de.ids_mannheim.korap.util;

public class QueryException extends Exception {
	
  int errorCode;
  
  public QueryException() {
      super();
  }

  public QueryException(String message) {
      super(message);
  }

  public QueryException(String message, Throwable cause) {
      super(message, cause);
  };

  public QueryException(Throwable cause) {
      super(cause);
  };  
  
  public QueryException(int code, String message) {	  
	  super(message);
	  setErrorCode(code);      
  }

  public int getErrorCode() {
	return errorCode;
  }

  public void setErrorCode(int errorCode) {
	this.errorCode = errorCode;
  }

};
