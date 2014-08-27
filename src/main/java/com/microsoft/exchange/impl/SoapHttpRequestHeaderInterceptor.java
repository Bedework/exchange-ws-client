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
package com.microsoft.exchange.impl;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

public class SoapHttpRequestHeaderInterceptor implements HttpRequestInterceptor {
	@Override
	public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        if (request.containsHeader(HTTP.TRANSFER_ENCODING)) {
            request.removeHeaders(HTTP.TRANSFER_ENCODING);
        }
        if (request.containsHeader(HTTP.CONTENT_LEN)) {
            request.removeHeaders(HTTP.CONTENT_LEN);
        }	    
	}
}
