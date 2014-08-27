/**
 * 
 */
package com.microsoft.exchange.ical.model;
import com.microsoft.exchange.types.ItemIdType;

import net.fortuna.ical4j.model.property.XProperty;

/**
 * @author ctcudd
 *
 */
public class ItemTypeChangeKey extends XProperty {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6922783488869641439L;
	
	private static final String X_EWS_ITEM_CHANGEKEY ="X-EWS-CHANGEKEY";
	
	public ItemTypeChangeKey(ItemIdType itemIdType) {
		super(X_EWS_ITEM_CHANGEKEY, itemIdType.getChangeKey());
	}

}
