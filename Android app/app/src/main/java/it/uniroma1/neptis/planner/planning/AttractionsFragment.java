/*
 * Copyright (c) 2018. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.planning;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.uniroma1.neptis.planner.Home;
import it.uniroma1.neptis.planner.R;
import it.uniroma1.neptis.planner.asynctasks.ComputePlanAsyncTask;
import it.uniroma1.neptis.planner.asynctasks.JSONAsyncTask;
import it.uniroma1.neptis.planner.custom.AttractionArrayAdapter;
import it.uniroma1.neptis.planner.custom.InstantAutoComplete;
import it.uniroma1.neptis.planner.firebase.FirebaseOnCompleteListener;
import it.uniroma1.neptis.planner.iface.MainInterface;
import it.uniroma1.neptis.planner.model.Attraction;
import it.uniroma1.neptis.planner.model.Request;

public class AttractionsFragment extends Fragment implements View.OnClickListener{

    private List<Attraction> attractions;
    private Attraction selectedAttraction;
    private List<Attraction> includeAttractions;
    private List<Attraction> excludeAttractions;
    private List<String> categories;
    private String selectedCategory;
    private List<String> includeCategories, excludeCategories;

    //Input variables
    private Request request;
    private InstantAutoComplete autocompleteNames;
    private InstantAutoComplete autocompleteCategories;
    private Button includeButton;
    private RecyclerView includeRecycler;
    private RecyclerView.Adapter includeAdapter;
    private Button excludeButton;
    private RecyclerView excludeRecycler;
    private RecyclerView.Adapter excludeAdapter;
    private Button next;

    private View prevView = null;
    private int position;

    private String computeURL;

    private MainInterface activity;

    private int from;

    public AttractionsFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        attractions = Home.attractionsList;
        categories = new ArrayList<>();
        Set<String> s = new HashSet<>();
        for(Attraction a : attractions) {
            s.add(a.getCategory());
        }
        categories.addAll(s);
        request = (Request)getArguments().getSerializable("request");
        includeAttractions = new ArrayList<>();
        excludeAttractions = new ArrayList<>();
        computeURL = Home.apiURL + "/compute-plan-" + request.getRequestParameters().get("category");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_attractions, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        autocompleteNames = view.findViewById(R.id.filter_attractions_autocomplete_names);
        ArrayAdapter<Attraction> adapter = new AttractionArrayAdapter(getContext(),android.R.layout.simple_dropdown_item_1line, attractions);
        autocompleteNames.setAdapter(adapter);
        autocompleteCategories = view.findViewById(R.id.filter_attractions_autocomplete_cat);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, categories);
        autocompleteCategories.setAdapter(categoryAdapter);
        includeButton = view.findViewById(R.id.filter_attractions_add_include);
        includeButton.setOnClickListener(this);
        includeRecycler = view.findViewById(R.id.filter_attractions_include_recycler);
        includeAdapter = new AttractionsAdapter(includeAttractions);
        includeRecycler.setAdapter(includeAdapter);
        excludeButton = view.findViewById(R.id.filter_attractions_add_exclude);
        excludeButton.setOnClickListener(this);
        excludeRecycler = view.findViewById(R.id.filter_attractions_exclude_recycler);
        excludeAdapter = new AttractionsAdapter(excludeAttractions);
        excludeRecycler.setAdapter(excludeAdapter);
        next = view.findViewById(R.id.filter_attractions_next);
        next.setOnClickListener(this);

        autocompleteNames.setOnItemClickListener((parent, view1, position, id) -> {
            selectedAttraction = (Attraction) parent.getItemAtPosition(position);
            includeButton.setEnabled(true);
            excludeButton.setEnabled(true);
        });
        autocompleteCategories.setOnItemClickListener((parent, view1, position, id) -> {
            selectedCategory = (String)parent.getItemAtPosition(position);
            includeButton.setEnabled(true);
            excludeButton.setEnabled(true);
        });
    }

    @Override
    public void onClick(View v) {
        autocompleteNames.setText("");
        autocompleteCategories.setText("");
        Attraction selected;
        switch (v.getId()) {
            case R.id.filter_attractions_add_include:
                selected = selectedAttraction;
                if (!excludeAttractions.contains(selected)) {
                    includeAttractions.add(selected);
                    includeAdapter.notifyDataSetChanged();
                    autocompleteNames.setText("");
                } else {
                    activity.showSnackBar("Attrazione esclusa");
                }
                break;
            case R.id.filter_attractions_add_exclude:
                selected = selectedAttraction;
                if (!includeAttractions.contains(selected)) {
                    excludeAttractions.add(selected);
                    excludeAdapter.notifyDataSetChanged();
                    autocompleteNames.setText("");
                } else {
                    activity.showSnackBar("Attrazione inclusa");
                }
                break;
            case R.id.filter_attractions_next:
                from=0;
                // setup the alert builder
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle("Quanto tempo hai a disposizione?");
                alert.setCancelable(false);

                // add a list
                final CharSequence[] choice = {"15-30 min","30-60 min","60+ min"};


                alert.setSingleChoiceItems(choice, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (choice[which] == "15-30 min") {
                            from = 1;
                        }
                        if (choice[which] == "30-60 min") {
                            from = 2;
                        }
                        if (choice[which] == "60+ min") {
                            from = 3;
                        }
                    }
                });
                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String x="";
                        if (from == 0) {
                            Toast.makeText(getContext(), "Seleziona un intervallo di tempo",
                                    Toast.LENGTH_SHORT).show();

                        } else if (from == 1) {
                            // Your Code
                            x="30";
                            dialog.cancel();
                            Intent intent1 = new Intent(getActivity().getBaseContext(),Home.class);
                            intent1.putExtra("message", x);
                            getActivity().startActivity(intent1);
                            request.setMustVisit(includeAttractions);
                            request.setExcludeVisit(excludeAttractions);

                            JSONAsyncTask t = new ComputePlanAsyncTask(activity, getContext(), request, activity.getLocation());
                            activity.getUser().getIdToken(true)
                                    .addOnCompleteListener(new FirebaseOnCompleteListener(t, computeURL));
                        } else if (from == 2) {
                            // Your Code
                            x="60";
                            dialog.cancel();
                            Intent intent1 = new Intent(getActivity().getBaseContext(),Home.class);
                            intent1.putExtra("message", x);
                            getActivity().startActivity(intent1);
                            request.setMustVisit(includeAttractions);
                            request.setExcludeVisit(excludeAttractions);

                            JSONAsyncTask t = new ComputePlanAsyncTask(activity, getContext(), request, activity.getLocation());
                            activity.getUser().getIdToken(true)
                                    .addOnCompleteListener(new FirebaseOnCompleteListener(t, computeURL));
                        } else if (from == 3) {
                            // Your Code
                            x="90";
                            dialog.cancel();
                            Intent intent1 = new Intent(getActivity().getBaseContext(),Home.class);
                            intent1.putExtra("message", x);
                            getActivity().startActivity(intent1);
                            request.setMustVisit(includeAttractions);
                            request.setExcludeVisit(excludeAttractions);

                            JSONAsyncTask t = new ComputePlanAsyncTask(activity, getContext(), request, activity.getLocation());
                            activity.getUser().getIdToken(true)
                                    .addOnCompleteListener(new FirebaseOnCompleteListener(t, computeURL));
                        }

                    }
                });
                AlertDialog dialog = alert.create();
                // Display the alert dialog on interface
                dialog.show();
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

    private class AttractionsAdapter extends RecyclerView.Adapter<AttractionsAdapter.AttractionHolder> {

        private List<Attraction> attractions;

        private AttractionsAdapter(List<Attraction> attractions) {
            this.attractions = attractions;
        }

        @Override
        public AttractionHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.museums_list_item, parent, false);
            return new AttractionHolder(v);
        }

        @Override
        public void onBindViewHolder(AttractionHolder holder, int position) {
            holder.getTextView().setText(attractions.get(position).getName());
        }

        @Override
        public int getItemCount() {
            return attractions.size();
        }


        protected class AttractionHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener{

            private TextView museumName;

            private AttractionHolder(View v) {
                super(v);
                this.museumName = v.findViewById(R.id.museum_name);
                v.setOnLongClickListener(AttractionHolder.this);
            }

            private TextView getTextView() {
                return this.museumName;
            }

            @Override
            public boolean onLongClick(View v) {
                if(prevView != null) {
                    prevView.setBackgroundColor(Color.WHITE);
                }
                v.setBackgroundColor(Color.LTGRAY);
                prevView = v;
                position = getLayoutPosition();
                return true;
            }
        }
    }
}
