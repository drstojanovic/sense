package com.mnm.sense.trackers;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.PieData;
import com.google.android.gms.maps.model.LatLng;
import com.mnm.sense.R;
import com.mnm.sense.Repository;
import com.mnm.sense.SenseApp;
import com.mnm.sense.Visualization;
import com.mnm.sense.adapters.VisualizationAdapter;
import com.mnm.sense.models.BarChartModel;
import com.mnm.sense.models.LineChartModel;
import com.mnm.sense.models.ListViewData;
import com.mnm.sense.models.ListViewModel;
import com.mnm.sense.models.MapModel;
import com.mnm.sense.models.PieChartModel;
import com.mnm.sense.models.TextModel;
import com.ubhave.sensormanager.ESException;
import com.ubhave.sensormanager.ESSensorManager;
import com.ubhave.sensormanager.SensorDataListener;
import com.ubhave.sensormanager.config.pull.PullSensorConfig;
import com.ubhave.sensormanager.data.SensorData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Tracker implements SensorDataListener
{
    public static final String DEFAULT_VISUALIZATIONS_KEY = "DefaultVisualizations";

    public class Limit
    {
        public String title;
        public int value;
        public int step;
        public int maxValue;

        public Limit(String text, int val, int s, int max)
        {
            title = text;
            value = val;
            step = s;
            maxValue = max;
        }
    }

    public interface UpdateCallback
    {
        void update(ArrayList<SensorData> with);
    }

    public String text;
    public int resource;
    public boolean isOn;
    public int accent = R.color.colorAccent;
    public int theme = R.style.AppTheme;

    public int type;
    public int id = -1;

    public Limit limit = null;

    public TreeMap<String, Visualization> visualizations = new TreeMap<>();
    public HashMap<String, HashMap<String, VisualizationAdapter>> adapters = new HashMap<>();
    public HashMap<String, UpdateCallback> updateCallbacks = new HashMap<>();
    public ArrayList<SensorData> sensorData = new ArrayList<>();
    public String[] attributes = { };

    protected static Handler handler = new Handler();
//    public Object lock = new Object();

    public Tracker(int t) throws ESException
    {
        type = t;
    }

    @Override
    public void onDataSensed(SensorData data)
    {
        correctData(data);

        sensorData.add(data);

        updateViews();

        if (hasLimit())
            limitNotification(data);

        try
        {
            Repository.instance().logSensorData(data);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onCrossingLowBatteryThreshold(boolean isBelowThreshold)
    {

    }

    // Avoid instantiating the sensor manager before getting all required permissions
    protected ESSensorManager sensorManager() throws ESException
    {
        return ESSensorManager.getSensorManager(SenseApp.context());
    }

    public void start() throws ESException
    {
        isOn = true;
        id = sensorManager().subscribeToSensorData(type, this);
    }

    public void restart()
    {
        try
        {
            if (subscribed())
            {
                sensorManager().unsubscribeFromSensorData(id);
                start();
            }
        }
        catch (ESException e)
        {
            e.printStackTrace();
        }
    }

    public void pause() throws ESException
    {
        isOn = true;
        sensorManager().pauseSubscription(id);
    }

    public void unpause() throws ESException
    {
        isOn = false;
        sensorManager().unPauseSubscription(id);
    }

    public boolean subscribed()
    {
        return id != -1;
    }

    public long getSenseInterval()
    {
        long senseInterval = 0;

        try
        {
            senseInterval = (long) sensorManager().getSensorConfigValue(type, PullSensorConfig.POST_SENSE_SLEEP_LENGTH_MILLIS);
        }
        catch (ESException e)
        {
            e.printStackTrace();
        }

        return senseInterval / 1000;
    }

    public void setSenseInterval(long interval)
    {
        try
        {
            ESSensorManager manager = sensorManager();
            Object value = manager.getSensorConfigValue(type, PullSensorConfig.POST_SENSE_SLEEP_LENGTH_MILLIS);

            if (value != null)
                manager.setSensorConfig(type, PullSensorConfig.POST_SENSE_SLEEP_LENGTH_MILLIS, interval);
        }
        catch (ESException e)
        {
            e.printStackTrace();
        }
    }

    public Object getModel(String visualizationType)
    {
        return getModel(attributes[0], visualizationType);
    }

    public Object getModel(String attribute, String visualizationType)
    {
        HashMap<String, VisualizationAdapter> attributeAdapters = adapters.get(attribute);
        VisualizationAdapter adapter = attributeAdapters.get(visualizationType);
        Object adaptedData = adapter.adapt(sensorData);

        switch (visualizationType)
        {
            case Visualization.TEXT:
                return new TextModel(this, (String) adaptedData);
            case Visualization.BAR_CHART:
                return new BarChartModel(this, (BarData) adaptedData, attribute);
            case Visualization.PIE_CHART:
                return new PieChartModel(this, (PieData) adaptedData);
            case Visualization.MAP:
                return new MapModel(this, (ArrayList<LatLng>) adaptedData, attribute);
            case Visualization.LINE_CHART:
                return new LineChartModel(this, (LineData) adaptedData);
            case Visualization.LIST_VIEW:
                return new ListViewModel(this, (ListViewData) adaptedData);
        }

        return null;
    }

    public VisualizationAdapter adapter(String attribute, String visualization)
    {
        return adapters.get(attribute).get(visualization);
    }

    public VisualizationAdapter defaultAdapter(String visualization)
    {
        return adapter(attributes[0], visualization);
    }

    public void updateViews()
    {
        for (HashMap.Entry entry : updateCallbacks.entrySet())
        {
            String key = (String) entry.getKey();
            UpdateCallback callback = (UpdateCallback) entry.getValue();

            callback.update(sensorData);
        }
    }

    public boolean hasLimit()
    {
        return limit != null;
    }

    public void limitNotification(SensorData data)
    {
    }

    public void correctData(SensorData data)
    {
    }

    public SharedPreferences getConfig()
    {
        return SenseApp.context().getSharedPreferences("com.mnm.sense." + text, Context.MODE_PRIVATE);
    }

    public void addDefaultVisualization(String visualization)
    {
        SharedPreferences prefs = getConfig();
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> defaults = prefs.getStringSet(DEFAULT_VISUALIZATIONS_KEY, new HashSet<String>());

        editor.clear();

        if (!defaults.contains(visualization))
        {
            defaults.add(visualization);
            editor.putStringSet(DEFAULT_VISUALIZATIONS_KEY, defaults);
            editor.commit();
        }
    }

    public void removeDefaultVisualization(String visualization)
    {
        SharedPreferences prefs = getConfig();
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> defaults = prefs.getStringSet(DEFAULT_VISUALIZATIONS_KEY, new HashSet<String>());

        editor.clear().commit();

        if (defaults.contains(visualization))
        {
            Iterator<String> it = defaults.iterator();

            while(it.hasNext())
            {
                if (it.next().equals(visualization))
                {
                    it.remove();
                    break;
                }
            }

            editor.putStringSet(DEFAULT_VISUALIZATIONS_KEY, defaults);
            editor.commit();
        }
    }

    public void purge()
    {
        sensorData.clear();

        updateViews();
    }
}
