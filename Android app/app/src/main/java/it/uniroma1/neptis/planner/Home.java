/*
 * Copyright (c) 2017. Thomas Collerton <tho.collerton@gmail.com>
 * This file is part of the Neptis project
 */

package it.uniroma1.neptis.planner;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import it.uniroma1.neptis.planner.asynctasks.GetFirebaseProfilePictureAsyncTask;
import it.uniroma1.neptis.planner.iface.MainInterface;
import it.uniroma1.neptis.planner.model.Attraction;
import it.uniroma1.neptis.planner.model.Plan;
import it.uniroma1.neptis.planner.model.Request;
import it.uniroma1.neptis.planner.planning.AttractionsFragment;
import it.uniroma1.neptis.planner.planning.ChoiceFragment;
import it.uniroma1.neptis.planner.planning.ChooseMuseumFragment;
import it.uniroma1.neptis.planner.planning.VisitsFragment;
import it.uniroma1.neptis.planner.plans.CurrentPlanFragment;
import it.uniroma1.neptis.planner.plans.PlansListFragment;
import it.uniroma1.neptis.planner.plans.SelectedCityPlanFragment;
import it.uniroma1.neptis.planner.plans.SelectedMuseumPlanFragment;
import it.uniroma1.neptis.planner.rating.CityAttractionTimeFragment;
import it.uniroma1.neptis.planner.rating.MuseumAttractionTimeFragment;
import it.uniroma1.neptis.planner.rating.RateAttractionFragment;
import it.uniroma1.neptis.planner.survey.SurveyFragment;
import it.uniroma1.neptis.planner.util.ConfigReader;
import it.uniroma1.neptis.planner.util.LocalStorage;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        MainInterface {

    private static final Logger logger = LoggerFactory.getLogger("logcat_logger");
    public static String apiURL;

    private DrawerLayout drawer;
    private NavigationView navigationView;
    private CoordinatorLayout coordLayout;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    private ConstraintLayout progressLayout;
    private TextView progressText;

    private FragmentManager fragmentManager;
    private FragmentTransaction transaction;
    private Fragment fragment;

    public Request request;

    private FirebaseAuth mAuth;
    private FirebaseUser user;

    private LocationManager locationManager;
    private Location location;
    public Address address;

    private String city = null;
    private String region = null;

    LocationRequest locationRequest;
    FusedLocationProviderClient locationClient;

    public static List<Attraction> attractionsList;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.debug("activity started");
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        apiURL = ConfigReader.getConfigValue(this, "serverURL");

        setContentView(R.layout.activity_home);
        coordLayout = findViewById(R.id.coordinator_layout);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = findViewById(R.id.home_toolbar_progress_bar);
        progressLayout = findViewById(R.id.progress_contraint);
        progressText = findViewById(R.id.home_progress_text);
        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_planning);

        View header = navigationView.getHeaderView(0);
        TextView headerName =
                header.findViewById(R.id.headerNameView);
        headerName.setText(user.getDisplayName());
        TextView headerEmail =
                header.findViewById(R.id.headerEmailView);
        headerEmail.setText(user.getEmail());
        ImageView headerImg = header.findViewById(R.id.headerImageView);
        if(user.getPhotoUrl() != null)
            new GetFirebaseProfilePictureAsyncTask(headerImg).execute(user.getPhotoUrl().toString());

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
          //  Snackbar.make(coordLayout, "Attiva il GPS", Snackbar.LENGTH_LONG).show();
        }

        fragmentManager = getSupportFragmentManager();
        mainMenu();

        locationClient = LocationServices.getFusedLocationProviderClient(this);

        request = null;
        attractionsList = new ArrayList<>();

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);  //location cambiata
        locationRequest.setInterval(1000); // Update location every second

        //Mock Location for Bracciano
        Location bracciano = new Location("test");
        bracciano.setLatitude(42.1017979);
        bracciano.setLongitude(12.176142199999958);
        bracciano.setTime(System.currentTimeMillis());

        //Mock location for Roma
        Location roma = new Location("test");
        roma.setLatitude(41.891348);
        roma.setLongitude(12.502954);
        roma.setTime(System.currentTimeMillis());

        boolean checkFineLocation = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        boolean checkCoarseLocation = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;

        initFragment();

        if (checkCoarseLocation && checkFineLocation) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            progressBar.setVisibility(View.VISIBLE);  //To show ProgressBar
            progressText.setText(R.string.loading_loc_data);
            progressLayout.setVisibility(View.VISIBLE);
            if(BuildConfig.DEBUG) {
                logger.debug("DEBUG");
                locationClient.setMockMode(true);
                locationClient.setMockLocation(roma);
                location = roma;
                computeGeolocation(location);
            }
            else {
                getStartupLocation();
            }

        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getStartupLocation();

                } else {
                    finish();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (getIntent().getStringExtra("computed_plan_file") != null) {
                setCurrentPlan(getIntent().getExtras());
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
//        } else if (fragmentManager.getBackStackEntryCount() == 1) {
//            drawer.openDrawer(GravityCompat.START);
        }else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Premi di nuovo BACK per tornare indietro", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        navigationView.setCheckedItem(id);
        Bundle bundle;
        switch (id) {
            case R.id.nav_planning:
                transaction = fragmentManager.beginTransaction();
                fragment = new ChoiceFragment();
                bundle = new Bundle();
                bundle.putString("city", address.getLocality());
                bundle.putString("region", address.getAdminArea());
                fragment.setArguments(bundle);
                transaction.replace(R.id.content_home, fragment);
                transaction.commit();
                toolbar.setTitle("Crea percorso");
                break;
            /*case R.id.nav_plans:
                transaction = fragmentManager.beginTransaction();
                fragment = new PlansListFragment();
                transaction.replace(R.id.content_home, fragment);
                transaction.commit();
                toolbar.setTitle("Piani salvati");
                break;*/
            case R.id.nav_survey:
                bundle = new Bundle();
                bundle.putString("url","https://goo.gl/forms/FzdfH6JIQ4KXqdLj1");
                transaction = fragmentManager.beginTransaction();
                fragment = new SurveyFragment();
                fragment.setArguments(bundle);
                transaction.replace(R.id.content_home, fragment);
                transaction.commit();
                toolbar.setTitle("Questionario Neptis");
                break;
            case R.id.nav_survey_free:
                bundle = new Bundle();
                bundle.putString("url","https://goo.gl/forms/vOk6h1UdpqZkw7H42");
                transaction = fragmentManager.beginTransaction();
                fragment = new SurveyFragment();
                fragment.setArguments(bundle);
                transaction.replace(R.id.content_home, fragment);
                transaction.commit();
                toolbar.setTitle("Questionario No-Neptis");
                break;
            case R.id.nav_exit:
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(task -> {
                            // user is now signed out
                            startActivity(new Intent(Home.this, LoginActivity.class));
                            finish();
                        });
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @SuppressLint("MissingPermission")
    private void getStartupLocation() {
        locationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(@NonNull Location loc) {
                        location = loc;
                        long currTime = System.currentTimeMillis();
                        //Request new location if last known location is > 5 mins
                        if (location==null) buildAlertMessageNoGps();
                        else {
                            if (currTime <= location.getTime() + (5 * 60 * 1000)) {
                                locationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                                    @Override
                                    public void onLocationResult(LocationResult locationResult) {
                                        super.onLocationResult(locationResult);
                                        locationClient.removeLocationUpdates(this);
                                        location = locationResult.getLastLocation();
                                        computeGeolocation(location);
                                    }
                                }, null);
                            } else {
                                computeGeolocation(location);
                            }
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("onfaillistener", "fail");
                e.printStackTrace();
            }
        });

    }

    @Override
    public void mainMenu() {
        //fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fragmentManager = getSupportFragmentManager();
        transaction = fragmentManager.beginTransaction();
        fragment = new ChoiceFragment();
        Bundle bundle = new Bundle();
        bundle.putString("city", city);
        bundle.putString("region", region);
        fragment.setArguments(bundle);
        transaction.replace(R.id.content_home, fragment, "choice");
        //transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
        toolbar.setTitle("Crea percorso");
        navigationView.setCheckedItem(R.id.nav_planning);
    }

    @Override
    public void selectVisits(Map<String, String> parameters) {
        if(request == null) request = new Request();
        request.addRequestParams(parameters);
        Fragment visitsFragment = new VisitsFragment();

        Bundle b = new Bundle();
        //Parameters needed to make the call in the AsyncTask
        b.putString("category", request.getRequestParameters().get("category"));
        b.putString("id", request.getRequestParameters().get("id"));
        b.putString("city", request.getRequestParameters().get("city"));
        b.putString("region", request.getRequestParameters().get("region"));
        visitsFragment.setArguments(b);
        transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.content_home, visitsFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void selectMuseum(Map<String, String> parameters) {
        request = new Request();
        request.addRequestParams(parameters);
        Fragment chooseMuseumFragment = new ChooseMuseumFragment();
        Bundle b = new Bundle();
        //Parameters needed to make the call in the AsyncTask
        b.putString("category", request.getRequestParameters().get("category"));
        b.putString("city", request.getRequestParameters().get("city"));
        b.putString("region", request.getRequestParameters().get("region"));
        chooseMuseumFragment.setArguments(b);
        transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.content_home, chooseMuseumFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }


    @Override
    public void selectIncludeExclude(Map<String, String> parameters) {
        request.addRequestParams(parameters);
        Fragment attractionsFragment = new AttractionsFragment();
        Bundle b = new Bundle();
        b.putString("category", request.getRequestParameters().get("category"));
        b.putString("city", request.getRequestParameters().get("city"));
        b.putString("region", request.getRequestParameters().get("region"));
        b.putString("id", request.getRequestParameters().get("id"));
        b.putSerializable("request", request);
        attractionsFragment.setArguments(b);
        transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.content_home, attractionsFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void selectPlan(Bundle bundle) {
        transaction = fragmentManager.beginTransaction();
        String planFileName = bundle.getString("computed_plan_file");
        String planString = LocalStorage.readFile(getApplicationContext(), planFileName);
        Plan p = Plan.parse(planString);
        if(p.getType().equals("city"))
            fragment = new SelectedCityPlanFragment();
        else
            fragment = new SelectedMuseumPlanFragment();
//        fragment = new CurrentPlanFragment();
        fragment.setArguments(bundle);
        transaction.replace(R.id.content_home, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void setCurrentPlan(Bundle bundle) {
        transaction = fragmentManager.beginTransaction();
        fragment = new CurrentPlanFragment();
        fragment.setArguments(bundle);
        transaction.replace(R.id.content_home, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
        getSupportActionBar().setTitle("Piano corrente");
    }

    @Override
    public void attractionDetail(Bundle bundle) {
        transaction = fragmentManager.beginTransaction();
        fragment = new RateAttractionFragment();
        fragment.setArguments(bundle);
        transaction.replace(R.id.content_home, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
        getSupportActionBar().setTitle(getString(R.string.fragment_rating_title));
    }

    private void computeGeolocation(Location l) {
        Geocoder g = new Geocoder(getApplicationContext(), Locale.ITALIAN);
        try {
            List<Address> addresses = g.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            address = addresses.get(0);
            Log.d("address", address.getLocality());
            Log.d("address", address.getAddressLine(0));
            city = address.getLocality();
            region = address.getAdminArea();
            progressBar.setVisibility(View.GONE);  //To show ProgressBar
            progressLayout.setVisibility(View.GONE);
            ChoiceFragment f = (ChoiceFragment) fragmentManager.findFragmentByTag("choice");
            if(f != null)
                f.setLocation(city, region);
            else {
                mainMenu();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void popBackStack() {
        fragmentManager.popBackStack();
    }

    @Override
    public FirebaseUser getUser() {
        return user;
    }

    @Override
    public Address getLocation() {
        return this.address;
    }

    @Override
    public void showToolbarProgress() {
        this.progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideToolbarProgress() {
        this.progressBar.setVisibility(View.GONE);
    }

    @Override
    public void showProgressLayout(String text) {

    }

    @Override
    public void hideProgressLayout() {

    }

    @Override
    public void showSnackBar(String msg) {
        Snackbar.make(coordLayout, msg, Snackbar.LENGTH_SHORT).show();
    }


    public void initFragment() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (getIntent().getStringExtra("computed_plan_file") != null) {
                setCurrentPlan(getIntent().getExtras());
                return;
            }
        }
        ChoiceFragment f = (ChoiceFragment) fragmentManager.findFragmentByTag("choice");
        if(f != null)
            f.setLocation(city, region);
        else {
            mainMenu();
        }
    }
    private void buildAlertMessageNoGps() {
       // Toast.makeText(this, "La tua localizzazione è disattivata, attivala e riavvia l'app", Toast.LENGTH_SHORT).show();
        Snackbar.make(coordLayout, "La tua localizzazione sembra disattivata, attivala e riavvia l'app", Snackbar.LENGTH_INDEFINITE).show();
    }
}
