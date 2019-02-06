/*
 * Copyright (c) 2017. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.rating;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import it.uniroma1.neptis.planner.R;
import it.uniroma1.neptis.planner.asynctasks.DownloadImageAsyncTask;
import it.uniroma1.neptis.planner.asynctasks.JSONAsyncTask;
import it.uniroma1.neptis.planner.firebase.FirebaseOnCompleteListener;
import it.uniroma1.neptis.planner.iface.MainInterface;
import it.uniroma1.neptis.planner.model.Attraction;
import it.uniroma1.neptis.planner.asynctasks.ReportAsyncTask;

public class RateAttractionFragment extends Fragment implements View.OnClickListener{

    private TextView attractionName;
    private TextView attractionDescription;
    private ImageView attractionPicture;
    private RatingBar ratingBar;
    private Button button;

    private String type;
    private Attraction attraction;

    private MainInterface activity;

    public RateAttractionFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rate_attraction, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        attraction = (Attraction)getArguments().getSerializable("attraction");
        type = getArguments().getString("type");
        attractionName = view.findViewById(R.id.attraction_rating_name);
        attractionName.setText(attraction.getName());
        attractionDescription = view.findViewById(R.id.attraction_rating_description);
        attractionDescription.setText(attraction.getDescription());
        attractionPicture = view.findViewById(R.id.attraction_rating_image);
        //Rating bar can get the values {1.0, 2.0, 3.0, 4.0, 5.0} (floats)
        ratingBar = view.findViewById(R.id.attraction_rating_bar);
        button = view.findViewById(R.id.attraction_rating_button);
        button.setOnClickListener(this);

        new DownloadImageAsyncTask(attractionPicture).execute(attraction.getImageURL());
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

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.attraction_rating_button) {
            JSONAsyncTask task = new RatingAsyncTask(getContext());
            activity.getUser().getIdToken(true)
                    .addOnCompleteListener(new FirebaseOnCompleteListener(task, "rating",type, attraction.getId(), String.valueOf((int)ratingBar.getRating())));
        }
    }

    private class RatingAsyncTask extends ReportAsyncTask {

        public RatingAsyncTask(Context context) {
            super(context);
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            activity.popBackStack();
        }
    }
}
