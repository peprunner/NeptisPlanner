/*
 * Copyright (c) 2017. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.planning;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;

import java.util.HashMap;
import java.util.Map;

import it.uniroma1.neptis.planner.Home;
import it.uniroma1.neptis.planner.R;
import it.uniroma1.neptis.planner.asynctasks.GetAttractionsListAsyncTask;
import it.uniroma1.neptis.planner.asynctasks.JSONAsyncTask;
import it.uniroma1.neptis.planner.firebase.FirebaseOnCompleteListener;
import it.uniroma1.neptis.planner.iface.MainInterface;

public class VisitsFragment extends Fragment implements View.OnClickListener{

    private static final String TAG = VisitsFragment.class.getName();

    private String attractionURL;

    private Button top10, top20, top30;

    private NumberPicker numPicker;

    private Button next;

    protected MainInterface activity;

    public VisitsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Home.attractionsList.clear();
        String category = getArguments().getString("category");
        switch(category) {
            case "city":
                String city = getArguments().getString("city");
                String region = getArguments().getString("region");
                attractionURL = Home.apiURL + String.format("/attractionc?city=%s&region=%s", city, region);
                break;
            case "museum":
                String museumId = getArguments().getString("id");
                attractionURL = Home.apiURL + String.format("/museums/attractions/%s", museumId);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_visits, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        top10 = view.findViewById(R.id.button_top10);
        top10.setOnClickListener(this);
        top20 = view.findViewById(R.id.button_top20);
        top20.setOnClickListener(this);
        top30 = view.findViewById(R.id.button_top30);
        top30.setOnClickListener(this);

        numPicker = view.findViewById(R.id.numberPicker);
        numPicker.setMinValue(1);
        numPicker.setMaxValue(1);
        numPicker.setValue(1);

        next = view.findViewById(R.id.visits_button_next);
        next.setOnClickListener(this);

        JSONAsyncTask t = new GetAttractionsListAsyncTask(activity, numPicker, top10, top20, top30);
        activity.getUser().getIdToken(true)
                .addOnCompleteListener(new FirebaseOnCompleteListener(t, attractionURL));
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()) {
            case R.id.visits_button_next:
                String numVisits = String.valueOf(numPicker.getValue());
                Map<String,String> parameters = new HashMap<>();
                parameters.put("visits",numVisits);
                activity.selectIncludeExclude(parameters);
                break;

            case R.id.button_top10:
                numPicker.setValue(10);
                break;
            case R.id.button_top20:
                numPicker.setValue(20);
                break;
            case R.id.button_top30:
                numPicker.setValue(30);
                break;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainInterface) {
            activity = (MainInterface) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement IBestTime");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        activity = null;
    }
}
