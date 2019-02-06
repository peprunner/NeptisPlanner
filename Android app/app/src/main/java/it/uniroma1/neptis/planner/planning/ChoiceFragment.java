/*
 * Copyright (c) 2017. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.planning;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import it.uniroma1.neptis.planner.R;
import it.uniroma1.neptis.planner.iface.MainInterface;

public class ChoiceFragment extends Fragment implements View.OnClickListener{

    private ConstraintLayout container;
    private TextView nameView;
    private RadioGroup tourSelect;
    private Button nextButton;

    private String city;
    private String region;
    private String category;

    private MainInterface activity;

    public ChoiceFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        city = getArguments().getString("city");
        region = getArguments().getString("region");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_choice, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = (ConstraintLayout)view;
        if(city == null)
            container.setVisibility(View.INVISIBLE);
        nameView = view.findViewById(R.id.choice_fragment_location_text);
        if(city != null)
            nameView.setText(getString(R.string.city_region, city, region));
        else nameView.setText(R.string.searching);
        nextButton = view.findViewById(R.id.location_fragment_next_button);
        nextButton.setOnClickListener(this);

        tourSelect = view.findViewById(R.id.location_fragment_tour_radio_group);
        tourSelect.setOnCheckedChangeListener((group, checkedId) -> {
            switch(checkedId) {
                case R.id.location_fragment_open_radio:
                    nextButton.setEnabled(true);
                    category = "city";
                    break;
                case R.id.location_fragment_museum_radio:
                    category = "museum";
                    nextButton.setEnabled(true);
                    break;
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.location_fragment_next_button:
                Map<String,String> planningParameters = new HashMap<>();
                planningParameters.put("category", category);
                switch(category) {
                    case "city":
                        planningParameters.put("city", city);
                        planningParameters.put("name", city);
                        planningParameters.put("region", region);
                        activity.selectVisits(planningParameters);
                        break;
                    case "museum":
                        planningParameters.put("city", city);
                        planningParameters.put("region", region);
                        activity.selectMuseum(planningParameters);
                        break;
                }
                break;
        }
    }

    public void setLocation(String city, String region) {
        this.city = city;
        this.region = region;
        nameView.setText(getString(R.string.city_region, city, region));
        this.tourSelect.setEnabled(true);
        container.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainInterface) {
            activity = (MainInterface) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        activity = null;
    }
}