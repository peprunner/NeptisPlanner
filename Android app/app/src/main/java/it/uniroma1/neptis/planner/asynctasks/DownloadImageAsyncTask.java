/*
 * Copyright (c) 2018. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner.asynctasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class DownloadImageAsyncTask extends AsyncTask<String, Void, Bitmap> {

    //private ImageView imageView;
    private WeakReference<ImageView> imageView;

    public DownloadImageAsyncTask(ImageView view) {
        this.imageView = new WeakReference<>(view);
    }

    @Override
    protected Bitmap doInBackground(String... strings) {
        String url = strings[0];
        Bitmap bitmap=null;
        try {
            URL imageUrl = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection)imageUrl.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            InputStream in =conn.getInputStream();
            bitmap = BitmapFactory.decodeStream(in);
        } catch(IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        this.imageView.get().setImageBitmap(bitmap);
    }
}
