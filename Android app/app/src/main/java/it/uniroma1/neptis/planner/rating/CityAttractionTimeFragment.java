/*
 * Copyright (c) 2018. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.rating;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;

import it.uniroma1.neptis.planner.Home;
import it.uniroma1.neptis.planner.R;
import it.uniroma1.neptis.planner.asynctasks.JSONAsyncTask;
import it.uniroma1.neptis.planner.asynctasks.ReportAsyncTask;
import it.uniroma1.neptis.planner.firebase.FirebaseOnCompleteListener;
import it.uniroma1.neptis.planner.iface.MainInterface;
import it.uniroma1.neptis.planner.model.Attraction;
import it.uniroma1.neptis.planner.util.ConfigReader;

public class CityAttractionTimeFragment extends Fragment {

    protected MainInterface activity;
    private String attractionURL;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;

    public CityAttractionTimeFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Home.attractionsList.clear();
        attractionURL = ConfigReader.getConfigValue(getContext(), "serverURL");
        attractionURL += "/attractionc";
        attractionURL += "?city=" + getArguments().getString("city");
        attractionURL += "&region=" + getArguments().getString("region");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_city_attraction_time, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.attr_rec);
        adapter = new AttractionRecyclerAdapter(Home.attractionsList);
        recyclerView.setAdapter(adapter);
        JSONAsyncTask task = new GetAttractionsAsyncTask();
        activity.getUser().getIdToken(true)
                .addOnCompleteListener(new FirebaseOnCompleteListener(task,attractionURL));
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

    protected class GetAttractionsAsyncTask extends JSONAsyncTask {

        @Override
        protected Integer doInBackground(String... params) {
            InputStream in;
            int code;
            String urlString = params[0];
            // HTTP GET
            try {

                URL url = new URL(urlString);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Authorization", token);

                in = new BufferedInputStream(urlConnection.getInputStream());
                code = urlConnection.getResponseCode();

            } catch (Exception e) {
                System.out.println(e.getMessage());
                return -1;
            }

            //****  CHECK the ResponseCode FIRST! ****
            if (code == 200) {
                String jsonResponse = readResponse(in);
                Log.d("response", jsonResponse);
                JSONArray attractions = null;
                try {
                    attractions = new JSONArray(jsonResponse);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                JSONObject attraction;
                for (int i = 0; i < attractions.length(); ++i) {
                    try {
                        attraction = attractions.getJSONObject(i);
                        Home.attractionsList.add(Attraction.parse(attraction));
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                }
                return 200;
            } else return code;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == 200) {
                adapter.notifyDataSetChanged();
            } else
                activity.showSnackBar(" There is an error");

        }
    }

    public class AttractionRecyclerAdapter extends RecyclerView.Adapter<AttractionRecyclerAdapter.AttractionHolder> {

        private List<Attraction> mDataset;
        private Random r;

        public AttractionRecyclerAdapter(List<Attraction> attractions) {
            mDataset = attractions;
            r = new Random();
        }

        @Override
        public AttractionHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.time_attraction_item, parent, false);
            AttractionHolder vh;
            vh = new AttractionHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(AttractionHolder holder, int position) {
            holder.bind(mDataset.get(position));
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }

        public class AttractionHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            private TextView attractionName;
            private Button rate;
            private Attraction curr;

            public AttractionHolder(View v) {
                super(v);
                this.attractionName = v.findViewById(R.id.time_txt);
                this.rate = v.findViewById(R.id.time_btn);
                this.rate.setOnClickListener(this);
            }

            public void bind(Attraction attraction) {
                curr = attraction;
                this.attractionName.setText(attraction.getName());
            }

            @Override
            public void onClick(View v) {
                switch(v.getId()) {
                    case R.id.time_btn:
                        JSONAsyncTask task =  new ReportAsyncTask(getContext());
                        activity.getUser().getIdToken(true)
                                .addOnCompleteListener(new FirebaseOnCompleteListener(task, "queue","city", curr.getId(),String.valueOf(r.nextInt(10))))
                                .addOnCompleteListener(new FirebaseOnCompleteListener(new ReportAsyncTask(getContext()), "visit","city", curr.getId(),String.valueOf(r.nextInt(10))));
                        break;
                }
            }
        }

    }

}
