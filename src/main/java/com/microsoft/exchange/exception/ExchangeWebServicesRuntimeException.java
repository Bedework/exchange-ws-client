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
 * {@link RuntimeException} raised by the exchange web services client.
 * 
 * @author Nicholas Blair
 */
public class ExchangeWebServicesRuntimeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 202143418251140727L;

	/**
	 * 
	 */
	public ExchangeWebServicesRuntimeException() {
	}

	/**
	 * @param message
	 */
	public ExchangeWebServicesRuntimeException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public ExchangeWebServicesRuntimeException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public ExchangeWebServicesRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

}
