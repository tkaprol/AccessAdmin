/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alphabetbloc.chvsettings.data;

import android.app.AlarmManager;


public class Constants {
	
	//RECEIVERS
	public static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
	public static final String AIRPLANE_MODE = "android.intent.action.AIRPLANE_MODE";
	
	//INTENTS
	public static final String DEVICE_ADMIN_WORK = "device_admin_work";
	public static final String SAVED_DEVICE_ADMIN_WORK = "saved_device_admin_work";

	//SMS PREFERENCES
	public static final String SMS_LINE = "sms_line";
	public static final String SMS_MESSAGE = "sms_message";
	public static final String SAVED_SMS_LINE = "saved_sms_line";
	public static final String SAVED_SMS_MESSAGE = "saved_sms_message";
	public static final String SMS_SENT = "sms_sent"; 
	public static final String TOAST_MESSAGE = "toast";
	public static final String LAST_SENT_GPS_MESSAGE = "last_sent_gps_message";
	public static final String LAST_SENT_SIM_MESSAGE = "last_sent_sim_message";
	
	//SMS CODES
	public static final String SMS_CODE_LOCK = "-lock";
	public static final String SMS_CODE_GPS = "-gps";
	public static final String SMS_CODE_WIPE_DATA = "-wipe-reset";
	public static final String SMS_CODE_WIPE_ODK = "-wipe-data";
	public static final String SMS_CODE_HOLD = "-hold:"; 
	public static final String SMS_CODE_CANCEL_ALARM = "-cancel-alarm";
	public static final String SMS_CODE_RESET_PWD_SECRET = "create.new.password.and.sms.to.admin";
	public static final String SMS_CODE_RESET_PWD_DEFAULT = "-reset-pwd";
	public static final String SMS_CODE_RESET_ADMIN_ID = "create.new.admin.id.and.sms.to.admin";
	
	//DEVICE ADMIN ACTIONS: IN ORDER OF PRIORITY
	public static final int LOCK_SCREEN = 1;
	public static final int HOLD_DEVICE = 2;
	public static final int RESET_TO_DEFAULT_PWD = 3;
	public static final int SEND_SMS = 4;
	public static final int SEND_GPS = 5;
	public static final int LOCK_SECRET_PWD = 6;
	public static final int RESET_ADMIN_ID = 7;
	public static final int SEND_SIM = 8;
	public static final int WIPE_ODK_DATA = 9;
	public static final int WIPE_DATA = 10;
	public static final int CANCEL_ALARMS = 11;
	
	//ALARM PREFERENCES
	public static final String ALARM_LOCK_SCREEN = "ALARM_LOCK_SCREEN";
	public static final String ALARM_HOLD_DEVICE = "ALARM_HOLD_DEVICE";
	public static final String ALARM_RESET_TO_DEFAULT_PWD = "ALARM_RESET_TO_DEFAULT_PWD";
	public static final String ALARM_SEND_SMS = "ALARM_SEND_SMS";
	public static final String ALARM_SEND_GPS = "ALARM_SEND_GPS";
	public static final String ALARM_LOCK_SECRET_PWD = "ALARM_LOCK_SECRET_PWD";
	public static final String ALARM_RESET_ADMIN_ID = "ALARM_RESET_ADMIN_ID";
	public static final String ALARM_SEND_SIM = "ALARM_SEND_SIM";
	public static final String ALARM_WIPE_ODK_DATA = "ALARM_WIPE_ODK_DATA";
	public static final String ALARM_WIPE_DATA = "ALARM_WIPE_DATA";
	public static final String ALARM_CANCEL_ALARMS = "ALARM_CANCEL_ALARMS";
	
	
	public static final Long ALARM_INTERVAL_SHORT = AlarmManager.INTERVAL_FIFTEEN_MINUTES / 20; //45 seconds
	public static final Long ALARM_INTERVAL_MEDIUM = AlarmManager.INTERVAL_FIFTEEN_MINUTES / 3; // 5 minutes
	public static final Long ALARM_INTERVAL_LONG = AlarmManager.INTERVAL_HOUR * 2; // 2 hours
	
	//ENCRYPTED PREFERENCES:
	public static final String UNIQUE_DEVICE_ID = "unique_device_id";
	public static final String SMS_REPLY_LINE = "sms_reply_line";
	public static final String DEFAULT_SMS_REPLY_LINE = "0707028049";
	public static final String DEFAULT_PASSWORD = "default_password"; 
	public static final String SECRET_PASSWORD = "secret_password";
	public static final String ENCRYPTED_PREFS = "encrypted_prefs";
	public static final String ADMIN_PASSWORD = "admin_password";
	public static final String DEFAULT_ADMIN_PASSWORD = "***REMOVED***";
	
	public static final int RESULT_ENABLE = 1;
	
	//INSTALLATION AND ON BOOT
	public static final String NEW_INSTALL = "new_install";
	public static final String SIM_SERIAL = "sim_serial";
	public static final String SIM_LINE = "sim_line";
	public static final String LAST_SIM_CHANGE = "last_sim_change";
	public static final String SIM_CHANGE_COUNT = "sim_change_count";
	
	//ADMIN LOGINS
	public static final String LAST_ADMIN_PWD_ATTEMPT = "last_admin_pwd_attempt";
	public static final String ADMIN_PWD_COUNT = "last_pwd_count";
	
}

