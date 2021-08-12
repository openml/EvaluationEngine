package org.openml.webapplication.exceptions;

public class LegacyWarning extends Exception {
	
	private static final long serialVersionUID = 3016391070447835856L;
	private int errorNo;
	private String message;
	
	public LegacyWarning(int errorNo, String message) {
		this.errorNo = errorNo;
		this.message = message;
	}

	public int getErrorNo() {
		return errorNo;
	}

	public String getMessage() {
		return message;
	}
}
