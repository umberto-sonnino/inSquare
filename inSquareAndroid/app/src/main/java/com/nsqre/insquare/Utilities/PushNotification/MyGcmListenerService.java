/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nsqre.insquare.Utilities.PushNotification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.nsqre.insquare.Activities.BottomNavActivity;
import com.nsqre.insquare.R;

import java.util.Date;

/**
 * Class that receives and handles gcm messages
 */
public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     * @see #updateSquares(String, String, String)
     * @see #sendNotification(String, String, String)
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "From: " + from);

        if (from.startsWith("/topics/global")) {
            String event = data.getString("event", "");
            String userId = data.getString("userId", "");
            String squareId = data.getString("squareId", "");
            Log.d(TAG, event);
            updateSquares(userId, squareId, event);
        } else {
            String message = data.getString("message", "");
            String squareName = data.getString("squareName", "");
            String squareId = data.getString("squareId", "");
            sendNotification(message, squareName, squareId);
        }
    }
    // [END receive_message]

    /**
     * Method that notifies listeners that some square data changed
     * @param userId the id of the user
     * @param squareId the id of the square that changed
     * @param event what changed
     */
    private void updateSquares(String userId, String squareId, String event) {
        Intent intent = new Intent("update_squares");
        intent.putExtra("event", "update_squares");
        intent.putExtra("action", event);
        intent.putExtra("userId", userId);
        intent.putExtra("squareId", squareId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Create and show a simple notification containing the received GCM message.
     * Manages how the notification will be shown in the notification bar.
     * @param message GCM message received.
     */
    private void sendNotification(String message, String squareName, String squareId) {
        Intent intent = new Intent(this, BottomNavActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        SharedPreferences notificationPreferences = getSharedPreferences("NOTIFICATION_MAP", MODE_PRIVATE);
        int notificationCount = 0;
        int squareCount = notificationPreferences.getInt("squareCount", 0);

        if(squareId.equals(notificationPreferences.getString("actualSquare",""))) {
            return;
        }

        if(!notificationPreferences.contains(squareId)) {
            notificationPreferences.edit().putInt("squareCount", squareCount + 1).apply();
        }
        notificationPreferences.edit().putInt(squareId, notificationPreferences.getInt(squareId, 0) + 1).apply();

        for(String square : notificationPreferences.getAll().keySet()) {
            if(!"squareCount".equals(square) && !"actualSquare".equals(square)) {
                notificationCount += notificationPreferences.getInt(square, 0);
            }
        }

        squareCount = notificationPreferences.getInt("squareCount", 0);

        Log.d(TAG, notificationPreferences.getAll().toString());

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setSmallIcon(R.drawable.nsqre_map_pin_empty_inside);
        notificationBuilder.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));

        if(squareCount == 1) {
            SharedPreferences messagePreferences = getSharedPreferences(squareId, MODE_PRIVATE);
            intent.putExtra("squareId", squareId);
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            messagePreferences.edit().putString(String.valueOf(notificationCount), message).commit();
            if(messagePreferences.getAll().size() <= 6) {
                for(int i = 1; i<=messagePreferences.getAll().keySet().size(); i++) {
                    inboxStyle.addLine(messagePreferences.getString(String.valueOf(i), ""));
                }
            } else {
                for(int i = messagePreferences.getAll().size() - 6; i<=messagePreferences.getAll().keySet().size(); i++) {
                    inboxStyle.addLine(messagePreferences.getString(String.valueOf(i), ""));
                }
            }
            notificationBuilder.setContentTitle(squareName);
            notificationBuilder.setStyle(inboxStyle
                    .setBigContentTitle(squareName)
                    .setSummaryText("inSquare"));
            notificationBuilder.setContentText(notificationCount > 1 ? "Hai " + notificationCount + " nuovi messaggi" : message);
        } else {
            intent.putExtra("map", 0);
            intent.removeExtra("squareId");
            notificationBuilder.setContentTitle("inSquare");
            notificationBuilder.setContentText("Hai " + (notificationCount) + " nuovi messaggi in "
                    + squareCount + " piazze");
        }
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setSound(defaultSoundUri);
        notificationBuilder.setVibrate(new long[] { 300, 300, 300, 300, 300 });
        notificationBuilder.setLights(Color.RED, 1000, 3000);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        updateSquares("","","update");

        SharedPreferences mutePreferences = getSharedPreferences("NOTIFICATION_MUTE_MAP", MODE_PRIVATE);


        if(mutePreferences.contains(squareId)){

            String expireDate = mutePreferences.getString(squareId, "");

                long myExpireDate = Long.parseLong(expireDate, 10);
                if (myExpireDate < (new Date().getTime())) {
                    mutePreferences.edit().remove(squareId).apply();
                    notificationManager.notify(0, notificationBuilder.build());
                }

        } else {

            notificationManager.notify(0, notificationBuilder.build());
        }

    }
    
    public static class QuickstartPreferences {

        public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";
        public static final String REGISTRATION_COMPLETE = "registrationComplete";

    }
}
