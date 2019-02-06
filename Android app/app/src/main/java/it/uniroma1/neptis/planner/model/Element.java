/*
 * Copyright (c) 2018. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.model;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "name",
        "id"
})
public class Element implements Comparable<Element>{

    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;

    public Element() {}

    public Element(String name, String id) {
        this.id = id;
        this.name = name;
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

    @Override
    public int compareTo(@NonNull Element o) {
        return this.name.compareTo(o.getName());
    }

    @Override
    public String toString() {
        return "Element{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
