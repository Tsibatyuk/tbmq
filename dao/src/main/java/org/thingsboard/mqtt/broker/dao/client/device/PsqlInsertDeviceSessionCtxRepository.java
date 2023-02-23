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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.mqtt.broker.dao.model.DeviceSessionCtxEntity;
import org.thingsboard.mqtt.broker.dao.util.PsqlDao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.thingsboard.mqtt.broker.dao.model.ModelConstants.DEVICE_SESSION_CTX_CLIENT_ID_PROPERTY;
import static org.thingsboard.mqtt.broker.dao.model.ModelConstants.DEVICE_SESSION_CTX_COLUMN_FAMILY_NAME;
import static org.thingsboard.mqtt.broker.dao.model.ModelConstants.DEVICE_SESSION_CTX_LAST_PACKET_ID_PROPERTY;
import static org.thingsboard.mqtt.broker.dao.model.ModelConstants.DEVICE_SESSION_CTX_LAST_SERIAL_NUMBER_PROPERTY;
import static org.thingsboard.mqtt.broker.dao.model.ModelConstants.DEVICE_SESSION_CTX_LAST_UPDATED_PROPERTY;

@PsqlDao
@Repository
@Transactional
public class PsqlInsertDeviceSessionCtxRepository implements InsertDeviceSessionCtxRepository {
    private static final String INSERT_OR_UPDATE = "INSERT INTO " + DEVICE_SESSION_CTX_COLUMN_FAMILY_NAME + " (" +
            DEVICE_SESSION_CTX_CLIENT_ID_PROPERTY + ", " +
            DEVICE_SESSION_CTX_LAST_UPDATED_PROPERTY + ", " +
            DEVICE_SESSION_CTX_LAST_SERIAL_NUMBER_PROPERTY + ", " +
            DEVICE_SESSION_CTX_LAST_PACKET_ID_PROPERTY + ") " +
            "VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (" + DEVICE_SESSION_CTX_CLIENT_ID_PROPERTY + ") " +
            "DO UPDATE SET " + DEVICE_SESSION_CTX_LAST_SERIAL_NUMBER_PROPERTY + " = ?, " +
            DEVICE_SESSION_CTX_LAST_PACKET_ID_PROPERTY + " = ?, " +
            DEVICE_SESSION_CTX_LAST_UPDATED_PROPERTY + " = ?;";


    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void saveOrUpdate(List<DeviceSessionCtxEntity> entities) {
        jdbcTemplate.batchUpdate(INSERT_OR_UPDATE, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                DeviceSessionCtxEntity deviceSessionCtxEntity = entities.get(i);
                ps.setString(1, deviceSessionCtxEntity.getClientId());
                ps.setLong(2, deviceSessionCtxEntity.getLastUpdatedTime());
                ps.setLong(3, deviceSessionCtxEntity.getLastSerialNumber());
                ps.setInt(4, deviceSessionCtxEntity.getLastPacketId());
                ps.setLong(5, deviceSessionCtxEntity.getLastSerialNumber());
                ps.setInt(6, deviceSessionCtxEntity.getLastPacketId());
                ps.setLong(7, deviceSessionCtxEntity.getLastUpdatedTime());
            }

            @Override
            public int getBatchSize() {
                return entities.size();
            }
        });
    }
}
