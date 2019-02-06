/*
 * Copyright (c) 2017. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.services.queue;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import it.uniroma1.neptis.planner.R;
import it.uniroma1.neptis.planner.TrackingActivity;
import it.uniroma1.neptis.planner.asynctasks.JSONAsyncTask;
import it.uniroma1.neptis.planner.asynctasks.ReportAsyncTask;
import it.uniroma1.neptis.planner.firebase.FirebaseOnCompleteListener;
import weka.classifiers.Classifier;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import static it.uniroma1.neptis.planner.util.NotificationChannelManagement.initChannels;

public class QueueRecognitionService extends IntentService {

    private static final String TAG = "QueueRecognitionService";
    private IBinder binder = new LocalBinder();

    private FirebaseUser user;

    //Sensor variables
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor pedometer;

    private SensorsListener sensorLstr;

    private NotificationManager notificationManager;
    private static final int NOTIFICATION_ID = 123;
    //Needed to update the notification text
    private NotificationCompat.Builder builder;
    private Notification notification;

    //Data structures for signal sampling and analysis
    private List<Double> xList;
    private List<Double> yList;
    private List<Double> zList;
    private int firstWindowCounter = 0;
    private int slidingWindowCounter = 0;
    private SensorsWindowDetection detection;
    private float steps;
    private LinkedList<Boolean> queueRecognition;
    private long serviceStartTime;
    private String attractionId;

    //Partial wake-lock is needed to keep the CPU awake
    // even when the phone is hibernating -> only for data collection
    private PowerManager.WakeLock wakeLock;

    //Logback logging
    //private final Logger gpsLogger = LoggerFactory.getLogger("gps");
    //private final Logger signalsLogger = LoggerFactory.getLogger("signals");

    //Classifiers
    private Instances instances;
    private Classifier perceptron;
    private int windowSize = 256;
    private int halfWindow = windowSize / 2;

    public QueueRecognitionService() {
        super("QueueRecognitionService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire(60*60*1000L /*60 minutes*/);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        pedometer = null;
        pedometer = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (pedometer != null)
            Log.i(TAG, "pedometer available, using this sensor");
        steps = 0.0F;
        xList = new ArrayList<>();
        yList = new ArrayList<>();
        zList = new ArrayList<>();

        detection = new SensorsWindowDetection();
        queueRecognition = new LinkedList<>();
        serviceStartTime = System.currentTimeMillis();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        attractionId = intent.getStringExtra("destination_id");
        user = FirebaseAuth.getInstance().getCurrentUser();
        setUpNotification();
        startForeground(NOTIFICATION_ID, notification);
        setUpClassifiers();
        startDataCapture();
        //Keep the service as alive as possible
        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {}

    public class LocalBinder extends Binder {
        public QueueRecognitionService getService() {
            return QueueRecognitionService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(sensorLstr);
        stopForeground(true);
        notificationManager.cancel(NOTIFICATION_ID);
        wakeLock.release();
        super.onDestroy();
    }


    public void startDataCapture() {
        Log.i(TAG, "Start writing data");
        sensorLstr = new SensorsListener();
        sensorManager.registerListener(sensorLstr, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        if(pedometer != null)
            sensorManager.registerListener(sensorLstr, pedometer, SensorManager.SENSOR_DELAY_GAME);
    }


    public void receiveAccelerometerData(double x, double y, double z) {
        //Sliding window implementation
        if (firstWindowCounter == windowSize) {
            if (slidingWindowCounter % halfWindow == 0) {
                slidingWindowCounter = 1;
                Instance instance = new DenseInstance(instances.numAttributes());
                instance.setDataset(instances);
                detection.setX(xList);
                detection.setY(yList);
                detection.setZ(zList);
                double[] features = detection.getFeatures();
                for (int d = 0; d < 9; d++)
                    instance.setValue(d, features[d]);
                try {
                    double neural_class;
                    Intent i = new Intent("ML_data");
                    neural_class = perceptron.classifyInstance(instance);
                    i.putExtra("neural", neural_class);
                    boolean walking;
                    walking = !(neural_class == 1.0);
                    System.out.println(walking);
                    queueRecognition.add(walking);
                    if(queueRecognition.size() == 5) {
                        int values = 0;
                        for(boolean b : queueRecognition)
                            if(b)
                                values++;
                        if(values > queueRecognition.size() / 2) {
                            Log.d("QUEUE","start report");
                            long queueTime = System.currentTimeMillis() - serviceStartTime;
                            final int minutes = (int)((queueTime / 1000)/60);
                            JSONAsyncTask task = new ReportAsyncTask(getApplicationContext());
                            user.getIdToken(true)
                                    .addOnCompleteListener(new FirebaseOnCompleteListener(task, "queue",
                                            "museum",
                                            attractionId,
                                            String.valueOf(minutes)));

                            sensorManager.unregisterListener(sensorLstr);
                        }
                        queueRecognition.removeFirst();
                    }
                    i.putExtra("walking", walking);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                slidingWindowCounter++;
            }
            xList.add(x);
            xList.remove(0);
            yList.add(y);
            yList.remove(0);
            zList.add(z);
            zList.remove(0);
        }
        else {
            firstWindowCounter++;
            xList.add(x);
            yList.add(y);
            zList.add(z);
        }
    }

    public void receiveStepCounterData(float s) {
        steps = s;
        Intent intent = new Intent("STEPS");
        intent.putExtra("steps",steps);
        //LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    //Set up the notification, useful to check wether the service is still running
    //with or without the activity being on the foreground
    private void setUpNotification() {
        Intent i = new Intent(this,TrackingActivity.class);
        i.putExtra("attraction_id",attractionId);
        PendingIntent pi = PendingIntent.getActivity(this,0,i,0);
        initChannels(getApplicationContext());
        builder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon((R.drawable.ic_main_notification))
                .setContentTitle("Neptis: queue detection")
                .setContentText("")
                .setContentIntent(pi)
                .setAutoCancel(true);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notification = builder.build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void setUpClassifiers() {
        try {
            InputStream is = getResources().getAssets().open("walk_semifinal2.arff");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            instances = new Instances(br);
            instances.setClassIndex(instances.numAttributes() - 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        perceptron = new MultilayerPerceptron();
        try {
            perceptron.buildClassifier(instances);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class SensorsListener implements android.hardware.SensorEventListener {
        private static final String TAG = "SensorEventListener";
        private double[] gravity;
        final double alpha = 0.8;

        public SensorsListener() {
            this.gravity = new double[3];
            Log.i(TAG, "Listener started");
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            Sensor sensor = event.sensor;
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                //For each axis of the accelerometer, ignore the gravity's forces noise
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                double x = event.values[0] - gravity[0];
                double y = event.values[1] - gravity[1];
                double z = event.values[2] - gravity[2];

                receiveAccelerometerData(x,y,z);
            }
            else if (sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                Log.i(TAG,"Step counted");
                //TODO: compute n of steps in a time frame to detect a walk
                    receiveStepCounterData(event.values[0]);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    }
}
