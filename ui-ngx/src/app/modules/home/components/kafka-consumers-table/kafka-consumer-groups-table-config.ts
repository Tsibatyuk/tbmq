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

import { EntityTableColumn, EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { TimePageLink } from '@shared/models/page/page-link';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { KafkaConsumerGroup } from '@shared/models/kafka.model';
import { KafkaService } from '@core/http/kafka.service';

export class KafkaConsumerGroupsTableConfig extends EntityTableConfig<KafkaConsumerGroup, TimePageLink> {

  constructor(private kafkaService: KafkaService,
              private translate: TranslateService,
              public entityId: string = null) {
    super();
    this.loadDataOnInit = true;
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = true;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;
    this.entityTranslations = {
      noEntities: 'kafka.no-kafka-consumer-group-text',
      search: 'kafka.consumer-groups-search'
    };

    this.entitiesFetchFunction = pageLink => this.fetchSessions(pageLink);

    this.columns.push(
      new EntityTableColumn<KafkaConsumerGroup>('groupId', 'kafka.id', '70%'),
      new EntityTableColumn<KafkaConsumerGroup>('state', 'kafka.state', '10%'),
      new EntityTableColumn<KafkaConsumerGroup>('members', 'kafka.members', '10%'),
      new EntityTableColumn<KafkaConsumerGroup>('lag', 'kafka.lag', '10%', entity => entity.lag)
    );
  }

  private fetchSessions(pageLink: TimePageLink): Observable<PageData<KafkaConsumerGroup>> {
    return this.kafkaService.getKafkaConsumerGroups(pageLink);
  }
}
