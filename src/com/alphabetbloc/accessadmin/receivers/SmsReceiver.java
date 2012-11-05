/**
 * 
 */
package com.alphabetbloc.accessadmin.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.alphabetbloc.accessadmin.data.Constants;
import com.alphabetbloc.accessadmin.data.EncryptedPreferences;
import com.alphabetbloc.accessadmin.data.Policy;
import com.alphabetbloc.accessadmin.services.DeviceAdminService;
import com.commonsware.cwac.wakeful.WakefulIntentService;

/**
 * Receives and parses SMS messages, sends the intent on to DeviceAdminService,
 * and blocks the SMS from being placed into the inbox.
 * 
 * @author Louis Fazen (louis.fazen@gmail.com)
 */

public class SmsReceiver extends BroadcastReceiver {

//	private static final String TAG = "SmsReceiver";

	private static String Imei;
	private static String lockDevice;
	private static String sendGPS;
	private static String wipeData;
	private static String wipeSdOdk;
	private static String resetPwdToDefault;
	private static String smsAdminId;
	private static String resetPwdToSmsPwd;
	private static String lockSecretPwd;
	private static String resetAdminId;
	private static String holdScreen;
	private static String stopHoldScreen;
	private static String cancelAlarm;
	private static String editAccessMrsPreference;
	private static String mSmsMessage = null;
	private Context mContext;
	private SmsMessage[] mSms;
	private int mExtra;

	public SmsReceiver() {
		// Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		Policy policy = new Policy(context);
		if (intent.getAction().equals(Constants.SMS_RECEIVED) && policy.isAdminActive()) {
			Bundle bundle = intent.getExtras();

			if (bundle != null) {
				Object[] pdus = (Object[]) bundle.get("pdus");
				mSms = new SmsMessage[pdus.length];
				for (int i = 0; i < pdus.length; i++) {
					mSms[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
				}

				if (mSms.length > -1) {
					readSMS();
				}

			}
		} else if (intent.getAction().equals(Constants.WIPE_DATA_COMPLETE)) {
			Intent i = new Intent(mContext, DeviceAdminService.class);
			i.putExtra(Constants.DEVICE_ADMIN_WORK, Constants.FACTORY_RESET);
			WakefulIntentService.sendWakefulWork(mContext, i);
		}
	}

	// TODO! does this receiver need to be a wakelock receover, or do all
	// receivers have a wakelock for the duration of their onReceive?
	private void readSMS() {
		if (Imei == null)
			createSmsStrings();
		if (matchingSmsString()) {
			abortBroadcast();
			Intent i = new Intent(mContext, DeviceAdminService.class);
			i.putExtra(Constants.DEVICE_ADMIN_WORK, mExtra);
			if (mSmsMessage != null)
				i.putExtra(Constants.SMS_MESSAGE, mSmsMessage);
			WakefulIntentService.sendWakefulWork(mContext, i);
		}

	}

	private void createSmsStrings() {
		final SharedPreferences prefs = new EncryptedPreferences(mContext, mContext.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		smsAdminId = prefs.getString(Constants.UNIQUE_DEVICE_ID, null);

		// REQUIRE smsAdminId:
		lockDevice = smsAdminId + Constants.SMS_CODE_LOCK;
		sendGPS = smsAdminId + Constants.SMS_CODE_GPS;
		wipeData = smsAdminId + Constants.SMS_CODE_WIPE_DATA;
		wipeSdOdk = smsAdminId + Constants.SMS_CODE_WIPE_ODK;
		holdScreen = smsAdminId + Constants.SMS_CODE_HOLD;
		stopHoldScreen = smsAdminId + Constants.SMS_CODE_STOP_HOLD;
		cancelAlarm = smsAdminId + Constants.SMS_CODE_CANCEL_ALARM;
		resetPwdToDefault = smsAdminId + Constants.SMS_CODE_RESET_PWD_DEFAULT;
		resetPwdToSmsPwd = smsAdminId + Constants.SMS_CODE_RESET_PWD_TO_SMS_PWD;
		editAccessMrsPreference = smsAdminId + Constants.SMS_CODE_EDIT_ACCESS_MRS_PREF;
		
		// DO NOT REQUIRE smsAdminId:
		lockSecretPwd = Constants.SMS_CODE_RESET_PWD_SECRET;
		resetAdminId = Constants.SMS_CODE_RESET_ADMIN_ID;

	}

	private boolean matchingSmsString() {

		if (mSms[0].getMessageBody().equals(lockDevice)) {
			mExtra = Constants.LOCK_SCREEN;
			return true;
		} else if (mSms[0].getMessageBody().equals(sendGPS)) {
			mExtra = Constants.SEND_GPS;
			return true;
		} else if (mSms[0].getMessageBody().equals(wipeData)) {
			mExtra = Constants.WIPE_DATA;
			return true;
		} else if (mSms[0].getMessageBody().equals(wipeSdOdk)) {
			mExtra = Constants.WIPE_ODK_DATA;
			return true;
		} else if (mSms[0].getMessageBody().equals(lockSecretPwd)) {
			mExtra = Constants.LOCK_SECRET_PWD;
			return true;
		} else if (mSms[0].getMessageBody().equals(resetAdminId)) {
			mExtra = Constants.RESET_ADMIN_ID;
			return true;
		} else if (mSms[0].getMessageBody().equals(cancelAlarm)) {
			mExtra = Constants.CANCEL_ALARMS;
			return true;
		} else if (mSms[0].getMessageBody().equals(stopHoldScreen)) {
			mExtra = Constants.STOP_HOLD_DEVICE;
			return true;
		} else if (mSms[0].getMessageBody().equals(resetPwdToDefault)) {
			mExtra = Constants.RESET_TO_DEFAULT_PWD;
			return true;
		} else if (mSms[0].getMessageBody().contains(editAccessMrsPreference)) {
			mExtra = Constants.EDIT_ACCESS_MRS_PREF;
			int message = mSms[0].getMessageBody().indexOf(":");
			mSmsMessage = mSms[0].getMessageBody().substring(message + 1);
			return true;
		} else if (mSms[0].getMessageBody().contains(resetPwdToSmsPwd)) {
			mExtra = Constants.RESET_PWD_TO_SMS_PWD;
			int message = mSms[0].getMessageBody().indexOf(":");
			mSmsMessage = mSms[0].getMessageBody().substring(message + 1);
			return true;
		} else if (mSms[0].getMessageBody().contains(holdScreen)) {
			mExtra = Constants.HOLD_DEVICE;
			int message = mSms[0].getMessageBody().indexOf(":");
			mSmsMessage = mSms[0].getMessageBody().substring(message + 1);
			return true;
		} else {
			return false;
		}
	}
}