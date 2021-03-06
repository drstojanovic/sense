package com.mnm.sense.trackers;

import com.mnm.sense.R;
import com.mnm.sense.SenseApp;
import com.mnm.sense.Timestamp;
import com.mnm.sense.Util;
import com.mnm.sense.Visualization;
import com.mnm.sense.adapters.CallsLatLngAdapter;
import com.mnm.sense.adapters.CameraLatLngAdapter;
import com.mnm.sense.adapters.RunningAppsLatLngAdapter;
import com.mnm.sense.adapters.SMSLatLngAdapter;
import com.mnm.sense.adapters.VisualizationAdapter;
import com.mnm.sense.map.AttributedFeature;
import com.mnm.sense.models.MapModel;
import com.ubhave.sensormanager.ESException;
import com.ubhave.sensormanager.data.SensorData;
import com.ubhave.sensormanager.sensors.SensorUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class MergedTracker extends Tracker
{
    public static final String ATTRIBUTE_SMS = "SMS";
    public static final String ATTRIBUTE_CALLS = "Calls";
    public static final String ATTRIBUTE_RUNNING_APPS = "Running Apps";
    public static final String ATTRIBUTE_CAMERA = "Camera";

    public ArrayList<Tracker> selectedTrackers;
    private String visualization;

    public MergedTracker(String v, ArrayList<Tracker> selection) throws ESException
    {
        super(SensorUtils.SENSOR_TYPE_LOCATION);

        visualization = v;
        selectedTrackers = selection;

        text = "Location";
        resource = R.drawable.ic_my_location_black_48dp;
        isOn = false;

        attributes = new String[] { "Everything." };

        build()
            .map(new Visualization(1, 1, false));
    }

    public Object getModel(int mode)
    {
        ArrayList<AttributedFeature> features = new ArrayList<>();

        for (Tracker tracker : selectedTrackers)
        {
            if (tracker.visualizations.get(visualization) == null)
                continue;

            MapModel model = (MapModel) tracker.getModel(mode, tracker.attributes[0], visualization);
            features.addAll(model.data);
        }

        return features;
    }

    @Override
    public Object getModel(String visualizationType)
    {
        return new MapModel(this, (ArrayList<AttributedFeature>) getModel(MODE_LOCAL), "Everything.");
    }

    @Override
    public Object getModel(int mode, String attribute, String visualizationType)
    {
        return new MapModel(this, (ArrayList<AttributedFeature>) getModel(mode), "Everything.");
    }

    public String buildRemote() throws ESException
    {
        if (selectedTrackers.size() == 0)
            return null;

        int size = selectedTrackers.size();
        String remote = "";

        for (int i = 0; i < size - 1; ++i)
            remote += SensorUtils.getSensorName(selectedTrackers.get(i).type) + "&";

        remote += SensorUtils.getSensorName(selectedTrackers.get(size - 1).type);

        return remote;
    }
}
