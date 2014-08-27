package com.microsoft.exchange.exception;

public class AutodiscoverException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7659779477291199459L;
	public AutodiscoverException(String s) {
		super(s);
	}
	public AutodiscoverException(Exception e) {
		super(e);
	}
}
