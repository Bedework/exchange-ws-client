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

package com.microsoft.exchange.impl;

import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapMessage;

import com.microsoft.exchange.types.ConnectingSIDType;

/**
 * {@link ImpersonationConnectingSIDSource} implemented with a {@link ThreadLocal}.
 * 
 * @author Nicholas Blair
 */
public class ThreadLocalImpersonationConnectingSIDSourceImpl implements ImpersonationConnectingSIDSource {

	private static final ThreadLocal<ConnectingSIDType> threadLocal = new ThreadLocal<ConnectingSIDType>();
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.impl.ConnectingSIDSource#getConnectingSID(org.springframework.ws.soap.SoapMessage, org.springframework.ws.context.MessageContext)
	 */
	@Override
	public ConnectingSIDType getConnectingSID(SoapMessage soapMessage,
			MessageContext messageContext) {
		return threadLocal.get();
	}

	/**
	 * Set the specified {@link ConnectingSIDType} in the {@link ThreadLocal}.
	 * 
	 * @param connectingSID
	 */
	public static void setConnectingSID(ConnectingSIDType connectingSID) {
		threadLocal.set(connectingSID);
	}
	
	/**
	 * Remove the {@link ConnectingSIDType} from the thread local.
	 */
	public static void clear() {
		threadLocal.remove();
	}
}
