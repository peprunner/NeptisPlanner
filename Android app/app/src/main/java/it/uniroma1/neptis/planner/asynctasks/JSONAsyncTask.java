/*
 * Copyright (c) 2018. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.asynctasks;

import android.os.AsyncTask;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public abstract class JSONAsyncTask extends AsyncTask<String,String,Integer> {

    protected ObjectMapper mapper;
    protected String token = null;

    protected JSONAsyncTask() {
        this.mapper = new ObjectMapper();
    }

    protected JSONAsyncTask(String token) {
        this();
        this.token = token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    protected String readResponse(InputStream in){
        //reads from inputStream
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toString();
    }
}
