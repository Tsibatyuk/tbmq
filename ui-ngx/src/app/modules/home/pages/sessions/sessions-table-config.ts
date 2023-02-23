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

import {
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { TimePageLink } from '@shared/models/page/page-link';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { MqttClientSessionService } from "@core/http/mqtt-client-session.service";
import {
  SessionsDetailsDialogComponent,
  SessionsDetailsDialogData
} from "@home/pages/sessions/sessions-details-dialog.component";
import {
  connectionStateColor,
  connectionStateTranslationMap,
  DetailedClientSessionInfo
} from "@shared/models/session.model";
import { clientTypeTranslationMap } from "@shared/models/client.model";
import { HelpLinks } from "@shared/models/constants";

export class SessionsTableConfig extends EntityTableConfig<DetailedClientSessionInfo, TimePageLink> {

  constructor(private mqttClientSessionService: MqttClientSessionService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              public entityId: string = null) {
    super();
    this.loadDataOnInit = true;
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = true;
    this.addEnabled = true;
    this.entitiesDeleteEnabled = false;
    this.tableTitle = this.translate.instant('mqtt-client-session.type-sessions');
    this.entityTranslations = {
      noEntities: 'mqtt-client-session.no-session-text',
      search: 'mqtt-client-session.search'
    };

    this.entitiesFetchFunction = pageLink => this.fetchSessions(pageLink);
    this.handleRowClick = ($event, entity) => this.showSessionDetails($event, entity);

    this.addActionDescriptors.push(
      {
        name: this.translate.instant('help.goto-help-page'),
        icon: 'help',
        isEnabled: () => true,
        onAction: () => this.gotoHelpPage()
      }
    );

    this.columns.push(
      new EntityTableColumn<DetailedClientSessionInfo>('clientId', 'mqtt-client.client-id', '25%'),
      new EntityTableColumn<DetailedClientSessionInfo>('connectionState', 'mqtt-client-session.connect', '25%',
        (entity) => this.translate.instant(connectionStateTranslationMap.get(entity.connectionState)),
        (entity) => ({ color: connectionStateColor.get(entity.connectionState) })
      ),
      new EntityTableColumn<DetailedClientSessionInfo>('nodeId', 'mqtt-client-session.node-id', '25%'),
      new EntityTableColumn<DetailedClientSessionInfo>('clientType', 'mqtt-client.client-type', '25%',
        (entity) => this.translate.instant(clientTypeTranslationMap.get(entity.clientType))
      )
    );
  }

  private fetchSessions(pageLink: TimePageLink): Observable<PageData<DetailedClientSessionInfo>> {
    return this.mqttClientSessionService.getShortClientSessionInfos(pageLink);
  }

  private showSessionDetails($event: Event, entity: DetailedClientSessionInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.mqttClientSessionService.getDetailedClientSessionInfo(entity.clientId).subscribe(
      session => {
        this.dialog.open<SessionsDetailsDialogComponent, SessionsDetailsDialogData>(SessionsDetailsDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            session: session
          }
        }).afterClosed().subscribe(() =>{
          this.table.updateData();
        });
      }
    );
    return false;
  }

  private gotoHelpPage(): void {
    let helpUrl = HelpLinks.linksMap['sessions'];
    if (helpUrl) {
      window.open(helpUrl, '_blank');
    }
  }
}
