package com.mnm.sense.adapters;

import android.location.Location;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.ubhave.sensormanager.data.SensorData;
import com.ubhave.sensormanager.data.pull.LocationData;

import java.util.ArrayList;

public class LocationLatLngAdapter implements VisualizationAdapter<SupportMapFragment, LatLng>
{
    @Override
    public Object adapt(ArrayList<SensorData> data)
    {
        return adaptAll(data);
    }

    @Override
    public LatLng adaptOne(SensorData data)
    {
        LocationData locationData = (LocationData) data;
        Location location = locationData.getLastLocation();

        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    @Override
    public ArrayList<LatLng> adaptAll(ArrayList<SensorData> data)
    {
        ArrayList<LatLng> result = new ArrayList<>(data.size());

        for (SensorData sensorData : data)
            result.add(adaptOne(sensorData));

        return result;
    }

    @Override
    public void prepareView(SupportMapFragment view)
    {

    }
}