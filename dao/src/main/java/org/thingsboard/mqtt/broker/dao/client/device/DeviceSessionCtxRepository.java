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
package org.thingsboard.mqtt.broker.dao.client.device;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.thingsboard.mqtt.broker.dao.model.DeviceSessionCtxEntity;

import java.util.Collection;

@Repository
public interface DeviceSessionCtxRepository extends PagingAndSortingRepository<DeviceSessionCtxEntity, String> {
    Collection<DeviceSessionCtxEntity> findAllByClientIdIn(Collection<String> clientIds);

    Page<DeviceSessionCtxEntity> findAll(Pageable pageable);
}
