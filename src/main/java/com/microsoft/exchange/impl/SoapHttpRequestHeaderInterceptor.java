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
