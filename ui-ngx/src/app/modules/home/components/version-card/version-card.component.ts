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

import { Component, OnInit } from '@angular/core';
import { Observable, of } from 'rxjs';
import { HomePageTitleType } from '@shared/models/home-page.model';

export interface VersionData {
  currentVersion: string;
  latestVersion: string;
  updateAvailable: boolean;
}

@Component({
  selector: 'tb-version-card',
  templateUrl: './version-card.component.html',
  styleUrls: ['./version-card.component.scss']
})
export class VersionCardComponent implements OnInit {

  cardType = HomePageTitleType.VERSION;
  versionData: VersionData;

  constructor() {
  }

  ngOnInit(): void {
    this.getData().subscribe(versionData => this.versionData = versionData);
  }

  getData(): Observable<VersionData> {
    return of({
      currentVersion: '1.0',
      latestVersion: '1.1',
      updateAvailable: false
    });
  }
}
