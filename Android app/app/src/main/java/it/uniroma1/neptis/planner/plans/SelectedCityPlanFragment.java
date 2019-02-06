/*
 * Copyright (c) 2018. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.plans;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import it.uniroma1.neptis.planner.R;
import it.uniroma1.neptis.planner.model.Attraction;
import it.uniroma1.neptis.planner.model.city.CityAttraction;
import it.uniroma1.neptis.planner.services.tracking.GeofencingService;

public class SelectedCityPlanFragment extends SelectedPlanFragment {

    @Override
    protected void setAdapter() {
        mAdapter = new CityAttractionRecyclerAdapter(plan.getAttractions());
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void startTour() {
        Intent geofencingService = new Intent(getContext(), GeofencingService.class);
        Bundle serviceBundle = new Bundle();
        serviceBundle.putSerializable("attractions", plan.getAttractions());
        serviceBundle.putString("current_plan", planFileName);
        serviceBundle.putInt("index",0);
        geofencingService.putExtras(serviceBundle);
        CityAttraction attraction = (CityAttraction) plan.getAttractions().get(0);
        //initAlertDialog(attraction.getLatitude(), attraction.getLongitude());
        //AlertDialog dialog = builder.create();
        //dialog.show();
        getActivity().startService(geofencingService);
        Bundle fragmentBundle = new Bundle();
        fragmentBundle.putString("computed_plan_file", planFileName);
        fragmentBundle.putInt("index",0);
        activity.setCurrentPlan(fragmentBundle);
    }

    @Override
    protected void stopTour() {

    }

    @Override
    public void onRate(double value) {

    }

    public class CityAttractionRecyclerAdapter extends SelectedPlanFragment.AttractionRecyclerAdapter {

        public CityAttractionRecyclerAdapter(List<Attraction> attractions) {
            super(attractions, "city");
        }

        @Override
        public CityAttractionHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.selected_plan_item, parent, false);
            CityAttractionHolder vh;
            vh = new CityAttractionHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            AttractionHolder attrHolder = (CityAttractionHolder)holder;
            attrHolder.bind(mDataset.get(position));
        }


        public class CityAttractionHolder extends SelectedPlanFragment.AttractionRecyclerAdapter.AttractionHolder{
            private Attraction curr;

            private CityAttractionHolder(View v) {
                super(v);
            }

            @Override
            protected void enableButtons() {
                start.setVisibility(View.GONE);
                stop.setVisibility(View.GONE);
                visitLabel.setVisibility(View.GONE);
            }

            @Override
            protected void startButtonClick() {

            }

            @Override
            protected void stopButtonClick() {

            }
        }

    }
}
