/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.mqtt.broker.service.processing.downlink;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.mqtt.broker.cluster.ServiceInfoProvider;
import org.thingsboard.mqtt.broker.gen.queue.QueueProtos;
import org.thingsboard.mqtt.broker.queue.TbQueueCallback;
import org.thingsboard.mqtt.broker.queue.TbQueueMsgMetadata;
import org.thingsboard.mqtt.broker.queue.common.TbProtoQueueMsg;
import org.thingsboard.mqtt.broker.queue.provider.DownLinkBasicPublishMsgQueueFactory;
import org.thingsboard.mqtt.broker.queue.provider.DownLinkPersistentPublishMsgQueueFactory;
import org.thingsboard.mqtt.broker.queue.publish.TbPublishBlockingQueue;
import org.thingsboard.mqtt.broker.queue.stats.ProducerStatsManager;
import org.thingsboard.mqtt.broker.service.analysis.ClientLogger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Slf4j
@Service
@RequiredArgsConstructor
class DownLinkQueuePublisherImpl implements DownLinkQueuePublisher {
    private final ServiceInfoProvider serviceInfoProvider;
    private final DownLinkBasicPublishMsgQueueFactory downLinkBasicPublishMsgQueueFactory;
    private final DownLinkPersistentPublishMsgQueueFactory downLinkPersistentPublishMsgQueueFactory;
    private final DownLinkPublisherHelper downLinkPublisherHelper;
    private final ClientLogger clientLogger;
    private final ProducerStatsManager statsManager;

    private TbPublishBlockingQueue<QueueProtos.ClientPublishMsgProto> basicPublisherQueue;
    private TbPublishBlockingQueue<QueueProtos.DevicePublishMsgProto> persistentPublisherQueue;


    @Value("${queue.basic-downlink-publish-msg.publisher-thread-max-delay}")
    private long basicMaxDelay;
    @Value("${queue.persistent-downlink-publish-msg.publisher-thread-max-delay}")
    private long persistentMaxDelay;

    @PostConstruct
    public void init() {
        this.basicPublisherQueue = TbPublishBlockingQueue.<QueueProtos.ClientPublishMsgProto>builder()
                .queueName("basicDownlink")
                .producer(downLinkBasicPublishMsgQueueFactory.createProducer(serviceInfoProvider.getServiceId()))
                .maxDelay(basicMaxDelay)
                .statsManager(statsManager)
                .build();
        this.basicPublisherQueue.init();
        this.persistentPublisherQueue = TbPublishBlockingQueue.<QueueProtos.DevicePublishMsgProto>builder()
                .queueName("persistentDownlink")
                .producer(downLinkPersistentPublishMsgQueueFactory.createProducer(serviceInfoProvider.getServiceId()))
                .maxDelay(persistentMaxDelay)
                .statsManager(statsManager)
                .build();
        this.persistentPublisherQueue.init();
    }

    // TODO: what to do if sending msg to Kafka fails?
    @Override
    public void publishBasicMsg(String targetServiceId, String clientId, QueueProtos.PublishMsgProto msg) {
        String topic = downLinkPublisherHelper.getBasicDownLinkServiceTopic(targetServiceId);
        QueueProtos.ClientPublishMsgProto clientPublishMsgProto = QueueProtos.ClientPublishMsgProto.newBuilder()
                .setClientId(clientId)
                .setPublishMsg(msg)
                .build();
        clientLogger.logEvent(clientId, this.getClass(), "Putting msg to basic down-link queue");
        basicPublisherQueue.add(new TbProtoQueueMsg<>(msg.getTopicName(), clientPublishMsgProto),
                new TbQueueCallback() {
                    @Override
                    public void onSuccess(TbQueueMsgMetadata metadata) {
                        clientLogger.logEvent(clientId, this.getClass(), "Sent msg to basic down-link queue");
                        if (log.isTraceEnabled()) {
                            log.trace("[{}] Successfully published BASIC msg to {} service.", clientId, targetServiceId);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.warn("[{}] Failed to publish BASIC msg to {} service.", clientId, targetServiceId, t);
                    }
                },
                topic
        );
    }

    @Override
    public void publishPersistentMsg(String targetServiceId, String clientId, QueueProtos.DevicePublishMsgProto msg) {
        String topic = downLinkPublisherHelper.getPersistentDownLinkServiceTopic(targetServiceId);
        clientLogger.logEvent(clientId, this.getClass(), "Putting msg to persistent down-link queue");
        persistentPublisherQueue.add(new TbProtoQueueMsg<>(clientId, msg),
                new TbQueueCallback() {
                    @Override
                    public void onSuccess(TbQueueMsgMetadata metadata) {
                        clientLogger.logEvent(clientId, this.getClass(), "Sent msg to persistent down-link queue");
                        if (log.isTraceEnabled()) {
                            log.trace("[{}] Successfully published PERSISTENT msg to {} service.", clientId, targetServiceId);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.warn("[{}] Failed to publish PERSISTENT msg to {} service.", clientId, targetServiceId, t);
                    }
                },
                topic
        );
    }

    @PreDestroy
    public void destroy() {
        if (basicPublisherQueue != null) {
            basicPublisherQueue.destroy();
        }
        if (persistentPublisherQueue != null) {
            persistentPublisherQueue.destroy();
        }
    }
}
