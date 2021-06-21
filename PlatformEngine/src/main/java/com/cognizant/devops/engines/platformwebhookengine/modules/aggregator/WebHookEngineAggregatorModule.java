/*******************************************************************************
 * Copyright 2017 Cognizant Technology Solutions
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.cognizant.devops.engines.platformwebhookengine.modules.aggregator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cognizant.devops.engines.platformengine.message.core.EngineStatusLogger;
import com.cognizant.devops.engines.platformengine.message.factory.EngineSubscriberResponseHandler;
import com.cognizant.devops.engines.platformwebhookengine.message.subscriber.WebHookDataSubscriber;
import com.cognizant.devops.engines.platformwebhookengine.message.subscriber.WebhookHealthSubscriber;
import com.cognizant.devops.platformcommons.config.ApplicationConfigInterface;
import com.cognizant.devops.platformcommons.config.ApplicationConfigProvider;
import com.cognizant.devops.platformcommons.constants.PlatformServiceConstants;
import com.cognizant.devops.platformdal.webhookConfig.WebHookConfig;
import com.cognizant.devops.platformdal.webhookConfig.WebHookConfigDAL;

public class WebHookEngineAggregatorModule extends TimerTask  implements ApplicationConfigInterface{
	private static Logger log = LogManager.getLogger(WebHookEngineAggregatorModule.class.getName());
	
	private static Map<String, EngineSubscriberResponseHandler> registry = new HashMap<>();
	private Map<String,String> loggingInfo = new HashMap<>();
	private static final String WEBHOOK_HEALTH_ROUTING_KEY = "WEBHOOKSUBSCRIBER_HEALTH";
	private static  final String TOOL_NAME ="toolName";
	private static final String AGENT_ID ="agentId";
	

	@Override
	public void run() {
		log.debug(" Webhook Engine started ==== ");
		try {
			ApplicationConfigInterface.loadConfiguration();
			ApplicationConfigProvider.performSystemCheck();
			WebHookConfigDAL webhookConfigDal = new WebHookConfigDAL();
			List<WebHookConfig> allWebhookConfigurations = webhookConfigDal.getAllActiveWebHookConfigurations();
			for (WebHookConfig webhookConfig : allWebhookConfigurations) {
				loggingInfo.put(TOOL_NAME, webhookConfig.getToolName());
				loggingInfo.put(AGENT_ID, webhookConfig.getWebHookName());
				registerAggragators(webhookConfig);
			}
			registerWebhookHealthAggragators(WEBHOOK_HEALTH_ROUTING_KEY);
		} catch (Exception e) {
			log.error(e);
			EngineStatusLogger.getInstance().createEngineStatusNode(
					" Error occured while initializing webhook Aggregator Module   " + e.getMessage(),
					PlatformServiceConstants.FAILURE);
		}
		log.debug(" Webhook Engine completed ==== ");
	}

	private void registerAggragators(WebHookConfig webhookConfig) {

		String dataRoutingKey = webhookConfig.getMQChannel();
		try {
			if (dataRoutingKey != null && !registry.containsKey(dataRoutingKey)) {
				registry.put(dataRoutingKey, new WebHookDataSubscriber(webhookConfig, dataRoutingKey));
				log.debug(" Type=WebhookEngine toolName={} category={} WebHookName={} routingKey={} dataSize={} execId={} ProcessingTime={} Webhook {} subscribed successfully ",loggingInfo.get(TOOL_NAME),"-",loggingInfo.get(AGENT_ID),dataRoutingKey,0,"-",0, webhookConfig.getWebHookName());
				EngineStatusLogger.getInstance().createWebhookEngineStatusNode(
						"Webhook " + webhookConfig.getWebHookName() + " subscribed successfully ",
						PlatformServiceConstants.SUCCESS);
			} else {
				WebHookDataSubscriber dataSubscriber = (WebHookDataSubscriber) registry.get(dataRoutingKey);
				dataSubscriber.setWebhookConfig(webhookConfig);
			}
		} catch (Exception e) {
			EngineStatusLogger.getInstance().createWebhookEngineStatusNode("Unable to subscribed Webhook "
					+ webhookConfig.getWebHookName() + " Error Detail :" + e.getMessage(),
					PlatformServiceConstants.FAILURE);
			log.error(" toolName={} agentId={} routingKey={} Unable to add subscriber for routing key: ",loggingInfo.get(TOOL_NAME),loggingInfo.get(AGENT_ID),dataRoutingKey, e);
		}
	}

	private void registerWebhookHealthAggragators(String healthRoutingKey) {
		try {
			if (healthRoutingKey != null && !registry.containsKey(healthRoutingKey)) {
				registry.put(healthRoutingKey, new WebhookHealthSubscriber(healthRoutingKey));
				log.debug(" Type=WebhookEngine toolName={} category={} WebHookName={} routingKey={} dataSize={} execId={} ProcessingTime={} Webhook Health Queue subscribed successfully{} ",loggingInfo.get(TOOL_NAME),"-",loggingInfo.get(AGENT_ID),healthRoutingKey,0,"-",0, healthRoutingKey);
				EngineStatusLogger.getInstance().createWebhookEngineStatusNode(
						"Webhook  Health Queue " + healthRoutingKey + " subscribed successfully ",
						PlatformServiceConstants.SUCCESS);
			}
		} catch (Exception e) {
			EngineStatusLogger.getInstance().createWebhookEngineStatusNode("Unable to subscribed Webhook Health Queue "
					+ healthRoutingKey + " Error Detail :" + e.getMessage(), PlatformServiceConstants.FAILURE);
			log.error(" toolName={} agentId={} routingKey={} Unable to add health subscriber for routing key: ",loggingInfo.get(TOOL_NAME),loggingInfo.get(AGENT_ID),healthRoutingKey, e);

		}
	}

}