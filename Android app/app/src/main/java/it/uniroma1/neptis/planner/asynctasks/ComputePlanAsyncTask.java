/*
 * Copyright (c) 2018. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.asynctasks;

import android.content.Context;
import android.location.Address;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Calendar;

import javax.net.ssl.HttpsURLConnection;

import it.uniroma1.neptis.planner.iface.MainInterface;
import it.uniroma1.neptis.planner.model.Attraction;
import it.uniroma1.neptis.planner.model.Request;

public class ComputePlanAsyncTask extends JSONAsyncTask {

    private MainInterface activity;
    private WeakReference<Context> context;
    private Request request;
    private Address location;

    public ComputePlanAsyncTask(MainInterface activity, Context context,
                                 Request request, Address location) {
        super();
        this.activity = activity;
        this.context = new WeakReference<>(context);
        this.request = request;
        this.location = location;
    }

    @Override
    protected void onPreExecute() {
        activity.showToolbarProgress();
    }

    @Override
    protected Integer doInBackground(String... params) {
        InputStream in;
        int code;
        String charset = "UTF-8";
        String urlString = params[0];

        try {
            URL url = new URL(urlString);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Authorization", token);
            urlConnection.setDoOutput(true);
            urlConnection.setChunkedStreamingMode(0);
            urlConnection.setRequestProperty("Accept-Charset", charset);
            urlConnection.setRequestProperty("Content-Type", "application/json");

            // Object json to send
            JSONObject json = new JSONObject();
            for(String param : request.getRequestParameters().keySet()) {
                json.put(param,request.getRequestParameters().get(param));
            }

            //Add the list of must-see places
            JSONArray must = new JSONArray();
            for(Attraction a : request.getMustVisit())
                must.put(a.getId());
            json.put("must", must);

            //Add the list of excluded places
            JSONArray exclude = new JSONArray();
            for(Attraction a : request.getExcludeVisit())
                exclude.put(a.getId());
            json.put("exclude", exclude);

            json.put("lat", location.getLatitude());
            json.put("lng", location.getLongitude());

            DataOutputStream printout = new DataOutputStream(urlConnection.getOutputStream());
            String s = json.toString();
            byte[] data = s.getBytes("UTF-8");
            printout.write(data);
            printout.flush();
            printout.close();

            in = new BufferedInputStream(urlConnection.getInputStream());
            code = urlConnection.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        if (code == 200) {
            try {
                //TODO check if parsing is useful
                throw new IOException();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            request.setPlan(readResponse(in));
            Log.d("LOG", "Your Plan: " + request.getPlan());
        }
        return code;
    }


    @Override
    protected void onPostExecute(Integer result) {
        activity.hideToolbarProgress();
        if (result == 200) {
            String filename = savePlan();
            Bundle b = new Bundle();
            b.putString("computed_plan_file", filename);
            activity.selectPlan(b);
        }
    }

    private String savePlan() {
        Calendar calendar = Calendar.getInstance();
        long ts = calendar.getTimeInMillis();
        String filename = request.getRequestParameters().get("name")  + "_" + ts;
        FileOutputStream outputStream;
        try {
            outputStream = context.get().openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(request.getPlan().getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filename;
    }
}
