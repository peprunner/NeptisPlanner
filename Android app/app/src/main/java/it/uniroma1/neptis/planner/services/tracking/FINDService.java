/*
 * Copyright (c) 2017. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.services.tracking;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import it.uniroma1.neptis.planner.R;
import it.uniroma1.neptis.planner.model.museum.MuseumAttraction;
import it.uniroma1.neptis.planner.plans.PlansActivity;

public class FINDService extends Service {

    private static final String LEARN = "learn";
    private static final String TRACK = "track";
    private IBinder binder = new LocalBinder();
    private Timer timer;
    private WifiManager wifiManager;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private WifiReceiver wifiReceiver;

    private String museum;
    private ArrayList<MuseumAttraction> attractions;
    private MuseumAttraction currentAttraction;

    private String planFileName;
    private int index;

    private String user;

    private NotificationManager notificationManager;
    private static final int NOTIFICATION_ID = 123;
    //Needed to update the notification text
    private NotificationCompat.Builder builder;
    private Notification notification;

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Find");
        wakeLock.acquire(60*60*1000L /*10 minutes*/);
        wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY,"FindWifiLock");
        wifiLock.acquire();
        wifiReceiver = new WifiReceiver();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        museum = intent.getStringExtra("museum_name");
        attractions = (ArrayList<MuseumAttraction>) intent.getSerializableExtra("attractions");
        user = intent.getStringExtra("user");
        planFileName = intent.getStringExtra("current_plan");
        index = intent.getIntExtra("index", 0);
        if(index == -1)
            index = 0;
        currentAttraction = attractions.get(index);
        setUpNotification();
        startForeground(NOTIFICATION_ID,notification);
        startScanThread();
        return START_STICKY;
    }

    private void startScanThread() {
        timer = new Timer();
        WiFiScan task = new WiFiScan(new WiFiScanHandler());
        timer.schedule(task, 0, 10 * 1000);  // interval 10 seconds
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public FINDService getService() {
            return FINDService.this;
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(wifiReceiver);
        stopForeground(true);
        notificationManager.cancel(NOTIFICATION_ID);
        wifiLock.release();
        wakeLock.release();
        timer.cancel();
        timer.purge();
        super.onDestroy();
    }

    private void setUpNotification() {
        Intent i = new Intent(this,PlansActivity.class);
        i.putExtra("computed_plan_file", planFileName);
        i.putExtra("index", index);
        PendingIntent pi = PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_UPDATE_CURRENT);
        builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_main_notification)
                .setContentTitle(currentAttraction.getName())
                .setContentText("Current room:" + currentAttraction.getArea())
                .setContentIntent(pi);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notification = builder.build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            for(ScanResult r : scanResults)
            new TrackTask(scanResults).execute(user, museum, currentAttraction.getName());
        }
    }

    private class WiFiScan extends TimerTask {
        private Handler handler;

        public WiFiScan(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            handler.sendEmptyMessage(1);
        }
    }

    private class WiFiScanHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    boolean n = wifiManager.startScan();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private class TrackTask extends AsyncTask<String,String,String> {

        private org.slf4j.Logger logger = LoggerFactory.getLogger(TrackTask.class);

        private JSONObject serverProperties;
        private List<ScanResult> scanResults;

        private TrackTask(List<ScanResult> scanResults) {
            this.scanResults = scanResults;
            serverProperties = readPropertiesFile();
        }

        @Override
        protected String doInBackground(String... params) {
            String user = params[0];
            String museum = params[1];
            String attraction = params[2];
            URL url;
            String out = null;
            try {
                url = new URL(serverProperties.getString("ip_address") + "/track");
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                OutputStream os = conn.getOutputStream();
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));
                JSONObject q = new JSONObject();
                q.put("group",museum);
                q.put("username",user);
                q.put("location",attraction);
                q.put("time", System.currentTimeMillis());

                JSONArray array = new JSONArray();
                for(ScanResult s : scanResults) {
                    JSONObject obj = new JSONObject();
                    obj.put("mac",s.BSSID);
                    obj.put("rssi",s.level);
                    array.put(obj);
                }
                q.put("wifi-fingerprint",array);
                Log.i("JSON",q.toString());
                bw.write(q.toString());
                bw.flush();
                bw.close();
                os.close();
                conn.connect();
                InputStream is = new BufferedInputStream(conn.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String s;
                StringBuilder outBuilder = new StringBuilder();
                while((s=br.readLine()) != null) {
                    outBuilder.append(s);
                }
                out = outBuilder.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            JSONObject o = null;
            try {
                o = new JSONObject(out);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return out;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
        }

        private JSONObject readPropertiesFile() {
            JSONObject out = null;
            try {
                StringBuilder properties = new StringBuilder();
                Scanner s = new Scanner(getAssets().open("server_properties.json"));
                while(s.hasNext()) {
                    properties.append(s.next());
                }
                out = new JSONObject(properties.toString());
            } catch(Exception e) {
                e.printStackTrace();
            }
            return out;
        }
    }
}
