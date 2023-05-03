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
package org.thingsboard.mqtt.broker.service.mqtt.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thingsboard.mqtt.broker.actors.client.state.ClientActorStateInfo;
import org.thingsboard.mqtt.broker.adaptor.ProtoConverter;
import org.thingsboard.mqtt.broker.common.data.ClientInfo;
import org.thingsboard.mqtt.broker.common.data.ClientType;
import org.thingsboard.mqtt.broker.gen.queue.QueueProtos.PublishMsgProto;
import org.thingsboard.mqtt.broker.service.analysis.ClientLogger;
import org.thingsboard.mqtt.broker.service.mqtt.persistence.application.ApplicationMsgQueuePublisher;
import org.thingsboard.mqtt.broker.service.mqtt.persistence.application.ApplicationPersistenceProcessor;
import org.thingsboard.mqtt.broker.service.mqtt.persistence.device.DevicePersistenceProcessor;
import org.thingsboard.mqtt.broker.service.mqtt.persistence.device.queue.DeviceMsgQueuePublisher;
import org.thingsboard.mqtt.broker.service.processing.MultiplePublishMsgCallbackWrapper;
import org.thingsboard.mqtt.broker.service.processing.PublishMsgCallback;
import org.thingsboard.mqtt.broker.service.processing.data.PersistentMsgSubscriptions;
import org.thingsboard.mqtt.broker.service.subscription.Subscription;
import org.thingsboard.mqtt.broker.service.subscription.shared.TopicSharedSubscription;
import org.thingsboard.mqtt.broker.session.ClientSessionCtx;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.mqtt.broker.common.data.ClientType.APPLICATION;
import static org.thingsboard.mqtt.broker.common.data.ClientType.DEVICE;

@Slf4j
@Service
@RequiredArgsConstructor
public class MsgPersistenceManagerImpl implements MsgPersistenceManager {

    private final GenericClientSessionCtxManager genericClientSessionCtxManager;
    private final ApplicationMsgQueuePublisher applicationMsgQueuePublisher;
    private final ApplicationPersistenceProcessor applicationPersistenceProcessor;
    private final DeviceMsgQueuePublisher deviceMsgQueuePublisher;
    private final DevicePersistenceProcessor devicePersistenceProcessor;
    private final ClientLogger clientLogger;

    @Override
    public void processPublish(PublishMsgProto publishMsgProto, PersistentMsgSubscriptions persistentSubscriptions, PublishMsgCallback callback) {
        List<Subscription> deviceSubscriptions = getSubscriptionsIfNotNull(persistentSubscriptions.getDeviceSubscriptions());
        List<Subscription> applicationSubscriptions = getSubscriptionsIfNotNull(persistentSubscriptions.getApplicationSubscriptions());
        Set<String> sharedTopics = getUniqueSharedTopics(persistentSubscriptions.getAllApplicationSharedSubscriptions());

        int callbackCount = getCallbackCount(deviceSubscriptions, applicationSubscriptions, sharedTopics);
        if (callbackCount == 0) {
            return;
        }
        PublishMsgCallback callbackWrapper = new MultiplePublishMsgCallbackWrapper(callbackCount, callback);

        String senderClientId = ProtoConverter.getClientId(publishMsgProto);
        clientLogger.logEvent(senderClientId, this.getClass(), "Before msg persistence");

        if (deviceSubscriptions != null) {
            for (Subscription deviceSubscription : deviceSubscriptions) {
                deviceMsgQueuePublisher.sendMsg(
                        getClientIdFromSubscription(deviceSubscription),
                        createReceiverPublishMsg(deviceSubscription, publishMsgProto),
                        callbackWrapper);
            }
        }
        if (applicationSubscriptions != null) {
            if (applicationSubscriptions.size() == 1) {
                Subscription applicationSubscription = applicationSubscriptions.get(0);
                applicationMsgQueuePublisher.sendMsg(
                        getClientIdFromSubscription(applicationSubscription),
                        createReceiverPublishMsg(applicationSubscription, publishMsgProto),
                        callbackWrapper);
            } else {
                for (Subscription applicationSubscription : applicationSubscriptions) {
                    applicationMsgQueuePublisher.sendMsg(
                            getClientIdFromSubscription(applicationSubscription),
                            createReceiverPublishMsg(applicationSubscription, publishMsgProto),
                            callbackWrapper);
                }
            }
        }
        if (sharedTopics != null) {
            for (String sharedTopic : sharedTopics) {
                applicationMsgQueuePublisher.sendMsgToSharedTopic(
                        sharedTopic,
                        createReceiverPublishMsg(publishMsgProto),
                        callbackWrapper);
            }
        }

        clientLogger.logEvent(senderClientId, this.getClass(), "After msg persistence");
    }

    private List<Subscription> getSubscriptionsIfNotNull(List<Subscription> persistentSubscriptions) {
        return CollectionUtils.isEmpty(persistentSubscriptions) ? null : persistentSubscriptions;
    }

    private Set<String> getUniqueSharedTopics(Set<Subscription> allAppSharedSubscriptions) {
        if (CollectionUtils.isEmpty(allAppSharedSubscriptions)) {
            return null;
        }
        return allAppSharedSubscriptions
                .stream()
                .collect(Collectors.groupingBy(Subscription::getTopicFilter))
                .keySet();
    }

    private String getClientIdFromSubscription(Subscription subscription) {
        return subscription.getClientSession().getClientId();
    }

    private int getCallbackCount(List<Subscription> deviceSubscriptions,
                                 List<Subscription> applicationSubscriptions,
                                 Set<String> sharedTopics) {
        return getCollectionSize(deviceSubscriptions) +
                getCollectionSize(applicationSubscriptions) +
                getCollectionSize(sharedTopics);
    }

    private <T> int getCollectionSize(Collection<T> collection) {
        return collection == null ? 0 : collection.size();
    }

    private PublishMsgProto createReceiverPublishMsg(Subscription clientSubscription, PublishMsgProto publishMsgProto) {
        var minQoSValue = Math.min(clientSubscription.getQos(), publishMsgProto.getQos());
        var retain = clientSubscription.getOptions().isRetain(publishMsgProto);
        return publishMsgProto.toBuilder()
                .setPacketId(0)
                .setQos(minQoSValue)
                .setRetain(retain)
                .build();
    }

    private PublishMsgProto createReceiverPublishMsg(PublishMsgProto publishMsgProto) {
        return publishMsgProto.toBuilder()
                .setPacketId(0)
                .build();
    }

    @Override
    public void startProcessingPersistedMessages(ClientActorStateInfo actorState, boolean wasPrevSessionPersistent) {
        ClientSessionCtx clientSessionCtx = actorState.getCurrentSessionCtx();
        genericClientSessionCtxManager.resendPersistedPubRelMessages(clientSessionCtx);

        ClientType clientType = clientSessionCtx.getSessionInfo().getClientInfo().getType();
        if (clientType == APPLICATION) {
            applicationPersistenceProcessor.startProcessingPersistedMessages(actorState);
        } else if (clientType == DEVICE) {
            if (!wasPrevSessionPersistent) {
                // TODO: it's still possible to get old msgs if DEVICE queue is overloaded
                // in case some messages got persisted after session clear
                devicePersistenceProcessor.clearPersistedMsgs(clientSessionCtx.getClientId());
            }
            devicePersistenceProcessor.startProcessingPersistedMessages(clientSessionCtx);
        }
    }

    @Override
    public void startProcessingSharedSubscriptions(ClientSessionCtx clientSessionCtx, Set<TopicSharedSubscription> subscriptions) {
        ClientType clientType = clientSessionCtx.getSessionInfo().getClientInfo().getType();
        if (clientType == APPLICATION) {
            applicationPersistenceProcessor.startProcessingSharedSubscriptions(clientSessionCtx, subscriptions);
        } else if (clientType == DEVICE) {
            devicePersistenceProcessor.startProcessingSharedSubscriptions(clientSessionCtx, subscriptions);
        }
    }

    @Override
    public void stopProcessingPersistedMessages(ClientInfo clientInfo) {
        if (clientInfo.getType() == APPLICATION) {
            applicationPersistenceProcessor.stopProcessingPersistedMessages(clientInfo.getClientId());
        } else if (clientInfo.getType() == DEVICE) {
            devicePersistenceProcessor.stopProcessingPersistedMessages(clientInfo.getClientId());
            // TODO: stop select query if it's running
        } else {
            log.warn("[{}] Persisted messages are not supported for client type {}.", clientInfo.getClientId(), clientInfo.getType());
        }
    }

    @Override
    public void saveAwaitingQoS2Packets(ClientSessionCtx clientSessionCtx) {
        genericClientSessionCtxManager.saveAwaitingQoS2Packets(clientSessionCtx);
    }

    @Override
    public void clearPersistedMessages(ClientInfo clientInfo) {
        // TODO: make async
        genericClientSessionCtxManager.clearAwaitingQoS2Packets(clientInfo.getClientId());
        if (clientInfo.getType() == APPLICATION) {
            applicationPersistenceProcessor.clearPersistedMsgs(clientInfo.getClientId());
        } else if (clientInfo.getType() == DEVICE) {
            devicePersistenceProcessor.clearPersistedMsgs(clientInfo.getClientId());
        }
    }

    @Override
    public void processPubAck(ClientSessionCtx clientSessionCtx, int packetId) {
        ClientInfo clientInfo = clientSessionCtx.getSessionInfo().getClientInfo();
        String clientId = clientInfo.getClientId();
        if (clientInfo.getType() == APPLICATION) {
            applicationPersistenceProcessor.processPubAck(clientId, packetId);
        } else if (clientInfo.getType() == DEVICE) {
            devicePersistenceProcessor.processPubAck(clientId, packetId);
        }
    }

    @Override
    public void processPubRec(ClientSessionCtx clientSessionCtx, int packetId) {
        ClientInfo clientInfo = clientSessionCtx.getSessionInfo().getClientInfo();
        String clientId = clientInfo.getClientId();
        if (clientInfo.getType() == APPLICATION) {
            applicationPersistenceProcessor.processPubRec(clientSessionCtx, packetId);
        } else if (clientInfo.getType() == DEVICE) {
            devicePersistenceProcessor.processPubRec(clientId, packetId);
        }
    }

    @Override
    public void processPubComp(ClientSessionCtx clientSessionCtx, int packetId) {
        ClientInfo clientInfo = clientSessionCtx.getSessionInfo().getClientInfo();
        String clientId = clientInfo.getClientId();
        if (clientInfo.getType() == APPLICATION) {
            applicationPersistenceProcessor.processPubComp(clientId, packetId);
        } else if (clientInfo.getType() == DEVICE) {
            devicePersistenceProcessor.processPubComp(clientId, packetId);
        }
    }
}
