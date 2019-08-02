/*******************************************************************************
 * Copyright 2017 Cognizant Technology Solutions
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package com.cognizant.devops.platformwebhookengine.app;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import com.cognizant.devops.platformcommons.config.ApplicationConfigCache;
import com.cognizant.devops.platformcommons.config.ApplicationConfigProvider;


import com.cognizant.devops.platformwebhookengine.modules.aggregator.EngineAggregatorModule;

public class Application {
    private static Logger log = LogManager.getLogger(Application.class.getName());

    private static int defaultInterval = 600;
    private Application() {

    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            defaultInterval = Integer.valueOf(args[0]);
        }
        try {
            // Load insight configuration
            ApplicationConfigCache.loadConfigCache();

            ApplicationConfigProvider.performSystemCheck();

            // Subscribe for desired events.

            JobDetail aggrgatorJob = JobBuilder.newJob(EngineAggregatorModule.class)
                .withIdentity("EngineAggregatorModule", "iSight").build();

            Trigger aggregatorTrigger = TriggerBuilder.newTrigger()
                .withIdentity("EngineAggregatorModuleTrigger", "iSight")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInSeconds(defaultInterval)
                    .repeatForever())
                .build();


            Scheduler scheduler;

            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
            scheduler.scheduleJob(aggrgatorJob, aggregatorTrigger);
            //EngineStatusLogger.getInstance().createEngineStatusNode("Platform Engine Service Started ",PlatformServiceConstants.SUCCESS);

        } catch (SchedulerException e) {
            //EngineStatusLogger.getInstance().createEngineStatusNode("Platform Engine Service not running due to Scheduler Exception "+e.getMessage(),PlatformServiceConstants.FAILURE);
            log.error(e);
        } catch (Exception e) {
            //EngineStatusLogger.getInstance().createEngineStatusNode("Platform Engine Service not running "+e.getMessage(),PlatformServiceConstants.FAILURE);
            log.error(e);
        }
    }

}