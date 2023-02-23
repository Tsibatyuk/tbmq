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
package org.thingsboard.mqtt.broker.util;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttVersion;
import org.thingsboard.mqtt.broker.session.ClientSessionCtx;
import org.thingsboard.mqtt.broker.session.DisconnectReasonType;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_CLIENT_IDENTIFIER_NOT_VALID;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED_5;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE_5;
import static org.thingsboard.mqtt.broker.util.MqttReasonCode.ADMINISTRATIVE_ACTION;
import static org.thingsboard.mqtt.broker.util.MqttReasonCode.FAILURE;
import static org.thingsboard.mqtt.broker.util.MqttReasonCode.KEEP_ALIVE_TIMEOUT;
import static org.thingsboard.mqtt.broker.util.MqttReasonCode.MALFORMED_PACKET;
import static org.thingsboard.mqtt.broker.util.MqttReasonCode.MESSAGE_RATE_TOO_HIGH;
import static org.thingsboard.mqtt.broker.util.MqttReasonCode.NOT_AUTHORIZED;
import static org.thingsboard.mqtt.broker.util.MqttReasonCode.PACKET_ID_NOT_FOUND;
import static org.thingsboard.mqtt.broker.util.MqttReasonCode.PACKET_TOO_LARGE;
import static org.thingsboard.mqtt.broker.util.MqttReasonCode.PROTOCOL_ERROR;
import static org.thingsboard.mqtt.broker.util.MqttReasonCode.QUOTA_EXCEEDED;
import static org.thingsboard.mqtt.broker.util.MqttReasonCode.SESSION_TAKEN_OVER;
import static org.thingsboard.mqtt.broker.util.MqttReasonCode.SUBSCRIPTION_ID_NOT_SUPPORTED;
import static org.thingsboard.mqtt.broker.util.MqttReasonCode.SUCCESS;
import static org.thingsboard.mqtt.broker.util.MqttReasonCode.TOPIC_NAME_INVALID;

public final class MqttReasonCodeResolver {

    public static MqttConnectReturnCode connectionRefusedNotAuthorized(ClientSessionCtx ctx) {
        return ctx.getMqttVersion() == MqttVersion.MQTT_5 ? CONNECTION_REFUSED_NOT_AUTHORIZED_5 : CONNECTION_REFUSED_NOT_AUTHORIZED;
    }

    public static MqttConnectReturnCode connectionRefusedClientIdNotValid(ClientSessionCtx ctx) {
        return ctx.getMqttVersion() == MqttVersion.MQTT_5 ? CONNECTION_REFUSED_CLIENT_IDENTIFIER_NOT_VALID : CONNECTION_REFUSED_IDENTIFIER_REJECTED;
    }

    public static MqttConnectReturnCode connectionRefusedServerUnavailable(ClientSessionCtx ctx) {
        return ctx.getMqttVersion() == MqttVersion.MQTT_5 ? CONNECTION_REFUSED_SERVER_UNAVAILABLE_5 : CONNECTION_REFUSED_SERVER_UNAVAILABLE;
    }

    public static MqttReasonCode packetIdNotFound(ClientSessionCtx ctx) {
        return ctx.getMqttVersion() == MqttVersion.MQTT_5 ? PACKET_ID_NOT_FOUND : null;
    }

    public static MqttReasonCode success(ClientSessionCtx ctx) {
        return ctx.getMqttVersion() == MqttVersion.MQTT_5 ? SUCCESS : null;
    }

    public static MqttReasonCode topicNameInvalid(ClientSessionCtx ctx) {
        return ctx.getMqttVersion() == MqttVersion.MQTT_5 ? TOPIC_NAME_INVALID : null;
    }

    public static MqttReasonCode notAuthorized(ClientSessionCtx ctx) {
        return ctx.getMqttVersion() == MqttVersion.MQTT_5 ? NOT_AUTHORIZED : null;
    }

    public static MqttReasonCode notAuthorizedSubscribe(ClientSessionCtx ctx) {
        return ctx.getMqttVersion() == MqttVersion.MQTT_5 ? NOT_AUTHORIZED : failure();
    }

    public static MqttReasonCode failure() {
        return FAILURE;
    }

    public static MqttReasonCode disconnect(DisconnectReasonType type) {
        switch (type) {
            case ON_CONFLICTING_SESSIONS:
                return SESSION_TAKEN_OVER;
            case ON_CHANNEL_CLOSED:
                return ADMINISTRATIVE_ACTION;
            case ON_RATE_LIMITS:
                return MESSAGE_RATE_TOO_HIGH;
            case ON_KEEP_ALIVE:
                return KEEP_ALIVE_TIMEOUT;
            case ON_MALFORMED_PACKET:
                return MALFORMED_PACKET;
            case ON_PROTOCOL_ERROR:
                return PROTOCOL_ERROR;
            case ON_QUOTA_EXCEEDED:
                return QUOTA_EXCEEDED;
            case ON_SUBSCRIPTION_ID_NOT_SUPPORTED:
                return SUBSCRIPTION_ID_NOT_SUPPORTED;
            case ON_PACKET_TOO_LARGE:
                return PACKET_TOO_LARGE;
            case ON_ERROR:
            default:
                return FAILURE;
        }
    }
}
