package com.mnm.sense.initializers;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;

import com.mnm.sense.R;
import com.mnm.sense.SenseApp;
import com.mnm.sense.Util;
import com.mnm.sense.Visualization;
import com.mnm.sense.activities.MainActivity;
import com.mnm.sense.activities.SensorSettingsActivity;
import com.mnm.sense.trackers.Tracker;
import com.mnm.sense.views.TrackerView;
import com.ubhave.sensormanager.ESException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TrackerViewInitializer extends ViewInitializer<TrackerView, Tracker>
{
    public static final int REQUEST_CODE = 501;

    public TrackerViewInitializer()
    {
        super(TrackerView.class, Tracker.class);
    }

    @Override
    public void init(final Context context, final TrackerView view, final Tracker model)
    {
        view.text.setText(model.text);
        view.text.setTextColor(context.getResources().getColor(model.accent));
        view.image.setImageResource(model.resource);
        view.image.setColorFilter(context.getResources().getColor(model.accent));
        view.switch_.setChecked(model.isOn);

        Util.setAccent(view.switch_, model.accent);

        view.switch_.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked)
            {
                try
                {
                    if (model.subscribed())
                    {
                        if (isChecked)
                            model.unpause();
                        else
                            model.pause();
                    }
                    else
                    {
                        model.start();

                        ArrayList<String> visualizations = new ArrayList<>();

                        for(Map.Entry<String, Visualization> entry : model.visualizations.entrySet())
                        {
                            if (entry.getValue().isDisplayed)
                                continue;

                            Set<String> defaults = model.getConfig().getStringSet(Tracker.DEFAULT_VISUALIZATIONS_KEY, new HashSet<String>());

                            if (defaults.contains(entry.getKey()))
                            {
                                entry.getValue().isDisplayed = true;
                                visualizations.add(entry.getKey());
                            }
                        }

                        ((MainActivity) context).addDashboardViews(model, visualizations);
                    }
                }
                catch (ESException e)
                {
                    e.printStackTrace();
                }
            }
        });

        view.setOnClickListener(new View.OnClickListener()
        {
            AppCompatActivity activity = (AppCompatActivity) context;
            @Override
            public void onClick(View v)
            {
                Pair<View, String> imagePair = Pair.create((View)view.image, "imageTransition");
                Pair<View, String> switchPair = Pair.create((View)view.switch_, "switchTransition");

                ActivityOptionsCompat options =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(activity,
                                imagePair);

                Intent i = new Intent(activity, SensorSettingsActivity.class);
                i.putExtra("tracker", model.type);

                ActivityCompat.startActivityForResult(activity, i, REQUEST_CODE, options.toBundle());
            }
        });

        view.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                view.setSelected(!view.isSelected());

//                int accent = ColorUtils.setAlphaComponent(SenseApp.context().getResources().getColor(model.accent), 100);
                int accent = SenseApp.context().getResources().getColor(model.accent);

                model.selected = view.isSelected();

                view.setBackgroundColor(model.selected ? accent : Color.WHITE);
                view.text.setTextColor(model.selected ? Color.WHITE : accent);
                view.image.setColorFilter(model.selected ? Color.WHITE : accent);

                Util.setAccent(view.switch_, model.selected ? android.R.color.white : model.accent);

                return true;
            }
        });
    }
}
