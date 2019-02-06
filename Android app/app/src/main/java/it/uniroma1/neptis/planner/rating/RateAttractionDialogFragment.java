/*
 * Copyright (c) 2018. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.rating;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RatingBar;

import it.uniroma1.neptis.planner.R;
import it.uniroma1.neptis.planner.plans.SelectedPlanFragment;

public class RateAttractionDialogFragment extends DialogFragment {

    private Button okButton;
    private RatingBar bar;

    public interface RatingListener {
        public void onRate(double value);
    }

    public RateAttractionDialogFragment() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
    }

    public static RateAttractionDialogFragment newInstance(String title) {
        RateAttractionDialogFragment frag = new RateAttractionDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rate_popup, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get field from view
        bar = view.findViewById(R.id.attraction_rating_bar);
        okButton = view.findViewById(R.id.ok_rate_fragment);
        okButton.setOnClickListener((View v) -> {
            SelectedPlanFragment frag = (SelectedPlanFragment)getTargetFragment();
            frag.onRate(bar.getRating());
        });
        // Fetch arguments from bundle and set title
        String title = getArguments().getString("title", "Enter Name");
        getDialog().setTitle(title);
    }
}
