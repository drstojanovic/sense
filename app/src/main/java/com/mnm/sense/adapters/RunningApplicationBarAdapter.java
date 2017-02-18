package com.mnm.sense.adapters;


import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.mnm.sense.Colors;
import com.ubhave.sensormanager.data.SensorData;
import com.ubhave.sensormanager.data.pull.RunningApplicationData;
import com.ubhave.sensormanager.data.pull.RunningApplicationDataList;

import java.util.ArrayList;
import java.util.HashMap;

import static com.mnm.sense.adapters.RunningApplicationPieAdapter.MAX_APPLICATION;


public class RunningApplicationBarAdapter extends VisualizationAdapter<BarChart,BarData>
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
    public BarData adaptOne(SensorData data)
    {
        RunningApplicationDataList appDataList = (RunningApplicationDataList)data;


        BarData barData = new BarData();

        ArrayList<RunningApplicationData> sorted = appDataList.sort();
        int bound = sorted.size() < MAX_APPLICATION ? sorted.size() : MAX_APPLICATION;

        for(int i = 0; i < bound; i++)
        {
            ArrayList<BarEntry> barEntries = new ArrayList<>();

            barEntries.add(new BarEntry(i, sorted.get(i).getForegroundTimeMins()));
            BarDataSet dataSet = new BarDataSet(barEntries, sorted.get(i).getName());
            dataSet.setColor(Colors.CUSTOM_COLORS[i]);
            barData.addDataSet(dataSet);
        }

        if(bound == MAX_APPLICATION)
        {
            long othersTime = 0;
            for(int i = bound; i < sorted.size(); i++)
                othersTime += sorted.get(i).getForegroundTimeMins();
            ArrayList<BarEntry> barEntries = new ArrayList<>();

            barEntries.add(new BarEntry(bound, othersTime));
            BarDataSet dataSet = new BarDataSet(barEntries, "Other");
            dataSet.setColor(Colors.CUSTOM_COLORS[bound]);
            barData.addDataSet(dataSet);
        }


        barData.setBarWidth(0.9f);
        barData.setValueTextSize(10f);
        barData.setValueTextSize(10f);

        return barData;
    }

    @Override
    public ArrayList<BarData> adaptAll(ArrayList<SensorData> data)
    {
        return null;
    }

    @Override
    public void prepareView(BarChart view)
    {
        YAxis yAxis = view.getAxisLeft();

        yAxis.setDrawAxisLine(true);
        yAxis.setEnabled(true);
        yAxis.setDrawLabels(true);

    }

    @Override
    public VisualizationAdapter<BarChart, BarData> newInstance()
    {
        return new RunningApplicationBarAdapter();
    }

    @Override
    public boolean isAggregating()
    {
        return true;
    }

    @Override
    public Object aggregate(ArrayList<SensorData> data)
    {
        HashMap<String, ArrayList<SensorData>> dataByDay = partitionByDays(data);

        HashMap<String, RunningApplicationData> dataMap = new HashMap<>();

        if(data.size() == 0)
            return null;

        for(ArrayList<SensorData> dataList : dataByDay.values())
        {
            RunningApplicationDataList lastInDay = null;

            int i = dataList.size() - 1;
            while(lastInDay == null && i >= 0)
            {
                lastInDay = (RunningApplicationDataList)dataList.get(i--);
                if(lastInDay.getRunningApplications().size() == 0)
                    lastInDay = null;
            }

            if(lastInDay == null)
                return null;

            for(RunningApplicationData appData: lastInDay.getRunningApplications())
            {
                if(dataMap.containsKey(appData.getName()))
                {
                    RunningApplicationData adjustedData = dataMap.get(appData.getName());
                    adjustedData.setForegroundTime(adjustedData.getForegroundTime() + appData.getForegroundTime());
                    long ltu = appData.getLastTimeUsed() > adjustedData.getLastTimeUsed() ? appData.getLastTimeUsed() : adjustedData.getLastTimeUsed();
                    adjustedData.setLastTimeUsed(ltu);
                    dataMap.put(appData.getName(), adjustedData);
                }
                else
                    dataMap.put(appData.getName(), appData);
            }
        }

        RunningApplicationDataList result = new RunningApplicationDataList(0, data.get(0).getSensorConfig());
        result.setRunningApplications(new ArrayList<>(dataMap.values()));
        return adaptOne(result);
    }
}
