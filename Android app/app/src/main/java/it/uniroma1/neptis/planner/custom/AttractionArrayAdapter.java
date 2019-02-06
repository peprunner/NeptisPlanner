/*
 * Copyright (c) 2017. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.custom;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import it.uniroma1.neptis.planner.R;
import it.uniroma1.neptis.planner.model.Attraction;

public class AttractionArrayAdapter extends ArrayAdapter<Attraction> {

    private List<Attraction> attractions;
    private List<Attraction> attractionsAll;
    private List<Attraction> suggestions;

    public AttractionArrayAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<Attraction> objects) {
        super(context, resource, objects);
        this.attractions = objects;
        this.attractionsAll = new ArrayList<>();
        attractionsAll.addAll(this.attractions);
        this.suggestions = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return this.attractions.size();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Attraction attraction = getItem(position);
        if(convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.plans_list_item, parent, false);
        }
        TextView attractionName = convertView.findViewById(R.id.textest);
        attractionName.setText(attraction.getName());
        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return nameFilter;
    }

    private Filter nameFilter = new Filter() {
        @Override
        public String convertResultToString(Object resultValue) {
            return ((Attraction)(resultValue)).getName();
        }
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            if(constraint != null) {
                suggestions.clear();
                for (Attraction attr : attractionsAll) {
                    if(attr.getName().toLowerCase().startsWith(constraint.toString().toLowerCase())){
                        suggestions.add(attr);
                    }
                }
                filterResults.values = suggestions;
                filterResults.count = suggestions.size();
            } else {
            }
            return filterResults;
        }
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            ArrayList<Attraction> filteredList = (ArrayList<Attraction>) results.values;
            if(results.count > 0) {
                clear();
                for (Attraction c : filteredList) {
                    add(c);
                }
                notifyDataSetChanged();
            }
        }
    };
}
