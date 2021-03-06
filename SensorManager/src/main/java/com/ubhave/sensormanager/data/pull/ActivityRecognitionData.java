package com.ubhave.sensormanager.data.pull;

import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.location.DetectedActivity;

public class ActivityRecognitionData
{
    public static SparseArray<String> activityText;

    private int type;
    private int confidence;

    public ActivityRecognitionData(int type, int confidence)
    {
        this.type = type;
        this.confidence = confidence;

        activityText = new SparseArray<>();
        activityText.append(DetectedActivity.IN_VEHICLE, "In vehicle");
        activityText.append(DetectedActivity.ON_BICYCLE, "On bicycle");
        activityText.append(DetectedActivity.ON_FOOT, "On foot");
        activityText.append(DetectedActivity.RUNNING, "Running");
        activityText.append(DetectedActivity.STILL, "Still");
        activityText.append(DetectedActivity.TILTING, "Tilting");
        activityText.append(DetectedActivity.UNKNOWN, "Unknown");
        activityText.append(DetectedActivity.WALKING, "Walking");

    }

    public int getType()
    {
        return type;
    }

    public static SparseArray<String> getActivityMap()
    {
        return activityText;
    }

    public int getConfidence()
    {
        return confidence;
    }

    public String getActivityText()
    {
        return activityText.get(type);
    }
}
