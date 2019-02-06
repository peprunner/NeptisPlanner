/*
 * Copyright (c) 2017. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.plans;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import it.uniroma1.neptis.planner.R;
import it.uniroma1.neptis.planner.custom.AttractionArrayAdapter;
import it.uniroma1.neptis.planner.iface.MainInterface;
import it.uniroma1.neptis.planner.model.Attraction;
import it.uniroma1.neptis.planner.model.Plan;
import it.uniroma1.neptis.planner.model.city.CityAttraction;
import it.uniroma1.neptis.planner.model.museum.MuseumAttraction;
import it.uniroma1.neptis.planner.services.tracking.GeofencingService;

public class CurrentPlanFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = CurrentPlanFragment.class.getName();
    protected TextView title;
    private MapView mapView;
    private GoogleMap map;
    private TextView attractionName;
    private Button tourButton;
    protected String planFileName;
    private int index;

    protected String planString;
    protected Plan plan;
    private CityAttraction nextAttraction;
    private Marker nextMarker;

    private AttractionArrayAdapter adapter;

    private ArrayList<Attraction> attractions;

    protected AlertDialog.Builder builder;

    private MainInterface activity;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()) {
                case "tour_update":
                    double latitude = intent.getDoubleExtra("latitude",0.0);
                    double longitude = intent.getDoubleExtra("longitude", 0.0);
                    LatLng coords = new LatLng(latitude, longitude);
                    map.moveCamera(CameraUpdateFactory.newLatLng(coords));
                    break;
                case "next_attraction":
                    int index = intent.getIntExtra("index", -1);
                    if(index == -1)
                        return;
                    updateMap(index);
                    break;
                case "end_tour":
                    activity.popBackStack();
                    break;
            }
        }
    };

    public CurrentPlanFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(isMyServiceRunning(GeofencingService.class)) {
            Log.d(TAG, "service running");
        }

        planFileName = getArguments().getString("computed_plan_file");
        planString = readFile(planFileName);
        index = getArguments().getInt("index");
        plan = parsePlan(planString);

        setUpBroadcastReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_current_plan, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        builder = new AlertDialog.Builder(getContext());
        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        attractionName = view.findViewById(R.id.current_plan_name);
        tourButton = view.findViewById(R.id.current_plan_stop_button);
        tourButton.setOnClickListener(v -> {
            getActivity().stopService(new Intent(getContext(),GeofencingService.class));
            nextMarker.remove();
            activity.mainMenu();
        });
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
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mMessageReceiver);
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private Plan parsePlan(String planString) {
        //Plan plan = null;
        JSONObject obj;
        try {
            obj = new JSONObject(planString);
            String name = obj.getString("name");
            String type = obj.getString("type");
            Plan plan = new Plan(name, type);
            JSONArray route = obj.getJSONArray("route");
            for (int i = 0; i < route.length(); i++) {
                JSONObject attraction = route.getJSONObject(i);
                String attrName = attraction.getString("name");
                String id = attraction.getString("id");
                String lat = attraction.getJSONObject("coordinates").getString("latitude");
                String lng = attraction.getJSONObject("coordinates").getString("longitude");
                double radius = attraction.getDouble("radius");
                CityAttraction a = new CityAttraction(id, attrName, "", (byte) 50, 2,"", lat, lng, radius);
                plan.addAttraction(a);
            }
            attractions = plan.getAttractions();
            return plan;
        } catch (JSONException e) {
            return null;
        }
    }


    private String readFile(String filename) {
        FileInputStream fis = null;
        try {
            fis = getContext().openFileInput(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader bufferedReader = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);
        LatLng current = new LatLng(activity.getLocation().getLatitude(), activity.getLocation().getLongitude());
        PolylineOptions lineOptions = new PolylineOptions();
        //lineOptions.add(current);
        for(Attraction attraction : plan.getAttractions()) {
            CityAttraction cityAttraction = (CityAttraction)attraction;
            Log.d(cityAttraction.getName() + " radius", ""+cityAttraction.getRadius());
            double lat = Double.parseDouble(cityAttraction.getLatitude());
            double lng = Double.parseDouble(cityAttraction.getLongitude());
            LatLng attractionCoord = new LatLng(lat, lng);
            lineOptions.add(attractionCoord);
            map.addCircle(new CircleOptions()
                .center(attractionCoord)
                .radius(cityAttraction.getRadius())
                .fillColor(getResources().getColor(R.color.radius_fill)));
        }
        updateMap(index);
        Polyline line = map.addPolyline(lineOptions);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(current,17));
    }

    private void updateMap(int index) {
        CityAttraction cityAttraction = (CityAttraction)plan.getAttractions().get(index);
        double lat = Double.parseDouble(cityAttraction.getLatitude());
        double lng = Double.parseDouble(cityAttraction.getLongitude());
        LatLng attractionCoord = new LatLng(lat, lng);
        if(nextMarker != null)
            nextMarker.remove();
        nextMarker = map.addMarker(new MarkerOptions().position(attractionCoord)
                .title(cityAttraction.getName()));
        attractionName.setText(cityAttraction.getName());
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void setUpBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter("tour_update");
        intentFilter.addAction("next_attraction");
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(mMessageReceiver, intentFilter);
    }

}
