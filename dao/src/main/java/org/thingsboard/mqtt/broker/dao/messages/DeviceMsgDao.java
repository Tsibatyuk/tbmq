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
package org.thingsboard.mqtt.broker.dao.messages;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.mqtt.broker.common.data.DevicePublishMsg;
import org.thingsboard.mqtt.broker.common.data.PersistedPacketType;

import java.util.List;

public interface DeviceMsgDao {
    void save(List<DevicePublishMsg> devicePublishMessages, boolean failOnConflict);

    List<DevicePublishMsg> findPersistedMessages(String clientId, int messageLimit);

    List<DevicePublishMsg> findPersistedMessagesBySerialNumber(String clientId, long fromSerialNumber, long toSerialNumber);

    void removePersistedMessages(String clientId);

    ListenableFuture<Void> removePersistedMessage(String clientId, int packetId);

    ListenableFuture<Void> updatePacketType(String clientId, int packetId, PersistedPacketType packetType);
}
