/*
 * Copyright (c) 2018. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.planning;


import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.uniroma1.neptis.planner.Home;
import it.uniroma1.neptis.planner.R;
import it.uniroma1.neptis.planner.asynctasks.GetMuseumsListAsyncTask;
import it.uniroma1.neptis.planner.asynctasks.JSONAsyncTask;
import it.uniroma1.neptis.planner.firebase.FirebaseOnCompleteListener;
import it.uniroma1.neptis.planner.iface.MainInterface;
import it.uniroma1.neptis.planner.model.Element;

public class ChooseMuseumFragment extends Fragment implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener{

    private String museumUrl;
    private String city, region;
    private List<Element> museumQuery;
    private List<Element> filteredList;
    private int position;
    private View prevView;
    private MainInterface activity;

    private EditText nameSearch;
    private SwipeRefreshLayout refreshLayout;
    private RecyclerView recyclerView;
    private MuseumRecyclerAdapter adapter;
    private Button next;

    public ChooseMuseumFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        city = getArguments().getString("city");
        region = getArguments().getString("region");
        position = -1;
        prevView = null;
        museumQuery = new ArrayList<>();
        filteredList = new ArrayList<>();
        this.museumUrl = Home.apiURL + String.format("/museums?city=%s&region=%s", city, region);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_choose_museum, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refreshLayout = view.findViewById(R.id.swiperefresh);
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setColorSchemeResources(R.color.neptis_light, R.color.neptis_blue);
        recyclerView = view.findViewById(R.id.choose_museum_recycler);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);
        adapter = new MuseumRecyclerAdapter(filteredList);
        recyclerView.setAdapter(adapter);

        refreshLayout.setRefreshing(true);
        JSONAsyncTask t = new GetMuseumsListAsyncTask(activity, refreshLayout, museumQuery, filteredList, adapter);
        activity.getUser().getIdToken(true)
                .addOnCompleteListener(new FirebaseOnCompleteListener(t, museumUrl));

        nameSearch = view.findViewById(R.id.choose_museum_filter);
        nameSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        next = view.findViewById(R.id.choose_museum_next);
        next.setOnClickListener(this);
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
        inflater.inflate(R.menu.choose_museum_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        refreshLayout.setRefreshing(true);
        JSONAsyncTask t = new GetMuseumsListAsyncTask(activity, refreshLayout, museumQuery, filteredList, adapter);
        if(item.getItemId() == R.id.choose_museum_menu_refresh) {
            activity.getUser().getIdToken(true)
                    .addOnCompleteListener(
                            new FirebaseOnCompleteListener(t, museumUrl));
        }
        return true;
    }



    @Override
    public void onClick(View v) {
        Element selected = filteredList.get(position);
        Map<String, String> planningParameters = new HashMap<>();
        planningParameters.put("id", selected.getId());
        planningParameters.put("name", selected.getName());
        activity.selectVisits(planningParameters);
    }

    @Override
    public void onRefresh() {
        JSONAsyncTask t = new GetMuseumsListAsyncTask(activity, refreshLayout, museumQuery, filteredList, adapter);
        activity.getUser().getIdToken(true)
                .addOnCompleteListener(
                        new FirebaseOnCompleteListener(t, museumUrl));
    }

    public class MuseumRecyclerAdapter extends RecyclerView.Adapter<MuseumRecyclerAdapter.MuseumHolder> {

        private List<Element> museums;
        private CustomFilter filter;

        private MuseumRecyclerAdapter(List<Element> museums) {
            this.museums = museums;
            this.filter = new CustomFilter(MuseumRecyclerAdapter.this);
        }

        @Override
        public MuseumHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.museums_list_item, parent, false);
            return new MuseumHolder(v);
        }

        @Override
        public void onBindViewHolder(MuseumHolder holder, int position) {
            holder.getTextView().setText(museums.get(position).getName());
        }

        @Override
        public int getItemCount() {
            return museums.size();
        }

        public Filter getFilter() {
            return this.filter;
        }

        protected class MuseumHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

            private TextView museumName;

            private MuseumHolder(View v) {
                super(v);
                this.museumName = v.findViewById(R.id.museum_name);
                v.setOnClickListener(MuseumHolder.this);
            }

            private TextView getTextView() {
                return this.museumName;
            }

            @Override
            public void onClick(View v) {
                if(prevView != null) {
                    prevView.setBackgroundColor(Color.WHITE);
                }

                v.setBackgroundColor(Color.LTGRAY);
                prevView = v;
                position = getLayoutPosition();
                next.setEnabled(true);
                next.setAlpha(1.0f);
            }
        }

        private class CustomFilter extends Filter {

            private MuseumRecyclerAdapter adapter;
            private CustomFilter(MuseumRecyclerAdapter adapter) {
                super();
                this.adapter = adapter;
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                filteredList.clear();
                final FilterResults results = new FilterResults();
                if (constraint.length() == 0) {
                    filteredList.addAll(museumQuery);
                } else {
                    final String filterPattern = constraint.toString().toLowerCase().trim();
                    for (final Element mWords : museumQuery) {
                        if (mWords.getName().toLowerCase().startsWith(filterPattern)) {
                            filteredList.add(mWords);
                        }
                    }
                }
                System.out.println("Count Number " + filteredList.size());
                results.values = filteredList;
                results.count = filteredList.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                this.adapter.notifyDataSetChanged();
            }
        }
    }
}
