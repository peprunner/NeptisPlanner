/*
 * Copyright (c) 2018. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.asynctasks;

import android.util.Log;
import android.widget.Button;
import android.widget.NumberPicker;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import it.uniroma1.neptis.planner.Home;
import it.uniroma1.neptis.planner.iface.MainInterface;
import it.uniroma1.neptis.planner.model.Attraction;

public class GetAttractionsListAsyncTask extends JSONAsyncTask {

    private MainInterface activity;
    private NumberPicker picker;
    private Button top10, top20, top30;

    public GetAttractionsListAsyncTask(MainInterface activity, NumberPicker picker, Button... buttons) {
        super();
        this.activity = activity;
        this.picker = picker;
        this.top10 = buttons[0];
        this.top20 = buttons[1];
        this.top30 = buttons[2];
    }

    @Override
    protected void onPreExecute() {
        activity.showToolbarProgress();
    }

    @Override
    protected Integer doInBackground(String... params) {
        InputStream in;
        int code;
        String urlString = params[0];

        try {
            URL url = new URL(urlString);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Authorization", token);

            in = new BufferedInputStream(urlConnection.getInputStream());
            code = urlConnection.getResponseCode();
            if (code == 200) {
                Home.attractionsList = mapper.readValue(in, mapper.getTypeFactory().constructCollectionType(List.class, Attraction.class));
                return 200;
            } else return code;
        } catch (IOException e) {
            e.printStackTrace();
            return 500;
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        activity.hideToolbarProgress();
        if (result == 200) {
            int maxAttractions = Home.attractionsList.size();
            picker.setMaxValue(maxAttractions);
            if(maxAttractions >= 10)
                top10.setEnabled(true);
            if(maxAttractions >= 20)
                top20.setEnabled(true);
            if(maxAttractions >= 30)
                top30.setEnabled(true);

        } else activity.showSnackBar("Errore");
    }
}
