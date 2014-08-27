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
