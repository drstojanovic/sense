package com.mnm.sense.adapters;


import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.maps.model.LatLng;
import com.ubhave.sensormanager.data.SensorData;
import com.ubhave.sensormanager.data.pull.ActivityRecognitionData;
import com.ubhave.sensormanager.data.pull.ActivityRecognitionDataList;
import com.ubhave.sensormanager.data.pull.RunningApplicationData;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class ActivityMonitor
{
    private static final int CONFIDENCE_THRESHOLD = 60;
    private static final int CONFIDENCE_THRESHOLD_SENSING = 25;

    public class ActivityPath
    {
        private String text;
        private int type;
        private LatLng origin;
        private ArrayList<LatLng> path;

        public ActivityPath(int type, String text, LatLng loc)
        {
            this.text = text;
            this.type = type;
            origin = loc;
            path = new ArrayList<>();

            if(type != DetectedActivity.STILL)
                path.add(loc);
        }

        public int getType()
        {
            return type;
        }

        public LatLng getLocation()
        {
            return origin;
        }

        public ArrayList<LatLng> getPath()
        {
            return path;
        }

        public void setPoint(LatLng point)
        {
            if(type != DetectedActivity.STILL)
                path.add(point);
        }

        public String getText()
        {
            return text;
        }
    }

    public class ActivityTimeTracker
    {
        public long startTimestamp;
        public int type;

        public SparseArray<Long> activityTimes;
        public ArrayList<ActivityPath> activityPaths;

        public ActivityTimeTracker()
        {
            startTimestamp = 0;
            type = -1;
            activityTimes = new SparseArray<>();
            activityPaths = new ArrayList<>();

            activityTimes.append(DetectedActivity.WALKING, 0l);
            activityTimes.append(DetectedActivity.RUNNING, 0l);
            activityTimes.append(DetectedActivity.STILL, 0l);
            activityTimes.append(DetectedActivity.IN_VEHICLE, 0l);
            activityTimes.append(DetectedActivity.ON_BICYCLE, 0l);
        }



        public int getMinutes(int ... keys)
        {
            long res = 0;
            for(int key : keys)
                res += activityTimes.get(key);

            return (int) TimeUnit.MILLISECONDS.toMinutes(res);
        }

        private boolean satisfies(ActivityRecognitionDataList list, int type)
        {
            for(ActivityRecognitionData data : list.getActivities())
                if(data.getType() == type && data.getConfidence() >= CONFIDENCE_THRESHOLD_SENSING)
                    return true;
            return false;
        }

        public void obtainTimes(ActivityRecognitionDataList dataList)
        {
            ActivityRecognitionData reliableData = null;

            ArrayList<ActivityRecognitionData> valid = getValidActivity(dataList);
            if(valid.size() != 0)
                reliableData = valid.get(0);

            if(reliableData != null)
            {
                if(type == -1)
                {
                    startTimestamp = dataList.getTimestamp();
                    type = reliableData.getType();

                    if(dataList.getLocation() != null)
                        activityPaths.add(new ActivityPath(type, reliableData.getActivityText(), new LatLng(dataList.getLocation().first, dataList.getLocation().second)));

                }
                else
                {
                    if(reliableData.getType() != type && !satisfies(dataList, type))
                    {
                        long time = dataList.getTimestamp() - startTimestamp;

                        activityTimes.put(type, activityTimes.get(type) + time);
                        if(dataList.getLocation() != null)
                            activityPaths.get(activityPaths.size() - 1).setPoint(new LatLng(dataList.getLocation().first, dataList.getLocation().second));

                        type = reliableData.getType();
                        startTimestamp = dataList.getTimestamp();

                        if(dataList.getLocation() != null)
                            activityPaths.add(new ActivityPath(type, reliableData.getActivityText(), new LatLng(dataList.getLocation().first, dataList.getLocation().second)));
                    }
                    else
                    {
                        long time = dataList.getTimestamp() - startTimestamp;
                        activityTimes.put(type, activityTimes.get(type) + time);
                        startTimestamp = dataList.getTimestamp();

                        if(dataList.getLocation() != null)
                            activityPaths.get(activityPaths.size() - 1).setPoint(new LatLng(dataList.getLocation().first, dataList.getLocation().second));
                    }
                }
            }
            else
            {
                if (type != -1)
                {
                    long time = dataList.getTimestamp() - startTimestamp;

                    activityTimes.put(type, activityTimes.get(type) + time);

                    if(dataList.getLocation() != null)
                        activityPaths.get(activityPaths.size() - 1).setPoint(new LatLng(dataList.getLocation().first, dataList.getLocation().second));

                    type = -1;
                    startTimestamp = 0l;
                }
            }
        }

        private ArrayList<ActivityRecognitionData> getValidActivity(ActivityRecognitionDataList activityList)
        {
            ArrayList<ActivityRecognitionData> result = new ArrayList<>();

            for(ActivityRecognitionData activity : activityList.getActivities())
                if(activityTimes.get(activity.getType(), -1l) != -1 && activity.getConfidence() >= CONFIDENCE_THRESHOLD)
                    result.add(activity);

            return result;
        }

    }

    private ActivityTimeTracker liveTimeTracker;

    public ActivityMonitor()
    {
        liveTimeTracker = new ActivityTimeTracker();
    }

    public void liveMonitoring(SensorData dataList)
    {
        liveTimeTracker.obtainTimes((ActivityRecognitionDataList) dataList);
    }

    public ActivityTimeTracker monitorPortion(ArrayList<SensorData> dataList)
    {
        ActivityTimeTracker timeTracker = new ActivityTimeTracker();

        for(SensorData data: dataList)
            timeTracker.obtainTimes((ActivityRecognitionDataList)data);

        return timeTracker;
    }

    public ActivityTimeTracker getLiveTracker()
    {
        return liveTimeTracker;
    }

}
