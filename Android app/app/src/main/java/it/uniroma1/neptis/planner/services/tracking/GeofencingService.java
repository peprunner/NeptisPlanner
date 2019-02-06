/*
 * Copyright (c) 2017. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.services.tracking;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

import it.uniroma1.neptis.planner.Home;
import it.uniroma1.neptis.planner.R;
import it.uniroma1.neptis.planner.asynctasks.JSONAsyncTask;
import it.uniroma1.neptis.planner.asynctasks.ReportAsyncTask;
import it.uniroma1.neptis.planner.firebase.FirebaseOnCompleteListener;
import it.uniroma1.neptis.planner.model.city.CityAttraction;

import static it.uniroma1.neptis.planner.util.NotificationChannelManagement.initChannels;

public class GeofencingService extends IntentService {

    private static final String TAG = GeofencingService.class.getName();
    private IBinder binder = new LocalBinder();
    private LocationRequest locationRequest;
    private FusedLocationProviderClient locationClient;
    private LocationCallback cb;
    private LocationListener destListener;
    private double destinationLat;
    private double destinationLng;
    private double destinationRadius;
    private String currentPlan;
    private String destinationId;
    private String destinationName;

    private double currentLat, currentLng;

    private ArrayList<CityAttraction> attractions;
    private CityAttraction currentAttraction;
    private int index;

    private long fenceEnterTime;
    private long fenceExitTime;
    private boolean fenceEntered;

    private NotificationManager notificationManager;
    private static final int MAIN_NOTIFICATION_ID = 123;
    private static final int UPDATE_NOTIFICATION_ID = 456;
    //Needed to update the notification text
    private NotificationCompat.Builder builder;
    private Notification notification;

    Vibrator vibrator;
    private PowerManager.WakeLock lock;

    private FirebaseUser user;

    public GeofencingService() {
        super("GeofencingService");
        destinationLat = 0.0;
        destinationLng = 0.0;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        lock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "GeofencingWakeLock");
        lock.acquire(60*60*1000L /*10 minutes*/);
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10 * 1000); // Update location every 10 seconds

        cb = new GeofenceLocationCallback();
        locationClient.requestLocationUpdates(locationRequest, cb, null);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        attractions = (ArrayList<CityAttraction>) intent.getSerializableExtra("attractions");
        index = intent.getIntExtra("index", 0);
        if (index == -1)
            index = 0;
        currentAttraction = attractions.get(index);
        currentPlan = intent.getStringExtra("current_plan");
        destinationId = intent.getStringExtra("id");
        destinationName = intent.getStringExtra("name");
        launchMainNotification();
        destinationLat = Double.parseDouble(currentAttraction.getLatitude());
        destinationLng = Double.parseDouble(currentAttraction.getLongitude());
        destinationRadius = currentAttraction.getRadius();

        fenceEntered = false;
        user = FirebaseAuth.getInstance().getCurrentUser();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        locationClient.removeLocationUpdates(cb);
        notificationManager.cancel(MAIN_NOTIFICATION_ID);
        lock.release();
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public GeofencingService getService() {
            return GeofencingService.this;
        }
    }

    public void launchMainNotification() {
        Intent i = new Intent(this, Home.class);
        i.putExtra("computed_plan_file", currentPlan);
        i.putExtra("index", index);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        initChannels(getApplicationContext());
        builder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon((R.drawable.ic_main_notification))
                .setContentTitle("Percorso corrente")
                .setContentText("Prossima attrazione: " + attractions.get(index).getName())
                .setContentIntent(pi);
        notification = builder.build();
        notificationManager.notify(MAIN_NOTIFICATION_ID, notification);
    }

    public void launchUpdateNotification(int index, String message) {
        initChannels(getApplicationContext());
        builder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon((R.drawable.ic_main_notification))
                .setContentTitle(attractions.get(index).getName())
                .setContentText(message)
                .setAutoCancel(true);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notification = builder.build();
        notificationManager.notify(UPDATE_NOTIFICATION_ID, notification);

        // Vibrate for 500 milliseconds
        vibrator.vibrate(500);
    }

    public void launchEndNotification() {
        Intent i = new Intent(this, Home.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        initChannels(getApplicationContext());
        builder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon((R.drawable.ic_main_notification))
                .setContentTitle("Percorso corrente")
                .setContentText("Fine")
                .setContentIntent(pi)
                .setAutoCancel(true);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notification = builder.build();
        notificationManager.notify(MAIN_NOTIFICATION_ID, notification);
    }

    private void sendCoordinates(String action, double latitude, double longitude) {
        Intent intent = new Intent(action);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendTourData() {

    }

    private void sendNextAttraction(int index) {
        Intent intent = new Intent("next_attraction");
        intent.putExtra("index", index);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendTourEnd(int index) {
        Intent intent = new Intent("end_tour");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void updateLocation(Location location) {
        currentLat = location.getLatitude();
        currentLng = location.getLongitude();
        sendCoordinates("tour_update", currentLat, currentLng);
        double distance = getDistance();
        if (distance <= destinationRadius) {
            if (!fenceEntered) {
                fenceEntered = true;
                launchUpdateNotification(index, "Arrivato");
                fenceEnterTime = System.currentTimeMillis();
            }
        } else {
            if (fenceEntered) {
                fenceEntered = false;
                fenceExitTime = System.currentTimeMillis();
                long totalTime = fenceExitTime - fenceEnterTime;
//                    final int minutes = (int) Math.ceil((fenceExitTime - fenceEnterTime) / 60000);
                final int minutes = (int) (totalTime / 60000) + ((totalTime % 60000 == 0) ? 0 : 1);
                JSONAsyncTask task = new ReportAsyncTask(getApplicationContext());
                user.getIdToken(true)
                        .addOnCompleteListener(new FirebaseOnCompleteListener(task,"visit", "city", currentAttraction.getId(), String.valueOf(minutes)));
                launchUpdateNotification(index, "La visita è durata " + minutes + " minuti");
                index++;
                if (index == attractions.size()) {
                    launchEndNotification();
                    stopSelf();
                    return;
                }
                sendNextAttraction(index);
                launchMainNotification();
                currentAttraction = attractions.get(index);
                destinationLat = Double.parseDouble(currentAttraction.getLatitude());
                destinationLng = Double.parseDouble(currentAttraction.getLongitude());

            }
        }
    }

    private double getDistance() {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(currentLat - destinationLat);
        double dLng = Math.toRadians(currentLng - destinationLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(destinationLat)) * Math.cos(Math.toRadians(currentLat)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private class GeofenceLocationCallback extends LocationCallback {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            updateLocation(locationResult.getLastLocation());
        }
    }
}
