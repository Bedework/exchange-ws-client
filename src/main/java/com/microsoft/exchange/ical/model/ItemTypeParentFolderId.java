/**
 * 
 */
package com.microsoft.exchange.ical.model;

import com.microsoft.exchange.types.FolderIdType;
import com.microsoft.exchange.types.ItemIdType;

import net.fortuna.ical4j.model.property.XProperty;

/**
 * @author ctcudd
 *
 */
public class ItemTypeParentFolderId extends XProperty {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3908080047637096789L;

	private static final String X_EWS_PARENT_FOLDER_ID = "X-EWS-PARENT-FOLDER-ID";
	
	public ItemTypeParentFolderId(FolderIdType parentFolderId) {
		super(X_EWS_PARENT_FOLDER_ID, parentFolderId.getId());
	}

}
