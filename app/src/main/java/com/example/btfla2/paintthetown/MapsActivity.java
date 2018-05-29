package com.example.btfla2.paintthetown;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.preference.PreferenceManager;
import java.lang.reflect.Type;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        View.OnClickListener,
        GoogleMap.OnCameraMoveStartedListener {

    private static final String version = "Version 0.1.3";

    // Max and Min update intervals for location services
    private static final long MAX_UPDATE_INTERVAL = 10000;
    private static final long MIN_UPDATE_INTERVAL = 10;
    private static final long MAX_TRAVEL_DISTANCE = 150;
    private static final long MIN_TRAVEL_DISTANCE = 10;
    private static final long DEFAULT_ZOOM = 17;

    // Request code
    private static final int LOCATION_REQUEST_CODE = 1563;

    private GoogleMap map;
    public LatLng currentLocation;
    private GoogleApiClient apiClient;
    private boolean canAccessLocation;
    private boolean followingLocation;
    private Marker currentLocationMarker;

    private boolean painting = false;
    private Date startTime;

    private ArrayList<Polyline> polylineList;
    private ArrayList<PolylineOptions> polylineOptionsList;

    private SharedPreferences preferences;
    private SharedPreferences.Editor preferencesEditor;

    private TextView leftBottomBarTextView;
    private TextView rightBottomBarTextView;
    private FloatingActionButton returnToLocationFAB;
    private FloatingActionButton startPaintingFAB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        canAccessLocation = (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);

        if (!canAccessLocation) {
            requestPermissions();
        } else {
            setupAPIClient();
        }

        followingLocation = true;

        // Show back button on title bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Get polyline list from shared preferences
        loadData();

        leftBottomBarTextView = (TextView) findViewById(R.id.leftTextView);
        rightBottomBarTextView = (TextView) findViewById(R.id.rightTextView);
        returnToLocationFAB = (FloatingActionButton) findViewById(R.id.returnToLocationFAB);
        returnToLocationFAB.setOnClickListener(this);
        startPaintingFAB = (FloatingActionButton) findViewById(R.id.centerFAB);
        startPaintingFAB.setOnClickListener(this);

        // Get painting state from intent
        Intent intent = getIntent();
        if (intent != null) {
            painting = intent.getBooleanExtra("painting", false);
        }
        if (painting & startTime == null) {
            startRun();
        }
        updateCenterFAB();
    }

    private void startRun() {
        startTime = Calendar.getInstance().getTime();
        //leftBottomBarTextView.setText(startTime.toString());
        rightBottomBarTextView.setText(version);
    }

    private void saveData() {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferencesEditor = preferences.edit();

        Gson gson = new Gson();
        String json = gson.toJson(polylineOptionsList);

        preferencesEditor.putString("polylineOptions", json);
        preferencesEditor.commit();
    }

    private void loadData() {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Gson gson = new Gson();
        String json = preferences.getString("polylineOptions", null);
        Type type = new TypeToken<ArrayList<PolylineOptions>>() {}.getType();

        polylineOptionsList = gson.fromJson(json, type);

        if (polylineOptionsList == null) {
            polylineOptionsList = new ArrayList<PolylineOptions>();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnCameraMoveStartedListener(this);

        polylineList = new ArrayList<Polyline>();
        for (int i = 0; i < polylineOptionsList.size(); i++) {
            polylineList.add(map.addPolyline(polylineOptionsList.get(i)));
        }

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng position) {
                // TODO: Remove debug tap to change location
                Location location = new Location("clickLocation");
                location.setLatitude(position.latitude);
                location.setLongitude(position.longitude);
                onLocationChanged(location);
            }
        });
    }

    private void startNewPolylineOptions() {
        if (painting) {
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.add(currentLocation);
            polylineOptions.color(generateRandomColour());

            polylineOptionsList.add(polylineOptions);
            polylineList.add(map.addPolyline(polylineOptions));
            saveData();
        }
    }

    private int generateRandomColour() {
        Random r = new Random();
        int colourComponent1 = r.nextInt(256);
        int colourComponent2 = r.nextInt(256);
        int colourChoice = r.nextInt(3);
        int red = 0, green = 0, blue = 0;
        switch (colourChoice){
            case 0:
                green = colourComponent1;
                blue = colourComponent2;
                break;
            case 1:
                red = colourComponent1;
                blue = colourComponent2;
                break;
            case 2:
                red = colourComponent1;
                green = colourComponent2;
        }

        int colour = Color.argb(255, red, green, blue);

        return colour;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (painting) {
            LatLng newLocation = new LatLng(location.getLatitude(), location.getLongitude());
            Float distance;

            if (currentLocation == null) {
                currentLocation = newLocation;

                startNewPolylineOptions();
            } else {
                distance = getDistance(currentLocation, newLocation);

                if (distance > MIN_TRAVEL_DISTANCE) {
                    currentLocation = newLocation;

                    if (distance > MAX_TRAVEL_DISTANCE) {
                        startNewPolylineOptions();
                    } else {
                        if (polylineOptionsList.size() > 0) {
                            polylineOptionsList.get(polylineOptionsList.size() - 1).add(currentLocation);
                            polylineList.get(polylineList.size() - 1).remove();
                            polylineList.set(polylineList.size() - 1, map.addPolyline(polylineOptionsList.get(polylineOptionsList.size() - 1)));
                            saveData();
                        }
                    }

                }
            }

            if (startTime != null) {
                // TODO: Remove debug toast
                Toast.makeText(this, "Duration updated", Toast.LENGTH_SHORT);

                Date currentTime = Calendar.getInstance().getTime();
                long milliseconds = (currentTime.getTime() - startTime.getTime());
                int seconds = (int) (milliseconds / 1000) % 60;
                int minutes = (int) (milliseconds / (1000 * 60)) % 60;
                int hours = (int) (milliseconds / (1000 * 60 * 60)) % 24;
                leftBottomBarTextView.setText(String.format("Duration: %01d:%02d:%02d", hours, minutes, seconds));
            }

            if (followingLocation) {
                setFocus(currentLocation);
            }

            if (currentLocationMarker != null) {
                currentLocationMarker.remove();
            }

            currentLocationMarker = map.addMarker(new MarkerOptions().position(currentLocation)
                    .title("Current Location"));
        }
    }

    public void setFocus(LatLng location) {
        float zoom = map.getCameraPosition().zoom;

        if (map != null) {
            if (zoom < DEFAULT_ZOOM) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_ZOOM));
            } else {
                map.moveCamera(CameraUpdateFactory.newLatLng(location));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_map, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clearPaintItem:
                preferences = PreferenceManager.getDefaultSharedPreferences(this);
                preferencesEditor = preferences.edit();

                new AlertDialog.Builder(this)
                        .setCancelable(true)
                        .setTitle("Confirm Clear Paint")
                        .setMessage("Are you sure you want to clear all the paint? This action can't be undone.")
                        .setPositiveButton("Clear Paint", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                preferencesEditor.clear();
                                preferencesEditor.commit();

                                for (Polyline polyline : polylineList) {
                                    polyline.remove();
                                }

                                polylineList.clear();
                                polylineOptionsList.clear();
                                startNewPolylineOptions();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;

            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(MapsActivity.this)
                    .setTitle("Permission required")
                    .setMessage("Please enable location services to use app")
                    .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MapsActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    LOCATION_REQUEST_CODE);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            finish();
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // Check which code is given
        switch (requestCode) {
            case LOCATION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    canAccessLocation = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
                }
                break;
            default:
                break;
        }

        setupAPIClient();
    }

    private void setupAPIClient() {
        if (canAccessLocation) {
            if (apiClient == null) {
                apiClient = new GoogleApiClient.Builder(this)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addApi(LocationServices.API)
                        .build();

                apiClient.connect();
            }
        } else {
            Toast.makeText(this, "Locations will not be displayed without permissions", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try {
            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setInterval(MAX_UPDATE_INTERVAL)
                    .setFastestInterval(MIN_UPDATE_INTERVAL);

            LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, this);

            //map.setMyLocationEnabled(true);
        } catch (SecurityException secEx) {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show();
        }
    }

    private static float getDistance(LatLng previousLatLng, LatLng nextLatLng) {
        Location previousLocation = new Location("previous");
        previousLocation.setLatitude(previousLatLng.latitude);
        previousLocation.setLongitude(previousLatLng.longitude);

        Location nextLocation = new Location("next");
        nextLocation.setLatitude(nextLatLng.latitude);
        nextLocation.setLongitude(nextLatLng.longitude);

        return previousLocation.distanceTo(nextLocation);
    }


    @Override
    public void onCameraMoveStarted(int reason) {
        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            if (followingLocation) {
                followingLocation = false;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.returnToLocationFAB:
                if (currentLocation != null) {
                    setFocus(currentLocation);
                    followingLocation = true;
                } else {
                    Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.centerFAB:
                if (painting) {
                    painting = false;
                    // change button resource
                    updateCenterFAB();

                    // go to results screen
                } else {
                    painting = true;
                    // change button resource
                    updateCenterFAB();

                    // start recording run
                    startRun();
                }
        }
    }

    private void updateCenterFAB() {
        if (painting) {
            startPaintingFAB.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            startPaintingFAB.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    @Override
    protected void onStart() {
        if (apiClient != null) {
            apiClient.connect();
        }
        super.onStart();
    }

    @Override
    protected void onStop() {
        if (apiClient != null) {
            apiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
