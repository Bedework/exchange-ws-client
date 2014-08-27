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
