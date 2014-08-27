package com.microsoft.exchange.exception;

public class SoapAutodiscoverException extends AutodiscoverException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5889558892962802033L;

	public SoapAutodiscoverException(String arg){
		super(arg);
	}
	
	public SoapAutodiscoverException(Exception arg){
		super(arg);
	}
}
