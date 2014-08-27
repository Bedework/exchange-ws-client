/**
 * 
 */
package com.microsoft.exchange.ical.model;

import net.fortuna.ical4j.model.property.XProperty;

/**
 * @author ctcudd
 *
 */
public class ExchangeStartTimeZoneProperty extends XProperty {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2399431457950768783L;
	private static final String X_EWS_START_TIMEZONE ="X-EWS-START-TIMEZONE";
	
	public ExchangeStartTimeZoneProperty(String startTimeZoneId) {
		super(X_EWS_START_TIMEZONE, startTimeZoneId);
	}

	
}
