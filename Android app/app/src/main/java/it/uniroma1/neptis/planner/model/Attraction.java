/*
 * Copyright (c) 2017. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.model;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Attraction implements Serializable {

    private static final String TAG = Attraction.class.getName();

    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    private String description;
    private String category;
    private byte rating;
    private int time;
    private String imageURL;

    public Attraction() {}

    public Attraction(String id, String name, String description, String category, byte rating, int time, String url) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.rating = rating;
        this.time = time;
        this.imageURL = url;
    }

    public Attraction(String id, String name, String description, byte rating, int time, String url) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = "categoria";
        this.rating = rating;
        this.time = time;
        this.imageURL = url;
    }

    public static Attraction parse(JSONObject attraction) {
        try {
            String id = attraction.getString("id");
            String name = attraction.getString("name");
            //byte rating = (byte) attraction.getInt("rating");
            return new Attraction(id,name,"descrizione",(byte)2, 2, null);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }

    }

    public JSONObject serialize() {
        JSONObject out = new JSONObject();
        try {
            out.put("id",id);
            out.put("name",name);
            out.put("rating",rating);
        } catch (JSONException e) {
            return null;
        }
        return out;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }
    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }
    @JsonProperty("name")
    public String getName() {
        return name;
    }
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return this.category;
    }

    public byte getRating() {
        return rating;
    }

    public void setRating(byte rating) {
        this.rating = rating;
    }

    public int getTime() {
        return time;
    }

    public String getImageURL() {
        return this.imageURL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attraction that = (Attraction) o;

        return id.equals(that.id) && name.equals(that.name);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Attraction{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
