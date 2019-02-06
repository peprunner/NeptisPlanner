/*
 * Copyright (c) 2018. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.services.tracking;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import it.uniroma1.neptis.planner.Home;
import it.uniroma1.neptis.planner.R;

import static it.uniroma1.neptis.planner.util.NotificationChannelManagement.initChannels;

public class MuseumVisitService extends IntentService {

    private IBinder mBinder = new VisitsBinder();
    private NotificationManager notificationManager;
    private static final int MAIN_NOTIFICATION_ID = 123;

    private NotificationCompat.Builder builder;
    private Notification notification;
    private String attractionName;
    private int attractionIndex;
    private long startTime;
    private long currTime;
    private Timer timer = new Timer();
    private static final int INTERVAL = 10 * 1000;

    public MuseumVisitService() {
        super("MuseumVisitService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        attractionName = intent.getStringExtra("attractionName");
        attractionIndex = intent.getIntExtra("attractionIndex", -1);
        startTime = System.currentTimeMillis();
        currTime = 0;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                currTime = System.currentTimeMillis();
                int elapsed = getElapsedTime();
                sendTime("visitTime", elapsed);
                launchUpdateNotification(elapsed + " secondi");
            }
        }, 0, INTERVAL);
        launchMainNotification();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    public void launchMainNotification() {
        Intent i = new Intent(this, Home.class);
        //i.putExtra("computed_plan_file", currentPlan);
        //i.putExtra("index", index);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        initChannels(getApplicationContext());
        builder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon((R.drawable.ic_main_notification))
                .setContentTitle("Visita in corso: " + attractionName)
                .setContentText(attractionName)
                .setContentIntent(pi);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notification = builder.build();
        notificationManager.notify(MAIN_NOTIFICATION_ID, notification);
    }

    public void launchUpdateNotification(String message) {
        initChannels(getApplicationContext());
        builder = new NotificationCompat.Builder(this,"default")
                .setSmallIcon((R.drawable.ic_main_notification))
                .setContentTitle("Visita in corso: " + attractionName)
                .setContentText(message);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notification = builder.build();
        notificationManager.notify(MAIN_NOTIFICATION_ID, notification);
    }

    public int getElapsedTime() {
        return (int)((currTime - startTime)/(1000)) + 1;
    }

    private void sendTime(String action, int elapsed) {
        Intent intent = new Intent(action);
        intent.putExtra("VisitTime", elapsed);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    }

    @Override
    public void onDestroy() {
        notificationManager.cancel(MAIN_NOTIFICATION_ID);
        timer.cancel();
        super.onDestroy();
    }

    public class VisitsBinder extends Binder {
        public MuseumVisitService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MuseumVisitService.this;
        }
    }
}
