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

import java.util.List;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Test;

public class DateHelpTest {
	
	private final Log log = LogFactory.getLog(this.getClass());

	@Test
	public void splitIntervalTest() {
		DateTime start= new DateTime();
		DateTime end = start.plusYears(1);
		Assert.assertTrue(start.isBefore(end));
		
		Assert.assertTrue( (end.getMillis() > start.getMillis()) );
		
		List<Interval> intervals = DateHelp.generateIntervals(start.toDate(), end.toDate());
		Assert.assertNotNull(intervals);
		Assert.assertEquals(2, intervals.size());
		Interval lastInterval = null;
		for(Interval interval : intervals) {
			log.info(interval);
			if(lastInterval != null) {
				Assert.assertTrue(interval.abuts(lastInterval));
			}
			lastInterval=interval;
		}
		
	}
	
	@Test
	public void splitMultipleIntervalTest() {
		DateTime start= new DateTime();
		DateTime end = start.plusYears(1);
		Assert.assertTrue(start.isBefore(end));
		
		Assert.assertTrue( (end.getMillis() > start.getMillis()) );
		
		List<Interval> intervals = DateHelp.generateMultipleIntervals(start.toDate(), end.toDate(),4);
		Assert.assertNotNull(intervals);
		Assert.assertEquals(4, intervals.size());
		Interval lastInterval = null;
		for(Interval interval : intervals) {
			log.info(interval);
			if(lastInterval != null) {
				Assert.assertTrue(interval.abuts(lastInterval));
			}
			lastInterval=interval;
		}
		
	}


}
