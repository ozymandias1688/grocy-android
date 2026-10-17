/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.notification;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.NOTIFICATIONS;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.util.DateUtil;

public class ChoresActionReceiver extends BroadcastReceiver {

  public static final String ACTION_EXECUTE = "xyz.zedler.patrick.grocy.action.CHORE_EXECUTE";
  public static final String ACTION_SKIP = "xyz.zedler.patrick.grocy.action.CHORE_SKIP";
  public static final String EXTRA_CHORE_ID = "chore_id";
  public static final String EXTRA_TRACK_DATE_ONLY = "track_date_only";

  @Override
  public void onReceive(Context context, Intent intent) {
    boolean skip = ACTION_SKIP.equals(intent.getAction());
    int choreId = intent.getIntExtra(EXTRA_CHORE_ID, -1);
    boolean trackDateOnly = intent.getBooleanExtra(EXTRA_TRACK_DATE_ONLY, false);
    if (choreId == -1) {
      return;
    }
    int notificationId = getNotificationId(choreId);

    NotificationManager notificationManager = (NotificationManager) context.getSystemService(
        Context.NOTIFICATION_SERVICE
    );
    if (notificationManager != null) {
      notificationManager.cancel(notificationId);
    }

    DateUtil dateUtil = new DateUtil(context);
    String trackedTime = trackDateOnly
        ? dateUtil.getCurrentDateWithoutTimeStr()
        : dateUtil.getCurrentDateWithTimeStr();
    JSONObject body = new JSONObject();
    try {
      body.put("skipped", skip);
      body.put("tracked_time", trackedTime);
    } catch (JSONException e) {
      return;
    }

    DownloadHelper dlHelper = new DownloadHelper(
        context, ChoresActionReceiver.class.getSimpleName()
    );
    dlHelper.post(
        dlHelper.grocyApi.executeChore(choreId),
        body,
        response -> {
          showToast(context, skip
              ? R.string.msg_chore_schedule_skipped
              : R.string.msg_chore_executed);
          dlHelper.destroy();
        },
        error -> {
          showToast(context, R.string.error_network);
          dlHelper.destroy();
        }
    );
  }

  private void showToast(Context context, int messageResId) {
    new Handler(Looper.getMainLooper()).post(
        () -> Toast.makeText(context, messageResId, Toast.LENGTH_SHORT).show()
    );
  }

  public static int getNotificationId(int choreId) {
    return NOTIFICATIONS.CHORES_ID + 1 + choreId;
  }

  public static Intent getIntent(Context context, String action, int choreId,
      boolean trackDateOnly) {
    Intent intent = new Intent(context, ChoresActionReceiver.class);
    intent.setAction(action);
    intent.putExtra(EXTRA_CHORE_ID, choreId);
    intent.putExtra(EXTRA_TRACK_DATE_ONLY, trackDateOnly);
    return intent;
  }
}
