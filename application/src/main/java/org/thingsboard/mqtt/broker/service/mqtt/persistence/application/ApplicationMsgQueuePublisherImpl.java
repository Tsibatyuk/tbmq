/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.mqtt.broker.service.mqtt.persistence.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.mqtt.broker.cluster.ServiceInfoProvider;
import org.thingsboard.mqtt.broker.common.util.ThingsBoardExecutors;
import org.thingsboard.mqtt.broker.gen.queue.QueueProtos;
import org.thingsboard.mqtt.broker.queue.TbQueueCallback;
import org.thingsboard.mqtt.broker.queue.TbQueueMsgMetadata;
import org.thingsboard.mqtt.broker.queue.common.TbProtoQueueMsg;
import org.thingsboard.mqtt.broker.queue.provider.ApplicationPersistenceMsgQueueFactory;
import org.thingsboard.mqtt.broker.queue.publish.TbPublishServiceImpl;
import org.thingsboard.mqtt.broker.service.analysis.ClientLogger;
import org.thingsboard.mqtt.broker.service.mqtt.persistence.application.util.MqttApplicationClientUtil;
import org.thingsboard.mqtt.broker.service.processing.PublishMsgCallback;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationMsgQueuePublisherImpl implements ApplicationMsgQueuePublisher {

    private final ClientLogger clientLogger;
    private final ApplicationPersistenceMsgQueueFactory applicationPersistenceMsgQueueFactory;
    private final ServiceInfoProvider serviceInfoProvider;

    private final boolean isTraceEnabled = log.isTraceEnabled();

    private TbPublishServiceImpl<QueueProtos.PublishMsgProto> publisher;

    @Value("${mqtt.handler.app_msg_callback_threads:0}")
    private int threadsCount;
    @Value("${queue.application-persisted-msg.client-id-validation:true}")
    private boolean validateClientId;
    @Value("${queue.application-persisted-msg.shared-topic-validation:true}")
    private boolean validateSharedTopicFilter;

    private ExecutorService callbackProcessor;

    @PostConstruct
    public void init() {
        this.callbackProcessor = ThingsBoardExecutors.initExecutorService(threadsCount, "app-msg-callback-processor");
        this.publisher = TbPublishServiceImpl.<QueueProtos.PublishMsgProto>builder()
                .queueName("applicationMsg")
                .producer(applicationPersistenceMsgQueueFactory.createProducer(serviceInfoProvider.getServiceId()))
                .partition(0)
                .build();
        this.publisher.init();
    }

    @Override
    public void sendMsg(String clientId, QueueProtos.PublishMsgProto msgProto, PublishMsgCallback callback) {
        clientLogger.logEvent(clientId, this.getClass(), "Start waiting for APPLICATION msg to be persisted");
        String clientQueueTopic = MqttApplicationClientUtil.getAppTopic(clientId, validateClientId);
        publisher.send(new TbProtoQueueMsg<>(msgProto.getTopicName(), msgProto),
                new TbQueueCallback() {
                    @Override
                    public void onSuccess(TbQueueMsgMetadata metadata) {
                        callbackProcessor.submit(() -> {
                            clientLogger.logEvent(clientId, this.getClass(), "Persisted msg in APPLICATION Queue");
                            if (isTraceEnabled) {
                                log.trace("[{}] Successfully sent publish msg to the queue.", clientId);
                            }
                            callback.onSuccess();
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        callbackProcessor.submit(() -> {
                            log.error("[{}] Failed to send publish msg to the queue for MQTT topic {}.",
                                    clientId, msgProto.getTopicName(), t);
                            callback.onFailure(t);
                        });
                    }
                },
                clientQueueTopic);
    }

    @Override
    public void sendMsgToSharedTopic(String sharedTopic, QueueProtos.PublishMsgProto msgProto, PublishMsgCallback callback) {
        publisher.send(new TbProtoQueueMsg<>(msgProto),
                new TbQueueCallback() {
                    @Override
                    public void onSuccess(TbQueueMsgMetadata metadata) {
                        callbackProcessor.submit(() -> {
                            if (isTraceEnabled) {
                                log.trace("[{}] Successfully sent publish msg to the shared topic queue. Partition: {}", sharedTopic, metadata.getMetadata().partition());
                            }
                            callback.onSuccess();
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        callbackProcessor.submit(() -> {
                            log.error("[{}] Failed to send publish msg to the shared topic queue.",
                                    sharedTopic, t);
                            callback.onFailure(t);
                        });
                    }
                },
                MqttApplicationClientUtil.getSharedAppTopic(sharedTopic, validateSharedTopicFilter));
    }

    @PreDestroy
    public void destroy() {
        publisher.destroy();
        if (callbackProcessor != null) {
            callbackProcessor.shutdownNow();
        }
    }
}
