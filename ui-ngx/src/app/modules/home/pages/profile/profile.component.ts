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

import {Component, OnInit} from '@angular/core';
import {AdminService} from '@core/http/admin.service';
import {AuthUser, User} from '@shared/models/user.model';
import {PageComponent} from '@shared/components/page.component';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {HasConfirmForm} from '@core/guards/confirm-on-exit.guard';
import {ActionAuthUpdateUserDetails} from '@core/auth/auth.actions';
import {environment as env} from '@env/environment';
import {TranslateService} from '@ngx-translate/core';
import {ActionSettingsChangeLanguage} from '@core/settings/settings.actions';
import {ChangePasswordDialogComponent} from '@modules/home/pages/profile/change-password-dialog.component';
import {MatDialog} from '@angular/material/dialog';
import {DialogService} from '@core/services/dialog.service';
import {ActivatedRoute} from '@angular/router';
import {getCurrentAuthUser} from '@core/auth/auth.selectors';
import {AuthService} from "@core/http/auth.service";

@Component({
  selector: 'tb-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent extends PageComponent implements OnInit, HasConfirmForm {

  profile: FormGroup;
  user: User;
  languageList = env.supportedLangs;
  private readonly authUser: AuthUser;

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private adminService: AdminService,
              private authService: AuthService,
              private translate: TranslateService,
              public dialog: MatDialog,
              public dialogService: DialogService,
              public fb: FormBuilder) {
    super(store);
    this.authUser = getCurrentAuthUser(this.store);
  }

  ngOnInit() {
    this.buildProfileForm();
    this.userLoaded(this.route.snapshot.data.user);
  }

  buildProfileForm() {
    this.profile = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      firstName: [''],
      lastName: [''],
      language: ['']
    });
  }

  save(): void {
    this.user = {...this.user, ...this.profile.value};
    if (!this.user.additionalInfo) {
      this.user.additionalInfo = {};
    }
    this.user.additionalInfo.lang = this.profile.get('language').value;
    this.adminService.saveAdminUser(this.user).subscribe(
      (user) => {
        this.userLoaded(user);
        this.store.dispatch(new ActionAuthUpdateUserDetails({ userDetails: {
            additionalInfo: {...user.additionalInfo},
            authority: user.authority,
            createdTime: user.createdTime,
            email: user.email,
            firstName: user.firstName,
            id: user.id,
            lastName: user.lastName,
          } }));
        this.store.dispatch(new ActionSettingsChangeLanguage({ userLang: user.additionalInfo.lang }));
      }
    );
  }

  changePassword(): void {
    this.dialog.open(ChangePasswordDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
    });
  }

  userLoaded(user: User) {
    this.user = user;
    this.profile.reset(user);
    let lang;
    if (user.additionalInfo) {
      if (user.additionalInfo.lang) {
        lang = user.additionalInfo.lang;
      }
    }
    if (!lang) {
      lang = this.translate.currentLang;
    }
    this.profile.get('language').setValue(lang);
  }

  confirmForm(): FormGroup {
    return this.profile;
  }
}
