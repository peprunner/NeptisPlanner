/*
 * Copyright (c) 2018. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.plans;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import it.uniroma1.neptis.planner.rating.AskQueueDialogFragment;
import it.uniroma1.neptis.planner.R;
import it.uniroma1.neptis.planner.asynctasks.JSONAsyncTask;
import it.uniroma1.neptis.planner.asynctasks.ReportAsyncTask;
import it.uniroma1.neptis.planner.firebase.FirebaseOnCompleteListener;
import it.uniroma1.neptis.planner.model.Attraction;
import it.uniroma1.neptis.planner.model.museum.MuseumAttraction;
import it.uniroma1.neptis.planner.rating.RateAttractionDialogFragment;
import it.uniroma1.neptis.planner.services.tracking.MuseumVisitService;

public class SelectedMuseumPlanFragment extends SelectedPlanFragment {

    private int index;
    private int index2;
    private MuseumAttractionRecyclerAdapter.MuseumAttractionHolder currentViewHolder = null;

    private RateAttractionDialogFragment rateFragment;

    @Override
    protected void setAdapter() {
        mAdapter = new MuseumAttractionRecyclerAdapter(plan.getAttractions());
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void startTour() {
        startButton.setVisibility(View.GONE);
        stopButton.setVisibility(View.VISIBLE);
        index = 0;
        index2=0;
        updateTour(index);
    }

    private void updateTour(int idx) {
        if (currentViewHolder != null) {
            currentViewHolder.toggleButtons();
            currentViewHolder.itemView.setBackgroundResource(R.drawable.plan_item_background);
        }
        if (idx < mAdapter.getItemCount()) {
            while (recyclerView.findViewHolderForAdapterPosition(idx).getItemViewType() != 1)
                idx++;
            ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(idx, 20);
            currentViewHolder = (MuseumAttractionRecyclerAdapter.MuseumAttractionHolder) recyclerView.findViewHolderForAdapterPosition(idx);
            currentViewHolder.toggleButtons();
            currentViewHolder.itemView.setBackgroundResource(R.drawable.plan_item_background_active);
            index = idx;
              //VIENE CHIAMATO IL FRAGMENT PER CHIEDERE SE C E UNA CODA //BUTTON
        } else {
            stopTour();
        }
    }

    @Override
    protected void stopTour() {
        startButton.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.GONE);
        if(currentViewHolder != null) {
            currentViewHolder.hideButtons();
            currentViewHolder.itemView.setBackgroundResource(R.drawable.plan_item_background);
            currentViewHolder = null;
        }
        index = 0;
        RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
            @Override protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }
        };
        smoothScroller.setTargetPosition(index);
        ((LinearLayoutManager) recyclerView.getLayoutManager()).startSmoothScroll(smoothScroller);
    }

    private void showQueueFragment() {
        FragmentManager fm = getFragmentManager();
        AskQueueDialogFragment editNameDialogFragment = AskQueueDialogFragment.newInstance("Some Title");
        // SETS the target fragment for use later when sending results
        editNameDialogFragment.setTargetFragment(this, 300);
        editNameDialogFragment.show(fm, "fragment_ask_queue");
    }

    private void showRateFragment() {
        FragmentManager fm = getFragmentManager();
        rateFragment = RateAttractionDialogFragment.newInstance("some tt");
        rateFragment.setTargetFragment(this, 350);
        rateFragment.show(fm, "fragment_rate");

    }

    public class MuseumAttractionRecyclerAdapter extends SelectedPlanFragment.AttractionRecyclerAdapter {

        private List<Object> custom;
        public MuseumAttractionRecyclerAdapter(List<Attraction> attractions) {
            super(attractions, "museum");
            custom = new ArrayList<>();
            String curr = ((MuseumAttraction)attractions.get(0)).getArea();
            custom.add(curr);
            for(Attraction a : attractions) {
                MuseumAttraction ma = (MuseumAttraction)a;
                if(!ma.getArea().equals(curr)) {
                    custom.add(ma.getArea());
                    curr = ma.getArea();
                }
                custom.add(ma);
            }
            Log.d("custom", custom.toString());
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v;
            ViewHolder vh = null;
            switch(viewType) {
                case 1:
                    v = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.selected_plan_item_2, parent, false);
                    vh = new MuseumAttractionHolder(v);
                    break;
                case 2:
                    v = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.selected_plan_room, parent, false);
                    vh = new RoomHolder(v);
            }
            return vh;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch(holder.getItemViewType()) {
                case 1:
                    MuseumAttractionHolder attrHolder = (MuseumAttractionHolder)holder;
                    attrHolder.bind((MuseumAttraction)custom.get(position));
                    break;
                case 2:
                    RoomHolder roomHolder = (RoomHolder)holder;
                    roomHolder.bind((String)custom.get(position));
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return custom.size();
        }

        @Override
        public int getItemViewType(int position) {
            Object obj = custom.get(position);
            if(obj instanceof String)
                return 2;
            return 1;
        }

        public class MuseumAttractionHolder extends SelectedPlanFragment.AttractionRecyclerAdapter.AttractionHolder{

            private MuseumAttractionHolder(View v) {
                super(v);
                enableButtons();
            }

            @Override
            protected void enableButtons() {
                start.setVisibility(View.GONE);
                stop.setVisibility(View.GONE);
                visitLabel.setVisibility(View.GONE);
            }

            public void toggleButtons() {
                int visibility = start.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
                int notVisibility = visibility == View.VISIBLE ? View.GONE : View.VISIBLE;
                start.setVisibility(visibility);
                stop.setVisibility(visibility);
                visitLabel.setVisibility(visibility);
                rate.setVisibility(notVisibility);
            }

            public void hideButtons() {
                start.setVisibility(View.GONE);
                stop.setVisibility(View.GONE);
                visitLabel.setVisibility(View.GONE);
                rate.setVisibility(View.VISIBLE);
            }

            @Override
            protected void startButtonClick() {
                Intent intent = new Intent(getContext(), MuseumVisitService.class);
                intent.putExtra("attractionName", curr.getName());
                //intent.putExtra("attractionIndex", curr.)
                getActivity().startService(intent);
                this.start.setEnabled(false);
                this.stop.setEnabled(true);
                showQueueFragment();    //FRAGMENT QUEUE SPOSTATO QUI, CHIEDE SE C E CODA APPENA SI INIZA LA VISITA DI UN ATTRAZIONE //BUTTON
            }

            @Override
            protected void stopButtonClick() {
                Intent intent = new Intent(getContext(), MuseumVisitService.class);
                getActivity().stopService(intent);
                JSONAsyncTask task = new ReportAsyncTask(getContext());
                activity.getUser().getIdToken(true)
                        .addOnCompleteListener(new FirebaseOnCompleteListener(task, "visit", "museum", curr.getId(), String.valueOf(visitTime)));
                this.start.setEnabled(true);
                this.stop.setEnabled(false);
                showRateFragment();
                //updateTour(++index);
            }
        }

        public class RoomHolder extends ViewHolder {

            private TextView tv;

            public RoomHolder(View itemView) {
                super(itemView);
                tv = itemView.findViewById(R.id.textView2);
            }

            public void bind(String name) {
                tv.setText(name);
            }
        }

    }

    @Override
    public void onRate(double value) {
       JSONAsyncTask task = new ReportRateTask(getContext());
        activity.getUser().getIdToken(true)
              .addOnCompleteListener(new FirebaseOnCompleteListener(task, "rating","museum",
                       plan.getAttractions().get(index2).getId(), String.valueOf((int)value)));
        Log.d("index", "value = " + index);
        index2++;
        Log.d("index", "value = " + index2);
        rateFragment.dismiss();
    }

    private class ReportRateTask extends ReportAsyncTask {

        ReportRateTask(Context context) {
            super(context);
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if(result == 201) {
                updateTour(++index);
            }
        }
    }
}
