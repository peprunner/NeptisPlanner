/*
 * Copyright (c) 2017. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.model.museum;

import it.uniroma1.neptis.planner.model.Attraction;

public class MuseumAttraction extends Attraction {

    private String area;

    public MuseumAttraction(String id, String name, String description, byte rating, int time,String url, String area) {
        super(id, name,description, rating, time, url);
        this.area = area;
    }

    public String getArea() {
        return area;
    }
}
