/**
 * 
 */
package com.microsoft.exchange.exception;

/**
 * @author ctcudd
 *
 */
public class ExchangeEventConverterException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7311932467373345426L;

	/**
	 * @param message
	 */
	public ExchangeEventConverterException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public ExchangeEventConverterException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public ExchangeEventConverterException(String message, Throwable cause) {
		super(message, cause);
	}

}
