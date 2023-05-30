///
/// Copyright © 2016-2023 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { NgModule } from '@angular/core';

import { MODULES_MAP } from '@shared/public-api';
import { modulesMap } from '../../common/modules-map';
import { MqttClientCredentialsModule } from './mqtt-client-credentials/mqtt-client-credentials.module';
import { ProfileModule } from './profile/profile.module';
import { MailServerModule } from '@home/pages/mail-server/mail-server.module';
import { SessionsModule } from '@home/pages/sessions/sessions.module';
import { AdminsModule } from '@home/pages/admins/admins.module';
import { SharedSubscriptionsModule } from '@home/pages/shared-subscriptions/shared-subscriptions.module';
import { HomeOverviewModule } from '@home/pages/home-overview/home-overview.module';
import { RetainedMessagesModule } from '@home/pages/retained-messages/retained-messages.module';
import { MonitoringModule } from '@home/pages/monitoring/monitoring.module';

@NgModule({
  exports: [
    MailServerModule,
    ProfileModule,
    MqttClientCredentialsModule,
    SessionsModule,
    AdminsModule,
    SharedSubscriptionsModule,
    HomeOverviewModule,
    RetainedMessagesModule,
    MonitoringModule
  ],
  providers: [
    {
      provide: MODULES_MAP,
      useValue: modulesMap
    }
  ]
})
export class HomePagesModule {
}
