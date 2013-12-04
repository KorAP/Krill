package de.ids_mannheim.korap.util;

public class QueryException extends Exception {

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
};
