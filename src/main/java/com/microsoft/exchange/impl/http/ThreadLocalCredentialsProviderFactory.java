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

import java.net.URI;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;

/**
 * {@link CredentialsProviderFactory} that returns a {@link CredentialsProvider} backed
 * by a {@link ThreadLocal}.
 * Integrators should guarantee a call to {@link #set(Credentials)} in the thread before 
 * the {@link HttpClient} execute call is initiated.
 * 
 * @author Nicholas Blair
 */
public class ThreadLocalCredentialsProviderFactory implements CredentialsProviderFactory {

	private static final ThreadLocal<Credentials> threadLocal = new ThreadLocal<Credentials>();
	
	/*
	 * (non-Javadoc)
	 * @see com.microsoft.exchange.impl.http.CredentialsProviderFactory#getCredentialsProvider(java.net.URI)
	 */
	@Override
	public CredentialsProvider getCredentialsProvider(URI uri) {
		return new CredentialsProvider() {
			@Override
			public void setCredentials(AuthScope authscope,
					Credentials credentials) {
				ThreadLocalCredentialsProviderFactory.set(credentials);
			}
			@Override
			public Credentials getCredentials(AuthScope authscope) {
				return ThreadLocalCredentialsProviderFactory.get();
			}
			@Override
			public void clear() {
				ThreadLocalCredentialsProviderFactory.clear();
			}
		};
	}

	/**
	 * 
	 * @return the current {@link Credentials} stored in the {@link ThreadLocal}.
	 */
	public static Credentials get() {
		return threadLocal.get();
	}
	
	/**
	 * Set a {@link Credentials} instance in the {@link ThreadLocal}
	 * @param credentials
	 */
	public static void set(Credentials credentials) {
		threadLocal.set(credentials);
	}
	
	/**
	 * Clear the {@link ThreadLocal}.
	 * @see {@link ThreadLocal#remove()}
	 */
	public static void clear() {
		threadLocal.remove();
	}
}
