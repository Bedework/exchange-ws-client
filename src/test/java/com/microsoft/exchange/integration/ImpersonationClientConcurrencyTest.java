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
package com.microsoft.exchange.integration;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.math.stat.descriptive.SynchronizedSummaryStatistics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.microsoft.exchange.DateHelp;
import com.microsoft.exchange.impl.ThreadLocalImpersonationConnectingSIDSourceImpl;
import com.microsoft.exchange.messages.FindItem;
import com.microsoft.exchange.messages.FindItemResponse;
import com.microsoft.exchange.types.ConnectingSIDType;

/**
 * Perform some tests targeted at observing throttling policy and other issues
 * when using a number of concurrent connections configured with impersonation support.
 * 
 * @author Nicholas Blair
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/com/microsoft/exchange/exchangeContext-usingImpersonation.xml")
public class ImpersonationClientConcurrencyTest extends AbstractIntegrationTest {



	private int targetConcurrency;
	/**
	 * @return the targetConcurrency
	 */
	public int getTargetConcurrency() {
		return targetConcurrency;
	}
	/**
	 * @param targetConcurrency the targetConcurrency to set
	 */
	@Value("${http.maxTotalConnections}")
	public void setTargetConcurrency(int targetConcurrency) {
		this.targetConcurrency = targetConcurrency;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.integration.AbstractIntegrationTest#initializeCredentials()
	 */
	@Override
	public void initializeCredentials() {
		ConnectingSIDType connectingSID = new ConnectingSIDType();
		connectingSID.setPrincipalName(emailAddress);
		ThreadLocalImpersonationConnectingSIDSourceImpl.setConnectingSID(connectingSID);
	}

	@Test @Override
	public void getPrimaryCalendarFolder() {
		super.getPrimaryCalendarFolder();
	}
	
	/**
	 * 
	 * @throws InterruptedException 
	 */
	@Test
	public void testConcurrentFindItems() throws InterruptedException {
		final int threadCount = targetConcurrency;
		// setup a latch to stall all threads until ready to run all at once (-1 so the last thread's run invocation triggers the start)
		final CountDownLatch startLatch = new CountDownLatch(threadCount);
		final CountDownLatch endLatch = new CountDownLatch(threadCount);
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		final Date start = DateHelp.makeDate(startDate);
		final Date end = DateHelp.makeDate(endDate);
		final SynchronizedSummaryStatistics stats = new SynchronizedSummaryStatistics();
		try {
			for(int i = 0; i < threadCount; i++) {
				final int index = i;
				executor.submit(new Runnable() {
					@Override
					public void run() {
						try {
							initializeCredentials();
							FindItem request = constructFindItemRequest(DateUtils.addDays(start, index), DateUtils.addDays(end, index), emailAddress);
							startLatch.countDown();
							try {
								startLatch.await();
							} catch (InterruptedException e) {
								throw new IllegalStateException("interrupted while waiting to start", e);
							}
							for(int j = 0; j < 10; j++) {
								StopWatch time = new StopWatch();
								time.start();
								FindItemResponse response = null;
								try {
									response = ewsClient.findItem(request);
								}catch(Exception e) {
									log.error(e);
								}
								time.stop();
								String capture = capture(response);
								log.info(Thread.currentThread().getName() + " response: " + capture);
								
								stats.addValue(time.getTime());
							}
						} finally {
							endLatch.countDown();
						}
					}
				});
			}
			// now block until everybody is done
			endLatch.await();
			log.info("testConcurrentFindItems complete for " + targetConcurrency + " threads, stats: " + stats);
		} finally {
			executor.shutdown();
		}

	}
}
