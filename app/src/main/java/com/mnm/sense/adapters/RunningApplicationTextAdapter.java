package com.mnm.sense.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.mnm.sense.R;
import com.mnm.sense.initializers.ListViewInitializer;
import com.mnm.sense.models.ListViewData;
import com.ubhave.sensormanager.data.SensorData;
import com.ubhave.sensormanager.data.pull.RunningApplicationData;
import com.ubhave.sensormanager.data.pull.RunningApplicationDataList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RunningApplicationTextAdapter extends RunningApplicationAdapter<ListView, ListViewData>
{
    @Override
    public Object adapt(ArrayList<SensorData> data)
    {
        if(data.size() == 0)
            return null;

        int last = data.size() - 1;

        return adaptOne(data.get(last));
    }

    @Override
    public ListViewData adaptOne(SensorData data)
    {
        RunningApplicationDataList appDataList = (RunningApplicationDataList)data;

        int size = appDataList.getRunningApplications().size();
        Drawable[] images = new Drawable[size];
        String[] names = new String[size];
        long[] lastTimeUsages = new long[size];
        float[] foregroundTimes = new float[size];

        int i = 0;
        for(RunningApplicationData appData: appDataList.getRunningApplications())
        {
            images[i] = appData.getIcon();
            names[i] = appData.getName();
            lastTimeUsages[i] = appData.getLastTimeUsed();
            foregroundTimes[i++] = appData.getForegroundTimeMins();
        }
        return new ListViewData(names, images, foregroundTimes, lastTimeUsages);
    }

    @Override
    public ArrayList<ListViewData> adaptAll(ArrayList<SensorData> data)
    {
        return null;
    }

    @Override
    public void prepareView(ListView view)
    {

    }

    @Override
    public VisualizationAdapter<ListView, ListViewData> newInstance()
    {
        return new RunningApplicationTextAdapter();
    }

}
