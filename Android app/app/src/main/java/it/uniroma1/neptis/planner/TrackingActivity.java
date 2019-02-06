/*
 * Copyright (c) 2017. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import it.uniroma1.neptis.planner.services.tracking.FINDService;

@Deprecated
public class TrackingActivity extends AppCompatActivity implements View.OnClickListener{

    private Button tracking;
    private Button stop;
    private String attractionId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);
        attractionId = getIntent().getStringExtra("attraction_id");
        tracking = (Button)findViewById(R.id.btn_tracking);
        tracking.setOnClickListener(this);
        stop = (Button)findViewById(R.id.btn_stop_tracking);
        stop.setOnClickListener(this);
        stop.setClickable(false);
    }

    @Override
    public void onClick(View v) {
        Intent i = new Intent(this, FINDService.class);
        i.putExtra("museum",attractionId);
        switch(v.getId()) {
            case R.id.btn_tracking:
                stop.setClickable(true);
                startService(i);
                break;
            case R.id.btn_stop_tracking:
                stop.setClickable(false);
                stopService(i);
                break;
        }
    }
}
