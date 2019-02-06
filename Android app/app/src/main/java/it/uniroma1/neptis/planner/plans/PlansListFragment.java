/*
 * Copyright (c) 2017. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.plans;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import it.uniroma1.neptis.planner.R;
import it.uniroma1.neptis.planner.iface.MainInterface;

public class PlansListFragment extends Fragment {

    private static final String TAG = PlansListFragment.class.getName();

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private int listPosition = -1;

    private ArrayList<String> filesList;
    private Menu menu;

    private MainInterface activity;

    public PlansListFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plans_list_recycler, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        filesList = new ArrayList<>();
        //Get the list of plans in the folder and display them in the RecyclerView
        File fileDirectory = getContext().getFilesDir();
        for(File f : fileDirectory.listFiles())
            Log.d("file",f.getName());
        File[] dirFiles = fileDirectory.listFiles((dir, name) -> name.matches("[\\w\\s]+_\\d+"));
        for (File f : dirFiles) {
            filesList.add(f.getName());
        }

        Collections.sort(filesList, (s1, s2) -> {
            Long l1 = Long.parseLong(s1.split("_")[1]);
            Long l2 = Long.parseLong(s2.split("_")[1]);
            return l1.compareTo(l2) * -1;
        });

        recyclerView = view.findViewById(R.id.plans_recycler);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);
        adapter = new PlansRecyclerAdapter(filesList);
        recyclerView.setAdapter(adapter);

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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.plans_menu, menu);
        this.menu = menu;
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.delete_plan) {
            File dir = getContext().getFilesDir();
            File file = new File(dir, filesList.get(listPosition));
            boolean deleted = file.delete();
            filesList.remove(listPosition);
            adapter.notifyItemRemoved(listPosition);
            adapter.notifyItemRangeChanged(listPosition, filesList.size());
            activity.showSnackBar("Piano rimosso");
            menu.findItem(R.id.delete_plan).setVisible(false);
        }
        return true;
    }

    private class PlansRecyclerAdapter extends RecyclerView.Adapter<PlansRecyclerAdapter.PlanHolder> {

        private List<String> fileNames;

        private PlansRecyclerAdapter(List<String> files) {
            this.fileNames = files;
        }

        @Override
        public PlansRecyclerAdapter.PlanHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.plans_item_3, parent, false);
            return new PlansRecyclerAdapter.PlanHolder(v);
        }

        @Override
        public void onBindViewHolder(PlansRecyclerAdapter.PlanHolder holder, int position) {
            holder.bind(fileNames.get(position));
        }

        @Override
        public int getItemCount() {
            return fileNames.size();
        }

        public class PlanHolder extends RecyclerView.ViewHolder {
            private TextView planName;
            private TextView planDate;


            private PlanHolder(View v) {
                super(v);
                this.planName = v.findViewById(R.id.plan_n);
                this.planDate = v.findViewById(R.id.plan_d);
                v.setOnClickListener((view) -> {
                    String item = fileNames.get(getLayoutPosition());
                    Bundle bundle = new Bundle();
                    bundle.putString("computed_plan_file", item);
                    bundle.putInt("index", -1);
                    activity.selectPlan(bundle);
                });
                v.setOnLongClickListener((view) -> {
                    //v.setBackgroundColor(Color.LTGRAY);
                    listPosition = getLayoutPosition();
                    menu.findItem(R.id.delete_plan).setVisible(true);
                    return true;
                });
            }

            private void bind(String filename) {
                String[] split = filename.split("_");
                this.planName.setText(split[0]);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(Long.parseLong(split[1]));
                String d = DateFormat.getDateTimeInstance().format(calendar.getTime());
                this.planDate.setText(d);
            }
        }
    }
}
