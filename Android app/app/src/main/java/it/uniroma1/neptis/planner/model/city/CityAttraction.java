/*
 * Copyright (c) 2017. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.model.city;

import it.uniroma1.neptis.planner.model.Attraction;

public class CityAttraction extends Attraction {

    private String latitude;
    private String longitude;
    private double radius;

    public CityAttraction(String id, String name, String description, byte rating, int time, String url, String latitude, String longitude, double radius) {
        super(id, name, description, rating, time, url);
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }
}
