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
import { MenuService } from '@core/services/menu.service';
import { Router } from '@angular/router';
import { HomePageTitleType } from '@shared/models/home-page.model';

@Component({
  selector: 'tb-quick-links',
  templateUrl: './quick-links.component.html',
  styleUrls: ['./quick-links.component.scss']
})
export class QuickLinksComponent implements OnInit {

  quickLinks$ = this.menuService.quickLinks();
  cardType = HomePageTitleType.QUICK_LINKS;

  constructor(private router: Router,
              private menuService: MenuService) {
  }

  ngOnInit(): void {
  }

  navigate(path: string) {
    if (path === 'rest-api') {
      const location = window.location.origin + '/swagger-ui.html';
      window.open(location, '_blank');
    } else {
      this.router.navigateByUrl(path);
    }
  }

}
