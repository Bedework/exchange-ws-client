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
package com.microsoft.exchange;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.ibm.icu.util.Region;
import com.ibm.icu.util.Region.RegionType;
import com.ibm.icu.util.TimeZone;

public class ICU4J_Test {
	private Log log = LogFactory.getLog(this.getClass());

	@Test
	public void getAllAvailableRegions() {
		for(RegionType regionType : RegionType.values()){
			Set<Region> availableRegions = Region.getAvailable(regionType);
			log.info(regionType+" contains "+ availableRegions.size() +" regions");
			for(Region region : availableRegions){
				log.info(region);
			}
		}
	}
	
	@Test
	public void getWindowsTimeZoneIDForSysTimeZoneIDs(){
		int validZonesCount = 0;
		String[] availableIDs = TimeZone.getAvailableIDs();
		for(String sysTimeZoneID : availableIDs){
			String windowsID = TimeZone.getWindowsID(sysTimeZoneID);
			if(null != windowsID){
				log.info(sysTimeZoneID+" maps to "+windowsID);
				validZonesCount++;
			}else{
				log.warn("NO MAPPING FOR "+sysTimeZoneID);
			}
		}
		log.info(validZonesCount +"/"+availableIDs.length +" system time zone ids can be mapped to a windows time zone id");
	}

	
}
