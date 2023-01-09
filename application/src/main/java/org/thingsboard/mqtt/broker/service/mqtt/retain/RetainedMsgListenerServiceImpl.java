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
package org.thingsboard.mqtt.broker.service.mqtt.retain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.mqtt.broker.adaptor.ProtoConverter;
import org.thingsboard.mqtt.broker.cluster.ServiceInfoProvider;
import org.thingsboard.mqtt.broker.common.data.BasicCallback;
import org.thingsboard.mqtt.broker.constant.BrokerConstants;
import org.thingsboard.mqtt.broker.dto.RetainedMsgDto;
import org.thingsboard.mqtt.broker.gen.queue.QueueProtos;
import org.thingsboard.mqtt.broker.service.stats.StatsManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.thingsboard.mqtt.broker.common.data.util.CallbackUtil.createCallback;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetainedMsgListenerServiceImpl implements RetainedMsgListenerService {

    private final RetainedMsgService retainedMsgService;
    private final RetainedMsgPersistenceService retainedMsgPersistenceService;
    private final ServiceInfoProvider serviceInfoProvider;
    private final StatsManager statsManager;

    private ConcurrentMap<String, RetainedMsg> retainedMessagesMap;

    @Override
    public void init(Map<String, RetainedMsg> retainedMsgMap) {
        this.retainedMessagesMap = new ConcurrentHashMap<>(retainedMsgMap);
        statsManager.registerRetainedMsgStats(retainedMessagesMap);

        log.info("Restoring stored retained messages for {} topics.", retainedMsgMap.size());
        retainedMsgMap.forEach((topic, retainedMsg) -> {
            if (log.isTraceEnabled()) {
                log.trace("[{}] Restoring retained msg - {}.", topic, retainedMsg);
            }
            retainedMsgService.saveRetainedMsg(topic, retainedMsg);
        });
    }

    @Override
    public void startListening(RetainedMsgConsumer retainedMsgConsumer) {
        retainedMsgConsumer.listen(this::processRetainedMsgUpdate);
    }

    @Override
    public void cacheRetainedMsgAndPersist(String topic, RetainedMsg retainedMsg) {
        BasicCallback callback = createCallback(
                () -> log.trace("[{}] Persisted retained msg", topic),
                t -> log.warn("[{}] Failed to persist retained msg. Exception - {}, reason - {}",
                        topic, t.getClass().getSimpleName(), t.getMessage()));
        cacheRetainedMsgAndPersist(topic, retainedMsg, callback);
    }

    @Override
    public void cacheRetainedMsgAndPersist(String topic, RetainedMsg retainedMsg, BasicCallback callback) {
        log.trace("[{}] Executing cacheRetainedMsgAndPersist {}.", topic, retainedMsg);
        cacheRetainedMsg(topic, retainedMsg);

        QueueProtos.RetainedMsgProto retainedMsgProto = ProtoConverter.convertToRetainedMsgProto(retainedMsg);
        retainedMsgPersistenceService.persistRetainedMsgAsync(topic, retainedMsgProto, callback);
    }

    @Override
    public void cacheRetainedMsg(String topic, RetainedMsg retainedMsg) {
        log.trace("[{}] Executing cacheRetainedMsg {}.", topic, retainedMsg);
        retainedMsgService.saveRetainedMsg(topic, retainedMsg);
        retainedMessagesMap.put(topic, retainedMsg);
    }

    @Override
    public void clearRetainedMsgAndPersist(String topic) {
        BasicCallback callback = createCallback(
                () -> log.trace("[{}] Persisted cleared retained msg", topic),
                t -> log.warn("[{}] Failed to persist cleared retained msg. Exception - {}, reason - {}",
                        topic, t.getClass().getSimpleName(), t.getMessage()));
        clearRetainedMsgAndPersist(topic, callback);
    }

    @Override
    public void clearRetainedMsgAndPersist(String topic, BasicCallback callback) {
        log.trace("[{}] Executing clearRetainedMsgAndPersist", topic);
        clearRetainedMsg(topic);

        retainedMsgPersistenceService.persistRetainedMsgAsync(topic, BrokerConstants.EMPTY_RETAINED_MSG_PROTO, callback);
    }

    @Override
    public void clearRetainedMsg(String topic) {
        log.trace("[{}] Executing clearRetainedMsg", topic);
        retainedMsgService.clearRetainedMsg(topic);
        retainedMessagesMap.remove(topic);
    }

    @Override
    public RetainedMsgDto getRetainedMsgForTopic(String topic) {
        log.trace("[{}] Executing getRetainedMsgForTopic", topic);
        RetainedMsg retainedMsg = retainedMessagesMap.getOrDefault(topic, null);
        return retainedMsg != null ? RetainedMsgDto.newInstance(retainedMsg) : null;
    }

    private void processRetainedMsgUpdate(String topic, String serviceId, RetainedMsg retainedMsg) {
        if (serviceInfoProvider.getServiceId().equals(serviceId)) {
            log.trace("[{}] Msg was already processed.", topic);
            return;
        }
        if (retainedMsg == null) {
            log.trace("[{}][{}] Clearing remote retained msg.", serviceId, topic);
            clearRetainedMsg(topic);
        } else {
            log.trace("[{}][{}] Saving remote retained msg.", serviceId, topic);
            cacheRetainedMsg(topic, retainedMsg);
        }
    }
}
