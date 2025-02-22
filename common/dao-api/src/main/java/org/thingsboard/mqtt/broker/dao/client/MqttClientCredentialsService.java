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
package org.thingsboard.mqtt.broker.dao.client;

import org.thingsboard.mqtt.broker.common.data.dto.ClientCredentialsInfoDto;
import org.thingsboard.mqtt.broker.common.data.dto.ShortMqttClientCredentials;
import org.thingsboard.mqtt.broker.common.data.page.PageData;
import org.thingsboard.mqtt.broker.common.data.page.PageLink;
import org.thingsboard.mqtt.broker.common.data.security.MqttClientCredentials;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MqttClientCredentialsService {

    MqttClientCredentials saveCredentials(MqttClientCredentials mqttClientCredentials);

    void deleteCredentials(UUID id);

    List<MqttClientCredentials> findMatchingCredentials(List<String> credentialIds);

    PageData<ShortMqttClientCredentials> getCredentials(PageLink pageLink);

    Optional<MqttClientCredentials> getCredentialsById(UUID id);

    ClientCredentialsInfoDto getClientCredentialsInfo();
}
