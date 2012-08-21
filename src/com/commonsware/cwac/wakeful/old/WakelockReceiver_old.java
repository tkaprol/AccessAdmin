/***
  Copyright (c) 2011 CommonsWare, LLC
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may
  not use this file except in compliance with the License. You may obtain
  a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.commonsware.cwac.wakeful.old;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.commonsware.cwac.wakeful.old.WakefulIntentService_old.AlarmListener;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.XmlResourceParser;
import android.util.Log;


public class WakelockReceiver_old extends BroadcastReceiver {
	private static final String WAKEFUL_META_DATA = "com.commonsware.cwac.wakeful";

	@Override
	public void onReceive(Context ctxt, Intent intent) {
		// it pulls the listener from the xml...
		AlarmListener listener = getListener(ctxt);

		if (listener != null) {
			if (intent.getAction() == null) {
				Log.e("louis.fazen", "onReceive with intent.getAction == null");
				// then it was not sent from onBOOT, so pass on the intent
				SharedPreferences prefs = ctxt.getSharedPreferences(WakefulIntentService_old.NAME, 0);

				prefs.edit().putLong(WakefulIntentService_old.LAST_ALARM, System.currentTimeMillis()).commit();
				// louis.fazen: adding the intent to the work... but if this is
				// simply the onBOOT intent, then this will not work...
				listener.sendWakefulWork(ctxt, intent);
			} else {
				// this is likely sent due to onBoot or some other system has
				// killed our previous process, so re-create an alarm from saved preferences...
				Log.e("louis.fazen", "onReceive with intent.getAction != null");
				//our intent at this point should be something 
				WakefulIntentService_old.scheduleAlarms(listener, ctxt, true, intent);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private WakefulIntentService_old.AlarmListener getListener(Context ctxt) {
		PackageManager pm = ctxt.getPackageManager();
		ComponentName cn = new ComponentName(ctxt, getClass());

		try {
			ActivityInfo ai = pm.getReceiverInfo(cn, PackageManager.GET_META_DATA);
			XmlResourceParser xpp = ai.loadXmlMetaData(pm, WAKEFUL_META_DATA);

			while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
				if (xpp.getEventType() == XmlPullParser.START_TAG) {
					if (xpp.getName().equals("WakefulIntentService_old")) {
						String clsName = xpp.getAttributeValue(null, "listener");
						Class<AlarmListener> cls = (Class<AlarmListener>) Class.forName(clsName);

						return (cls.newInstance());
					}
				}

				xpp.next();
			}
		} catch (NameNotFoundException e) {
			throw new RuntimeException("Cannot find own info???", e);
		} catch (XmlPullParserException e) {
			throw new RuntimeException("Malformed metadata resource XML", e);
		} catch (IOException e) {
			throw new RuntimeException("Could not read resource XML", e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Listener class not found", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Listener is not public or lacks public constructor", e);
		} catch (InstantiationException e) {
			throw new RuntimeException("Could not create instance of listener", e);
		}

		return (null);
	}
}