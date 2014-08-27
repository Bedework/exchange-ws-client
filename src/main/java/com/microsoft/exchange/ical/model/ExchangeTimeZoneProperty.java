/**
 * 
 */
package com.microsoft.exchange.ical.model;

import net.fortuna.ical4j.model.property.XProperty;

/**
 * @author ctcudd
 *
 */
public class ExchangeTimeZoneProperty extends XProperty {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5679555633801009746L;
	private static final String X_EWS_TIMEZONE ="X-EWS-TIMEZONE";

	public ExchangeTimeZoneProperty(String timeZoneId) {
		super(X_EWS_TIMEZONE, timeZoneId);
	}

}
