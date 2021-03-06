package com.mnm.sense.trackers;

import android.location.Location;
import android.util.Pair;

import com.google.android.gms.location.DetectedActivity;
import com.mnm.sense.Locator;
import com.mnm.sense.NotificationCreator;
import com.mnm.sense.R;
import com.mnm.sense.Visualization;
import com.mnm.sense.adapters.ActivityLatLngAdapter;
import com.mnm.sense.adapters.ActivityMonitor;
import com.mnm.sense.adapters.ActivityPieAdapter;
import com.mnm.sense.adapters.ActivityTextAdapter;
import com.ubhave.sensormanager.ESException;
import com.ubhave.sensormanager.data.SensorData;
import com.ubhave.sensormanager.data.pull.ActivityRecognitionDataList;
import com.ubhave.sensormanager.sensors.SensorUtils;

public class ActivityTracker extends Tracker
{
    private static final String ATTRIBUTE_ACTIVITY = "Activity";
    private boolean first = true;

    private ActivityMonitor monitor;

    public ActivityTracker() throws ESException
    {
        super(SensorUtils.SENSOR_TYPE_ACTIVITY_RECOGNITION);

        text = "Activity";
        resource = R.drawable.ic_directions_run_black_48dp;
        isOn = false;

        attributes = new String[]{ATTRIBUTE_ACTIVITY};

        limit = new Limit("Daily goal", 45, 1, 500);

        monitor = new ActivityMonitor();

        ActivityPieAdapter pieAdapter = new ActivityPieAdapter(monitor);
        pieAdapter.setLimit(limit.value);

        build()
                .map(new Visualization(0, 0, false))
                .text(new Visualization(1, 3, false))
                .pieChart(new Visualization(2, 3, false))
                .attribute(ATTRIBUTE_ACTIVITY)
                .adapters(
                        new ActivityLatLngAdapter(monitor),
                        new ActivityTextAdapter(),
                        pieAdapter);
    }

    @Override
    protected void monitorData(SensorData data)
    {
        monitor.liveMonitoring(data);
    }

    @Override
    public void limitNotification(SensorData data)
    {
        checkGoal();
    }


    private void checkGoal()
    {
        int totalTime = monitor.getLiveTracker().getMinutes(DetectedActivity.RUNNING, DetectedActivity.WALKING);

        if (totalTime >= limit.value && first)
        {
            first = false;
            NotificationCreator.create(resource, "Sense", "Congrats, you reached your daily goal!");
        }
    }

    @Override
    protected void attachLocation(SensorData data)
    {
        Locator locator = Locator.instance();
        Location location = locator.locateAt(data.getTimestamp());

        if (location == null)
        {
            synchronized (locator)
            {
                try
                {
                    locator.wait();
                    location = locator.lastLocation();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }

        ((ActivityRecognitionDataList)data).setLocation(Pair.create(location.getLatitude(), location.getLongitude()));
    }

    @Override
    public void purge()
    {
        first = true;

        super.purge();
    }
}
