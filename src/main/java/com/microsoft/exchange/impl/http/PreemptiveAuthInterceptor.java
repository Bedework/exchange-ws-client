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

package com.microsoft.exchange.impl.http;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.protocol.HttpContext;

/**
 * @author Nicholas Blair
 */
public class PreemptiveAuthInterceptor implements HttpRequestInterceptor {

	protected static final String PREEMPTIVE_AUTH = "com.microsoft.exchange.impl.http.preemptive-auth";
	private final AuthScope authScope;
	protected final Log log = LogFactory.getLog(this.getClass());
	/**
	 * 
	 */
	public PreemptiveAuthInterceptor() {
		this(AuthScope.ANY);
	}
	/**
	 * @param authScope
	 */
	public PreemptiveAuthInterceptor(AuthScope authScope) {
		this.authScope = authScope;
	}
	/* (non-Javadoc)
	 * @see org.apache.http.HttpRequestInterceptor#process(org.apache.http.HttpRequest, org.apache.http.protocol.HttpContext)
	 */
	@Override
	public void process(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
		if (authState.getAuthScheme() == null) {
			AuthScheme preemptiveAuthScheme = (AuthScheme) context.getAttribute(PREEMPTIVE_AUTH);
			if (preemptiveAuthScheme != null) {
				CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
				Credentials creds = credsProvider.getCredentials(authScope);
				if (creds == null) {
					throw new HttpException("No credentials for preemptive authentication");
				}
				authState.setAuthScheme(preemptiveAuthScheme);
				authState.setCredentials(creds);
				if(log.isTraceEnabled()) {
					log.trace("successfully set credentials " + creds + " and authScheme " + preemptiveAuthScheme + " for request " + request);
				}
			} else {
				log.warn(PREEMPTIVE_AUTH + " authScheme not found in context; make sure you are using the CustomHttpComponentsMessageSender and the preemptiveAuthEnabled property is set to true");
			}
		} else {
			log.warn("context's authState attribute (" + authState + ") has non-null AuthScheme for request " + request);
		}
	}

}
