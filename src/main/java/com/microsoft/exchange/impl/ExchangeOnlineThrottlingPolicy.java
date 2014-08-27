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
package com.microsoft.exchange.impl;

/**
 * This class encapsulates information regarding the throttling policies
 * applicable to Exchange Online.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/exchange/hh881884%28v=exchg.140%29.aspx">http://msdn.microsoft.com/en-us/library/exchange/hh881884%28v=exchg.140%29.aspx</a>
 * @author Nicholas Blair
 */
public class ExchangeOnlineThrottlingPolicy {

	/**
	 * Indicates that there are more concurrent requests against the server than are allowed by a user's policy.
	 */
	public static final String ERROR_EXCEEDED_CONNECTION_COUNT = "ErrorExceededConnectionCount";
	/**
	 * Indicates that a user's throttling policy maximum subscription count has been exceeded.
	 */
	public static final String ERROR_EXCEEDED_SUBSCRIPTION_COUNT = "ErrorExceededSubscriptionCount";
	/**
	 * Indicates that a search operation call has exceeded the total number of items that can be returned.
	 */
	public static final String ERROR_EXCEEDED_FIND_COUNT_LIMIT = "ErrorExceededFindCountLimit";
	/**
	 * Occurs when the server is busy.
	 */
	public static final String ERROR_SERVER_BUSY = "ErrorServerBusy";
	/**
	 * The maximum number of entries returned for FindItem requests.
	 */
	public static final int FIND_ITEM_MAX_ENTRIES_RETURNED = 1000;
	/**
	 * The maximum number of concurrent connections for a service account using impersonation.
	 */
	public static final int MAX_CONCURRENT_CONNECTIONS_IMPERSONATION = 10;
}
