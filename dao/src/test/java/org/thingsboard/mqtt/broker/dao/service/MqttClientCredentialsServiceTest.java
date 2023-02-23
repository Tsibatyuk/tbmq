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
package org.thingsboard.mqtt.broker.dao.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.thingsboard.mqtt.broker.cache.CacheConstants;
import org.thingsboard.mqtt.broker.common.data.client.credentials.BasicMqttCredentials;
import org.thingsboard.mqtt.broker.common.data.dto.ShortMqttClientCredentials;
import org.thingsboard.mqtt.broker.common.data.page.PageData;
import org.thingsboard.mqtt.broker.common.data.page.PageLink;
import org.thingsboard.mqtt.broker.common.data.security.ClientCredentialsType;
import org.thingsboard.mqtt.broker.common.data.security.MqttClientCredentials;
import org.thingsboard.mqtt.broker.common.util.JacksonUtil;
import org.thingsboard.mqtt.broker.common.util.MqttClientCredentialsUtil;
import org.thingsboard.mqtt.broker.dao.DaoSqlTest;
import org.thingsboard.mqtt.broker.dao.client.MqttClientCredentialsService;
import org.thingsboard.mqtt.broker.dao.exception.DataValidationException;
import org.thingsboard.mqtt.broker.dao.util.protocol.ProtocolUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@DaoSqlTest
public class MqttClientCredentialsServiceTest extends AbstractServiceTest {

    private final IdComparator<ShortMqttClientCredentials> idComparator = new IdComparator<>();

    @Autowired
    private MqttClientCredentialsService mqttClientCredentialsService;
    @Autowired
    private CacheManager cacheManager;

    Cache cache;

    @Before
    public void setUp() {
        cache = cacheManager.getCache(CacheConstants.MQTT_CLIENT_CREDENTIALS_CACHE);
    }

    private void checkCacheNonNullAndEvict(String credentialsId) {
        Objects.requireNonNull(cache, "Cache manager is null").evict(credentialsId);
    }

    @Test
    public void testSaveCredentialsCheckCache() throws JsonProcessingException {
        String credentialsId = ProtocolUtil.clientIdCredentialsId("cacheClient");

        checkCacheNonNullAndEvict(credentialsId);
        mqttClientCredentialsService.saveCredentials(
                validMqttClientCredentials("cachedName", "cacheClient", null, null)
        );

        List<MqttClientCredentials> credentials = mqttClientCredentialsService.findMatchingCredentials(List.of(credentialsId));
        Assert.assertNotNull(credentials);
        Assert.assertFalse(credentials.isEmpty());
        MqttClientCredentials clientCredentials = cache.get(credentialsId, MqttClientCredentials.class);
        Assert.assertNotNull(clientCredentials);
        Assert.assertEquals(clientCredentials, credentials.get(0));

        mqttClientCredentialsService.deleteCredentials(credentials.get(0).getId());
        Assert.assertNull(cache.get(credentialsId, MqttClientCredentials.class));
    }

    @Test
    public void testSaveCredentialsCheckCacheAfterAnotherSave() throws JsonProcessingException {
        String credentialsId = ProtocolUtil.clientIdCredentialsId("cacheClient");

        checkCacheNonNullAndEvict(credentialsId);
        mqttClientCredentialsService.saveCredentials(
                validMqttClientCredentials("cachedName", "cacheClient", null, null)
        );

        List<MqttClientCredentials> credentials = mqttClientCredentialsService.findMatchingCredentials(List.of(credentialsId));
        Assert.assertNotNull(credentials);
        Assert.assertFalse(credentials.isEmpty());
        Assert.assertNotNull(cache.get(credentialsId, MqttClientCredentials.class));

        MqttClientCredentials clientCredentials = credentials.get(0);
        clientCredentials.setName("newName");
        MqttClientCredentials savedCredentials = mqttClientCredentialsService.saveCredentials(clientCredentials);
        Assert.assertNull(cache.get(credentialsId, MqttClientCredentials.class));

        mqttClientCredentialsService.deleteCredentials(savedCredentials.getId());
    }

    @Test
    public void testSaveTwoCredentialsCheckCacheAfterFetchOneOfThem() throws JsonProcessingException {
        String credentialsId1 = ProtocolUtil.clientIdCredentialsId("cacheClient1");
        String credentialsId2 = ProtocolUtil.clientIdCredentialsId("cacheClient2");

        checkCacheNonNullAndEvict(credentialsId1);
        checkCacheNonNullAndEvict(credentialsId2);
        mqttClientCredentialsService.saveCredentials(
                validMqttClientCredentials("cachedName1", "cacheClient1", null, null)
        );
        mqttClientCredentialsService.saveCredentials(
                validMqttClientCredentials("cachedName2", "cacheClient2", null, null)
        );

        List<MqttClientCredentials> credentials = mqttClientCredentialsService.findMatchingCredentials(List.of(credentialsId1));
        Assert.assertNotNull(credentials);
        Assert.assertFalse(credentials.isEmpty());
        Assert.assertNotNull(cache.get(credentialsId1, MqttClientCredentials.class));
        Assert.assertNull(cache.get(credentialsId2, MqttClientCredentials.class));

        credentials = mqttClientCredentialsService.findMatchingCredentials(List.of(credentialsId1, credentialsId2));
        Assert.assertNotNull(cache.get(credentialsId2, MqttClientCredentials.class));

        mqttClientCredentialsService.deleteCredentials(credentials.get(0).getId());
        mqttClientCredentialsService.deleteCredentials(credentials.get(1).getId());
    }

    @Test
    public void testCreateValidCredentials() throws JsonProcessingException {
        mqttClientCredentialsService.saveCredentials(validMqttClientCredentials("test", "client", "user", null));
    }

    @Test(expected = DataValidationException.class)
    public void testCreateCredentialsWithEmptyName() throws JsonProcessingException {
        mqttClientCredentialsService.saveCredentials(validMqttClientCredentials(null, "client", "user", null));
    }

    @Test(expected = DataValidationException.class)
    public void testCreateDeviceCredentialsWithEmptyCredentialsType() {
        MqttClientCredentials clientCredentials = new MqttClientCredentials();
        clientCredentials.setName("TestClient");
        mqttClientCredentialsService.saveCredentials(clientCredentials);
    }

    @Test(expected = DataValidationException.class)
    public void testCreateNoClientAndUsername() {
        MqttClientCredentials clientCredentials = new MqttClientCredentials();
        clientCredentials.setCredentialsType(ClientCredentialsType.MQTT_BASIC);
        mqttClientCredentialsService.saveCredentials(clientCredentials);
    }

    @Test(expected = DataValidationException.class)
    public void testCreateNotValidCredentialsValue() {
        MqttClientCredentials clientCredentials = new MqttClientCredentials();
        clientCredentials.setName("TestClient");
        clientCredentials.setCredentialsType(ClientCredentialsType.MQTT_BASIC);
        clientCredentials.setCredentialsValue("NOT_VALID");
        mqttClientCredentialsService.saveCredentials(clientCredentials);
    }

    @Test(expected = DataValidationException.class)
    public void testCreateNotValidCredentialsValue_WrongAuthPattern() {
        MqttClientCredentials clientCredentials = new MqttClientCredentials();
        clientCredentials.setName("TestClient");
        clientCredentials.setCredentialsType(ClientCredentialsType.MQTT_BASIC);
        BasicMqttCredentials wrongPatternBasicCred = BasicMqttCredentials.newInstance("test", "test", "test", List.of("(not_closed"));
        clientCredentials.setCredentialsValue(JacksonUtil.toString(wrongPatternBasicCred));
        mqttClientCredentialsService.saveCredentials(clientCredentials);
    }


    @Test(expected = DataValidationException.class)
    public void testCreateDuplicateCredentials() throws JsonProcessingException {
        MqttClientCredentials clientCredentials = mqttClientCredentialsService.saveCredentials(validMqttClientCredentials("test", "client", "user", null));
        try {
            mqttClientCredentialsService.saveCredentials(validMqttClientCredentials("test", "client", "user", "password"));
        } finally {
            mqttClientCredentialsService.deleteCredentials(clientCredentials.getId());
        }
    }

    @Test
    public void testFindMatchingMixed() throws JsonProcessingException {
        MqttClientCredentials client1Credentials, client2Credentials, client3Credentials;
        client1Credentials = mqttClientCredentialsService.saveCredentials(
                validMqttClientCredentials("test", "client1", "test1", "password1"));
        client2Credentials = mqttClientCredentialsService.saveCredentials(
                validMqttClientCredentials("test", "client2", "test1", null));
        client3Credentials = mqttClientCredentialsService.saveCredentials(
                validMqttClientCredentials("test", "client1", "test2", null));

        Assert.assertEquals(
                Collections.singletonList(client1Credentials),
                mqttClientCredentialsService.findMatchingCredentials(Collections.singletonList(
                        ProtocolUtil.mixedCredentialsId("test1", "client1")
                )));

        mqttClientCredentialsService.deleteCredentials(client1Credentials.getId());
        mqttClientCredentialsService.deleteCredentials(client2Credentials.getId());
        mqttClientCredentialsService.deleteCredentials(client3Credentials.getId());
    }

    @Test
    public void testFindMatchingByUserName() throws JsonProcessingException {
        MqttClientCredentials client1Credentials, client2Credentials;
        client1Credentials = mqttClientCredentialsService.saveCredentials(
                validMqttClientCredentials("test", null, "user1", null));
        client2Credentials = mqttClientCredentialsService.saveCredentials(
                validMqttClientCredentials("test", null, "user2", null));

        Assert.assertEquals(
                Collections.singletonList(client1Credentials),
                mqttClientCredentialsService.findMatchingCredentials(Collections.singletonList(
                        ProtocolUtil.usernameCredentialsId("user1")
                )));

        mqttClientCredentialsService.deleteCredentials(client1Credentials.getId());
        mqttClientCredentialsService.deleteCredentials(client2Credentials.getId());
    }

    @Test
    public void testFindMatchingByClientId() throws JsonProcessingException {
        MqttClientCredentials client1Credentials, client2Credentials;
        client1Credentials = mqttClientCredentialsService.saveCredentials(
                validMqttClientCredentials("test", "client1", null, null));
        client2Credentials = mqttClientCredentialsService.saveCredentials(
                validMqttClientCredentials("test", "client2", null, null));

        Assert.assertEquals(
                Collections.singletonList(client1Credentials),
                mqttClientCredentialsService.findMatchingCredentials(Collections.singletonList(
                        ProtocolUtil.clientIdCredentialsId("client1")
                )));

        mqttClientCredentialsService.deleteCredentials(client1Credentials.getId());
        mqttClientCredentialsService.deleteCredentials(client2Credentials.getId());
    }

    @Test
    public void testFindMqttClientCredentials() throws JsonProcessingException {
        List<ShortMqttClientCredentials> mqttClientCredentialsList = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            MqttClientCredentials savedCredentials = mqttClientCredentialsService.saveCredentials(
                    validMqttClientCredentials(
                            "Credentials" + i,
                            "clientId" + i,
                            "username" + i,
                            "password" + i));
            mqttClientCredentialsList.add(MqttClientCredentialsUtil.toShortMqttClientCredentials(savedCredentials));
        }

        List<ShortMqttClientCredentials> loadedMqttClientCredentialsList = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<ShortMqttClientCredentials> pageData;
        do {
            pageData = mqttClientCredentialsService.getCredentials(pageLink);
            loadedMqttClientCredentialsList.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        mqttClientCredentialsList.sort(idComparator);
        loadedMqttClientCredentialsList.sort(idComparator);

        Assert.assertEquals(mqttClientCredentialsList, loadedMqttClientCredentialsList);

        loadedMqttClientCredentialsList.forEach(smcc ->
                mqttClientCredentialsService.deleteCredentials(smcc.getId()));

        pageLink = new PageLink(33);
        pageData = mqttClientCredentialsService.getCredentials(pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    private MqttClientCredentials validMqttClientCredentials(String credentialsName, String clientId, String username, String password) throws JsonProcessingException {
        MqttClientCredentials clientCredentials = new MqttClientCredentials();
        clientCredentials.setName(credentialsName);
        clientCredentials.setCredentialsType(ClientCredentialsType.MQTT_BASIC);
        BasicMqttCredentials basicMqttCredentials = BasicMqttCredentials.newInstance(clientId, username, password, null);
        clientCredentials.setCredentialsValue(mapper.writeValueAsString(basicMqttCredentials));
        return clientCredentials;
    }
}
