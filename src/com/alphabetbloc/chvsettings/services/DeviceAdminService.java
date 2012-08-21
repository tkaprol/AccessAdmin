package com.alphabetbloc.chvsettings.services;

import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.alphabetbloc.chvsettings.R;
import com.alphabetbloc.chvsettings.activities.MessageHoldActivity;
import com.alphabetbloc.chvsettings.data.Constants;
import com.alphabetbloc.chvsettings.data.EncryptedPreferences;
import com.alphabetbloc.chvsettings.data.Policy;
import com.alphabetbloc.chvsettings.data.StringGenerator;
import com.alphabetbloc.chvsettings.receivers.DeviceAdmin;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.commonsware.cwac.wakeful.WakelockWorkListener;
import com.commonsware.cwac.wakeful.WakelockWorkReceiver;

/**
 * Service used to conduct all device admin work. The service holds a wakelock
 * while doing device admin work by extending @CommonsWare's
 * WakefulIntentService. The service takes care of scheduling an alarm to
 * continue the device admin work even if there is a reboot or if the process is
 * killed. The alarm will continue to wake the device at the time specified in
 * the WakelockListener, and try to complete the work through a wakelock on
 * boot. The service will then cancel its own alarms on successful completion of
 * its intent.
 * 
 * <p>
 * DEVICE_ADMIN_WORK type extras: <br>
 * 1. SEND_SMS: sends SMS with a repeat alarm until sent
 * 2. SEND_GPS: Sending an SMS with GPS coordinates<br>
 * 3. SEND_SIM: Sending SMS with new serial and line when SIM is changed<br>
 * 4. LOCK_SCREEN: Locks the Screen<br>
 * 5. WIPE_ODK_DATA: Wiping the patient sensitive data<br>
 * 6. WIPE_DATA: Wiping the entire device to factory reset (will allow user to
 * setup new device)<br>
 * 7. RESET_TO_DEFAULT_PWD: Resetting password to a default, depends on what the password quality is<br>
 * 8. LOCK_SECRET_PWD: Resetting password to a random string (so as to
 * permanently lock device until reset password to default)<br>
 * 9. HOLD_DEVICE: Starting MessageHoldActivity to send message to user before
 * e.g. locking the device.<br>
 * 10. CANCEL_ALARMS: Reset all alarms<br>
 * <p>
 * To use this service and ensure a wakelock, do not call directly, but call
 * through WakefulIntentService. First, create an intent for
 * DeviceAdminService.class and add intent extras to resolve the action for
 * DeviceAdminService. Then pass the intent through the sendWakefulWork method:
 * <br>
 * <br>
 * <b>Example:</b>
 * <p>
 * Intent i = new Intent(mContext, DeviceAdminService.class);<br>
 * i.putExtra(Constants.DEVICE_ADMIN_WORK, deviceAdminAction); <br>
 * WakefulIntentService.sendWakefulWork(mContext, i);<br>
 * </p>
 * 
 * @author Louis.Fazen@gmail.com
 * 
 */

public class DeviceAdminService extends WakefulIntentService {

	DevicePolicyManager mDPM;
	ComponentName mDeviceAdmin;
	SharedPreferences mPrefs;
	Policy mPolicy;

	private Context mContext;
	private static final String TAG = "DeviceAdminService";

	public DeviceAdminService() {
		super("AppService");

	}

	@Override
	protected void doWakefulWork(Intent intent) {
		mContext = this;
		mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		mDeviceAdmin = new ComponentName(DeviceAdminService.this, DeviceAdmin.class);
		mPolicy = new Policy(mContext);

		// May be new Intent or called from BOOT... so resolve intent:
		mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String smsLine;
		String smsMessage;
		int smsIntent;

		if (intent.getIntExtra(Constants.DEVICE_ADMIN_WORK, 0) != 0) {
			// we have a new intent from SMS
			smsIntent = intent.getIntExtra(Constants.DEVICE_ADMIN_WORK, 0);
			smsLine = intent.getStringExtra(Constants.SMS_LINE);
			smsMessage = intent.getStringExtra(Constants.SMS_MESSAGE);
			if (smsLine == null)
				smsLine = "";
			if (smsMessage == null)
				smsMessage = "";

			// if new intent is a priority intent, set up alarms in case process
			// is killed...
			int standingIntent = mPrefs.getInt(Constants.SAVED_DEVICE_ADMIN_WORK, 0);
			if (smsIntent >= standingIntent) {
				// kill any old alarms so only 1 active device admin process
				// (all alarms should have same simple pi)
				cancelAlarms(mContext);

				// schedule new alarm to continue after kill or reboot
				mPrefs.edit().putInt(Constants.SAVED_DEVICE_ADMIN_WORK, smsIntent).commit();
				mPrefs.edit().putString(Constants.SAVED_SMS_LINE, smsLine).commit();
				mPrefs.edit().putString(Constants.SAVED_SMS_MESSAGE, smsMessage).commit();
				scheduleAlarms(new WakelockWorkListener(), mContext, true);
			}

		} else {
			// Service called by alarm or boot, so recreate intent from saved
			// (after boot or kill, intent extras would be lost)
			// do not reset alarms
			smsIntent = mPrefs.getInt(Constants.SAVED_DEVICE_ADMIN_WORK, 0);
			smsLine = mPrefs.getString(Constants.SMS_LINE, "");
			smsMessage = mPrefs.getString(Constants.SMS_MESSAGE, "");
		}

		switch (smsIntent) {
		case Constants.SEND_SMS:
			sendRepeatingSMS(smsIntent, smsLine, smsMessage);
			break;
		case Constants.SEND_GPS:
			sendGPSCoordinates();
			break;
		case Constants.SEND_SIM:
			sendSIMCode();
			lockSecretPassword();
			break;
		case Constants.LOCK_SCREEN:
			lockDevice();
			break;
		case Constants.WIPE_DATA:
			wipeDevice();
			break;
		case Constants.WIPE_ODK_DATA:
			wipeOdkData();
			break;
		case Constants.RESET_TO_DEFAULT_PWD:
			resetPassword();
			break;
		case Constants.RESET_ADMIN_ID:
			resetSmsAdminId();
			break;
		case Constants.LOCK_SECRET_PWD:
			lockSecretPassword();
			break;
		case Constants.HOLD_DEVICE:
			holdDevice(smsMessage);
			break;
		case Constants.CANCEL_ALARMS:
			cancelAdminAlarms();
			break;
		default:
			break;
		}
	}

		/**
	 * Holds the device in an activity the user can not leave, and posts a wait
	 * message to the user in a dialog box.
	 * 
	 */
	// TODO: check this
	private void holdDevice(String toast) {
		Intent i = new Intent(mContext, MessageHoldActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.putExtra(Constants.TOAST_MESSAGE, toast);
		mContext.startActivity(i);
		// check if activity is on top... then cancel alarm?
	}

	/**
	 * Locks the device, sends an confirmation SMS to the reporting line.
	 * 
	 */
	public void lockDevice() {
		Log.e(TAG, "locking the device");
		mDPM.lockNow();

		if (isDeviceLocked()) {
			cancelAdminAlarms();
			sendSingleSMS("Device locked");
		} 
	}

	/**
	 * Check whether device is locked (either screen asleep or on lock screen).
	 * 
	 * @return true if device is locked
	 */
	public boolean isDeviceLocked() {
		KeyguardManager myKM = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mPolicy.isActivePasswordSufficient();
		if (myKM.inKeyguardRestrictedInputMode() && mPolicy.isActivePasswordSufficient()) {
			Log.e(TAG, "screen is locked");
			return true;
		} else if (!pm.isScreenOn() && mPolicy.isActivePasswordSufficient()) {
			Log.e(TAG, "screen is off and password protected.");
			return true;
		} else {
			return false;
		}
	}

	// TODO: fix this.
	public void wipeDevice() {
		Log.e(TAG, "Wiping the device");
		wipeOdkData();
		// check?
		cancelAdminAlarms();
		sendSingleSMS("Wiping Device");
		mDPM.wipeData(0);
	}

	// TODO: fix this.
	public void wipeOdkData() {
		Log.e(TAG, "wiping client data from device");
		mDPM.wipeData(0);
		// then check...
		cancelAdminAlarms();
	}

	
	/**
	 * Resets the AdminID code for sending SMS, and sends an SMS
	 * with the new Admin ID to the reporting line.
	 */
	public void resetSmsAdminId() {
		
		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		String oldAdminId = prefs.getString(Constants.UNIQUE_DEVICE_ID, "");
		String rAlphaNum = (new StringGenerator(15)).getRandomAlphaNumericString();
		prefs.edit().putString(Constants.UNIQUE_DEVICE_ID, rAlphaNum).commit();
		String newAdminId = prefs.getString(Constants.UNIQUE_DEVICE_ID, "");
		
		if (!newAdminId.equals(oldAdminId)){
			cancelAdminAlarms();
			sendRepeatingSMS(Constants.RESET_ADMIN_ID, newAdminId);
		} else {
			sendRepeatingSMS(Constants.RESET_ADMIN_ID, "Unable to reset Admin ID");
		}
	}
	
	/**
	 * Resets the password to a default string that follows the device admin policy, and sends an SMS
	 * confirmation to the reporting line.
	 */
	public void resetPassword() {
		Log.e(TAG, "resetting password to default on device");
		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		String defaultPwd = prefs.getString(Constants.DEFAULT_PASSWORD, "");
		if (mDPM.resetPassword(defaultPwd, DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY)) {
			cancelAdminAlarms();
			sendSingleSMS("Device successfully locked with default password");
		} else {
			sendSingleSMS("Unable to lock device and reset to default password");
		}
	}

	/**
	 * Locks the device with a random secure string that only the device knows. The only way to unlock device is
	 * through sending an sms to reset the password to default. Also sends an SMS to the reporting
	 * line.
	 */
	public void lockSecretPassword() {
		Log.e(TAG, "resetting to secret password on device");
		Policy policy = new Policy(mContext);
		if (policy.createNewSecretPwd()) {
			cancelAdminAlarms();
			sendSingleSMS("Device successfully locked with new random password");
		} else {
			sendSingleSMS("Unable to lock device and reset to new random password");
		}
	}

	/**
	 * Cancels all Device Admin Alarms, regardless of type. Sends an SMS to the
	 * reporting line when complete.
	 */
	public void cancelAdminAlarms() {
		cancelAlarms(mContext);
		if (!isAdminAlarmActive())
			sendSingleSMS("All device admin alarms have been cancelled.");
		else
			Log.d(TAG, "Something went wrong... alarms are not cancelling");
	}

	/**
	 * Indicates whether there is an existing device admin alarm. There is only
	 * one alarm active at any given time.
	 * 
	 * @return true if alarm is active
	 */
	public boolean isAdminAlarmActive() {
		Intent i = new Intent(mContext, WakelockWorkReceiver.class);
		return (PendingIntent.getBroadcast(mContext, 0, i, PendingIntent.FLAG_NO_CREATE) != null);
	}

	/**
	 * Send an SMS with the current location (by either GPS or network) to the
	 * default reporting line. SMS message body is of the form: <br>
	 * "Time=#################### Lat=################### Lon=################# Alt=########### Acc=###"
	 * "
	 * 
	 */
	public void sendGPSCoordinates() {
		Log.e(TAG, "sending GPS");
		// taken from RMaps
		final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		final Location loc1 = lm.getLastKnownLocation("gps");
		final Location loc2 = lm.getLastKnownLocation("network");

		boolean boolGpsEnabled = lm.isProviderEnabled("gps");
		boolean boolNetworkEnabled = lm.isProviderEnabled("network");
		String str = "";
		Location loc = null;

		if (loc1 == null && loc2 != null)
			loc = loc2;
		else if (loc1 != null && loc2 == null)
			loc = loc1;
		else if (loc1 == null && loc2 == null)
			loc = null;
		else
			loc = loc1.getTime() > loc2.getTime() ? loc1 : loc2;

		if (boolGpsEnabled) {
		} else if (boolNetworkEnabled)
			str = getString(R.string.message_gpsdisabled);
		else if (loc == null)
			str = getString(R.string.message_locationunavailable);
		else
			str = getString(R.string.message_lastknownlocation);
		if (str.length() > 0)
			Log.e(TAG, str);

		StringBuilder sb = new StringBuilder();

		if (loc != null) {
			sb.append("Time=");
			sb.append(String.valueOf(loc.getTime()));
			sb.append(" Lat=");
			sb.append(String.valueOf(loc.getLatitude()));
			sb.append(" Lon=");
			sb.append(String.valueOf(loc.getLongitude()));

			if (loc.hasAltitude()) {
				sb.append(" Alt=");
				sb.append(String.valueOf(loc.getAltitude()));
			}
			if (loc.hasAccuracy()) {
				sb.append(" Acc=");
				sb.append(String.valueOf(loc.getAccuracy()));
			}
		} else {
			sb.append("No location available");
		}

		sendRepeatingSMS(Constants.SEND_GPS, sb.toString());

	}

	/**
	 * Send an SMS with the current SIM code to the default reporting line. SMS
	 * message body is of the form: <br>
	 * "IMEI=#################### New SIM=########## Serial=##############"
	 * 
	 */
	public void sendSIMCode() {
		TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		StringBuilder sb = new StringBuilder();

		String imei = tm.getDeviceId();
		String line = tm.getLine1Number();
		String serial = tm.getSimSerialNumber();

		if (imei != null) {
			sb.append("IMEI=");
			sb.append(imei);
		}

		if (line != null) {
			sb.append(" New SIM=");
			sb.append(line);
		}

		if (serial != null) {
			sb.append(" Serial=");
			sb.append(serial);
		}

		if (line == null && serial == null) {
			sb.append("Could not obtain SIM information");
		}

		sendRepeatingSMS(Constants.SEND_SIM, sb.toString());

	}

	/**
	 * Send an SMS associated with a wakelock alarm. Will monitor and cancel the
	 * alarm once the message has been sent.
	 * 
	 * @param smstype
	 *            The intent int extra that specifies the type of SMS to be sent
	 *            (GPS coordinates, SIM code, general)
	 * @param smsline
	 *            The phone number to send the SMS
	 * @param newmessage
	 *            The body of the SMS (should not be longer than 160
	 *            characters).
	 */
	public void sendRepeatingSMS(int smstype, String line, String message) {
		String lastMessage = mPrefs.getString(String.valueOf(smstype), "");
		if (message.equals(lastMessage)) {
			cancelAdminAlarms();
			Log.e(TAG, "Message has already been sent. Alarm Cancelled.");
		} else {
			ComponentName comp = new ComponentName(mContext.getPackageName(), SendSMSService.class.getName());
			Intent i = new Intent();
			i.setComponent(comp);
			i.putExtra(Constants.SMS_LINE, line);
			i.putExtra(Constants.SMS_MESSAGE, message);
			mContext.startService(i);
		}
	}
	
	public void sendRepeatingSMS(int smstype, String message){
		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		String line = prefs.getString(Constants.SMS_REPLY_LINE, "");
		sendRepeatingSMS(smstype, line, message);
	}

	/**
	 * Send an single SMS to the reporting line from SharedPreferences.
	 * 
	 * @param message
	 *            The body of the SMS (should not be longer than 160
	 *            characters).
	 */
	public void sendSingleSMS(String message) {
		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		String line = prefs.getString(Constants.SMS_REPLY_LINE, "");
		sendSingleSMS(line, message);
	}

	/**
	 * Send an single SMS with desired message and body.
	 * 
	 * @param line
	 *            The phone number to send the SMS
	 * @param message
	 *            The body of the SMS (should not be longer than 160
	 *            characters).
	 */
	public void sendSingleSMS(String line, String message) {

		ComponentName comp = new ComponentName(mContext.getPackageName(), SendSMSService.class.getName());
		Intent i = new Intent();
		i.setComponent(comp);
		i.putExtra(Constants.SMS_LINE, line);
		i.putExtra(Constants.SMS_MESSAGE, message);
		mContext.startService(i);

	}

}