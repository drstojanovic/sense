package com.ubhave.sensormanager.sensors.pull;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.ubhave.sensormanager.ESException;
import com.ubhave.sensormanager.config.pull.PullSensorConfig;
import com.ubhave.sensormanager.data.SensorData;
import com.ubhave.sensormanager.data.pull.ActivityRecognitionDataList;
import com.ubhave.sensormanager.process.pull.ActivityRecognitionProcessor;
import com.ubhave.sensormanager.sensors.SensorUtils;

import java.util.List;

public class ActivityRecognitionSensor extends AbstractPullSensor implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{


    private static final String TAG = "ActivityRecognitionSensor";
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.PACKAGE_USAGE_STATS
    };

    private GoogleApiClient mApiClient;
    private static ActivityRecognitionSensor activityRecognitionSensor;
    private static Object lock = new Object();

    List<DetectedActivity> detectedActivities;
    private ActivityRecognitionDataList activities;

    private PendingIntent pendingIntent;

    public static ActivityRecognitionSensor getSensor(final Context context) throws ESException
    {
        if (activityRecognitionSensor == null)
        {
            synchronized (lock)
            {
                if (activityRecognitionSensor == null)
                {
                    activityRecognitionSensor = new ActivityRecognitionSensor(context);
                }
            }
        }
        return activityRecognitionSensor;
    }

    public static ActivityRecognitionSensor getSensor()
    {
        return activityRecognitionSensor;
    }

    protected ActivityRecognitionSensor(Context context)
    {
        super(context);
        mApiClient = new GoogleApiClient.Builder(context)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mApiClient.connect();

    }

    @Override
    public SensorData getMostRecentRawData()
    {
        return activities;
    }

    @Override
    public void processSensorData()
    {
        ActivityRecognitionProcessor processor = (ActivityRecognitionProcessor) getProcessor();
        activities = processor.process(pullSenseStartTimestamp, detectedActivities, sensorConfig.clone());
    }

    @Override
    public synchronized void onConnected(@Nullable Bundle bundle)
    {
        Log.d("Connection success", "onConnected called");
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Log.d("Connection suspend", String.valueOf(i));
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        Log.d("Connection failed", connectionResult.toString());
    }


    @Override
    public synchronized boolean startSensing()
    {
        if (mApiClient != null)
        {
            new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        synchronized (ActivityRecognitionSensor.this)
                        {
                            Intent intent = new Intent(applicationContext, ActivityRecognizedService.class);
                            pendingIntent = PendingIntent.getService(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, (long) getSensorConfig(PullSensorConfig.POST_SENSE_SLEEP_LENGTH_MILLIS), pendingIntent);
                        }

                    }
                    catch (ESException e)
                    {
                        e.printStackTrace();
                    }
                }
            }.start();
            return true;
        }
        return false;
    }

    @Override
    public void stopSensing()
    {
        if(mApiClient != null)
        {
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mApiClient, pendingIntent);
        }
    }

    @Override
    protected String getLogTag()
    {
        return TAG;
    }

    @Override
    public int getSensorType()
    {
        return SensorUtils.SENSOR_TYPE_ACTIVITY_RECOGNITION;
    }

    public void handleDetectedActivities(List<DetectedActivity> probableActivities)
    {
        synchronized (this)
        {
            detectedActivities = probableActivities;
//        notifySenseCyclesComplete();
            notify();
        }
    }
}