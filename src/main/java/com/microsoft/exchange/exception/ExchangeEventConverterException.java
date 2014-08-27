/**
 * See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Board of Regents of the University of Wisconsin System
 * licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
