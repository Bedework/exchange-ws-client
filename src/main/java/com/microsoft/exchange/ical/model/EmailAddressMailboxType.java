package com.microsoft.exchange.ical.model;

import com.microsoft.exchange.types.MailboxTypeType;

import net.fortuna.ical4j.model.parameter.XParameter;
/**
 * XParameter indented to take the value of a {@link MailboxTypeType}
 * 
 * @author ctcudd
 *
 */
public class EmailAddressMailboxType extends XParameter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5365130984683399890L;
	private static final String EMAIL_ADDRESS_MAILBOX_TYPE = "X-EWS-EMAIL-ADDRESS-MAILBOX-TYPE";
	
	public EmailAddressMailboxType(MailboxTypeType mailboxTypeType) {
		super(EMAIL_ADDRESS_MAILBOX_TYPE, mailboxTypeType.value());
	}

}
