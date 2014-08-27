/**
 * 
 */
package com.microsoft.exchange.ical.model;

import net.fortuna.ical4j.model.property.XProperty;

import com.microsoft.exchange.types.FolderIdType;

/**
 * @author ctcudd
 *
 */
public class ItemTypeParentFolderChangeKey extends XProperty {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5828192510311418283L;

	private static final String X_EWS_PARENT_FOLDER_CHANGEKEY ="X-EWS-PARENT-FOLDER-CHANGEKEY";

	public ItemTypeParentFolderChangeKey(FolderIdType parentFolderId) {
		super(X_EWS_PARENT_FOLDER_CHANGEKEY, parentFolderId.getChangeKey());
	}

}
