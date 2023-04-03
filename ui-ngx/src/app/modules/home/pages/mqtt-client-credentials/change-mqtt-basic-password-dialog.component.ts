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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { MqttClientCredentialsService } from '@core/http/mqtt-client-credentials.service';

export interface ChangeMqttBasicPasswordDialogData {
  credentialsId: string;
}

@Component({
  selector: 'tb-change-password-dialog',
  templateUrl: './change-mqtt-basic-password-dialog.component.html',
  styleUrls: ['./change-mqtt-basic-password-dialog.component.scss']
})
export class ChangeMqttBasicPasswordDialogComponent extends DialogComponent<ChangeMqttBasicPasswordDialogComponent,
  ChangeMqttBasicPasswordDialogData> implements OnInit {

  changePassword: FormGroup;
  credentialsId = this.data.credentialsId;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private translate: TranslateService,
              public dialogRef: MatDialogRef<ChangeMqttBasicPasswordDialogComponent>,
              @Inject(MAT_DIALOG_DATA) public data: ChangeMqttBasicPasswordDialogData,
              public fb: FormBuilder,
              private mqttClientCredentialsService: MqttClientCredentialsService) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.buildChangePasswordForm();
  }

  buildChangePasswordForm() {
    this.changePassword = this.fb.group({
      currentPassword: [null],
      newPassword: [null],
      newPassword2: [null]
    });
  }

  onChangePassword(): void {
    if (this.changePassword.get('newPassword').value !== this.changePassword.get('newPassword2').value) {
      this.store.dispatch(new ActionNotificationShow({
        message: this.translate.instant('login.passwords-mismatch-error'),
        type: 'error'
      }));
    } else {
      this.mqttClientCredentialsService.changePassword(
        this.changePassword.get('currentPassword').value,
        this.changePassword.get('newPassword').value,
        this.credentialsId).subscribe(
        (credentials) => {
          this.store.dispatch(new ActionNotificationShow({
            message: this.translate.instant('mqtt-client-credentials.password-changed'),
            type: 'success'
          }));
          this.dialogRef.close(credentials);
        });
    }
  }
}
