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
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import {
  RetainedMessagesTableConfigResolver
} from "@home/pages/retained-messages/retained-messages-table-config-resolver.service";

const routes: Routes = [
  {
    path: 'retained-messages',
    component: EntitiesTableComponent,
    data: {
      auth: [Authority.SYS_ADMIN],
      title: 'retained-message.retained-messages',
      breadcrumb: {
        label: 'retained-message.retained-messages',
        icon: 'mdi:archive-outline',
        isMdiIcon: true
      }
    },
    resolve: {
      entitiesTableConfig: RetainedMessagesTableConfigResolver
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    RetainedMessagesTableConfigResolver
  ]
})

export class RetainedMessagesRoutingModule {
}
