/*
 * Copyright (c) 2017. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import it.uniroma1.neptis.planner.model.city.CityAttraction;
import it.uniroma1.neptis.planner.model.museum.MuseumAttraction;

public class Plan {

    protected String name;
    private String type;
    protected ArrayList<Attraction> attractions;

    public Plan(String name, String type) {
        this.name = name;
        this.type = type;
        this.attractions = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void addAttraction(Attraction ca) {
        attractions.add(ca);
    }

    public ArrayList<Attraction> getAttractions() {
        return this.attractions;
    }

    public static Plan parse(String planString) {
        JSONObject obj;
        try {
            obj = new JSONObject(planString);
            String name = obj.getString("name");
            String type = obj.getString("type");
            Plan plan = new Plan(name, type);
            if(type.equals("city")) {
                JSONArray route = obj.getJSONArray("route");
                for (int i = 0; i < route.length(); i++) {
                    JSONObject attraction = route.getJSONObject(i);
                    String attrName = attraction.getString("name");
                    String id = attraction.getString("id");
                    String description = attraction.getString("description");
                    String lat = attraction.getJSONObject("coordinates").getString("latitude");
                    String lng = attraction.getJSONObject("coordinates").getString("longitude");
                    double radius = attraction.getDouble("radius");
                    int time = attraction.getInt("time");
                    byte attractionRating = (byte)attraction.getInt("rating");
                    String url = attraction.getString("picture");
                    CityAttraction a = new CityAttraction(id, attrName, description, attractionRating, time, url, lat, lng, radius);
                    plan.addAttraction(a);
                }
                return plan;
            } else if(type.equals("museum")) {
                //startButton.setVisibility(View.GONE);
                JSONArray attractions = obj.getJSONArray("route");
                for(int j = 0; j < attractions.length(); j++) {
                    JSONObject att = attractions.getJSONObject(j);
                    String attractionName = att.getString("name");
                    String attractionId = att.getString("id");
                    String description = att.getString("description");
                    String room = att.getString("room");
                    byte attractionRating = (byte)att.getInt("rating");
                    String url = att.getString("picture");
                    int time = att.getInt("time");
                    MuseumAttraction at = new MuseumAttraction(attractionId,attractionName, description, attractionRating, time, url, room);
                    plan.addAttraction(at);
                }
                return plan;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}
