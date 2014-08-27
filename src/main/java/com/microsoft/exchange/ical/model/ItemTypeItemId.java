package com.microsoft.exchange.ical.model;

import com.microsoft.exchange.types.ItemIdType;

import net.fortuna.ical4j.model.property.XProperty;
/**
 * 
 * @author ctcudd
 *
 */
public class ItemTypeItemId extends XProperty{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7840657473114832271L;
	private static final String X_EWS_ITEM_ID ="X-EWS-ITEMID";

	public ItemTypeItemId(ItemIdType itemIdType) {
		super(X_EWS_ITEM_ID, itemIdType.getId());
	}

}
