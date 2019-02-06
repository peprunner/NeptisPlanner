/*
 * Copyright (c) 2018. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.asynctasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class GetFirebaseProfilePictureAsyncTask extends AsyncTask<String,Void, Integer> {

    private ImageView view;
    private Bitmap bm;
    public GetFirebaseProfilePictureAsyncTask(ImageView view) {
        this.view = view;
    }
    @Override
    protected Integer doInBackground(String... params) {

        try {
            URL url = new URL(params[0]);
            HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();
            return 200;
        } catch(IOException e) {
            return 500;
        }
    }

    @Override
    protected void onPostExecute(Integer integer) {
        super.onPostExecute(integer);
        if(integer == 200)
            view.setImageBitmap(bm);
    }
}
