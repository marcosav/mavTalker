package com.gmail.marcosav2010.connection.exception;

public class ConnectionException extends Exception {

	private static final long serialVersionUID = 4764600295741975224L;

	public ConnectionException(String msg) {
		super(msg);
	}
	
	public ConnectionException(String msg, Throwable t) {
		super(msg, t);
	}
}
