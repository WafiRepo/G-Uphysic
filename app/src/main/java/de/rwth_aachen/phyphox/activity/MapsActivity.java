package de.rwth_aachen.phyphox.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import de.rwth_aachen.phyphox.App;
import de.rwth_aachen.phyphox.Helper.FirestoreUtil;
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.databinding.ActivityMapsBinding;
import de.rwth_aachen.phyphox.model.DataModel;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Marker currentLocationMarker;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        App app = (App) getApplication();
        DataModel dataModel = app.getDataModel();
        if(!dataModel.getPhotoDraw().isEmpty()){
            loadImageWithGlide(dataModel.getPhotoDraw().get(dataModel.getPhotoDraw().size()-1), binding.imgLeft);
        }
        loadImageWithGlide(dataModel.getPhoto(), binding.imgRight);
        // Set up the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        // Define the LocationCallback to handle location updates
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateLocationOnMap(location);
                }
            }
        };

        // Request location permission
        checkLocationPermission();
        binding.btnShareLocation.setOnClickListener(view -> {
            if (dataModel.getLongitude() != 0.0 && dataModel.getLatitude() != 0.0 && !dataModel.getLocationName().isEmpty()) {
                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setTitle("Save data to Server");
                progressDialog.setMessage("Please wait...");
                progressDialog.setCancelable(false);
                progressDialog.show();
                
                String userId = SessionManager.getId(MapsActivity.this);
                String userName = SessionManager.getName(MapsActivity.this);
                
                FirestoreUtil.addOrUpdateDocumentWithVersioning("record", dataModel.getId(), dataModel,
                        userId, userName,
                        () -> {
                            progressDialog.dismiss();
                            Toast.makeText(MapsActivity.this, "Record success Saved", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(MapsActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        },
                        e -> {
                            progressDialog.dismiss();
                            Toast.makeText(MapsActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            } else {
                Toast.makeText(MapsActivity.this, "Update Location First", Toast.LENGTH_LONG).show();
            }
        });
    }

    // Check and request location permission
    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    // Start receiving real-time location updates
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000); // Set interval to 5 seconds for updates

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.mMap = googleMap;
    }

    private void updateLocationOnMap(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String city;
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            city = (addresses != null && addresses.size() > 0) ? addresses.get(0).getAddressLine(0) : "Unknown Location";

            Log.d("MAPS_ACTIVITY", "Updated Location: " + city);

            // Remove previous marker if it exists
            if (currentLocationMarker != null) {
                currentLocationMarker.remove();
            }
            App app = (App) getApplication();
            DataModel dataModel = app.getDataModel();
            dataModel.setLatitude(latLng.latitude);
            dataModel.setLongitude(latLng.longitude);
            dataModel.setLocationName(city);
            dataModel.setFinished(true);
            app.setDataModel(dataModel);
            // Add new marker at the current location
            currentLocationMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(city));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

        } catch (IOException e) {
            Log.e("MAPS_ACTIVITY", "Geocoder IOException", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates when the activity is paused to save resources
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume location updates when the activity is active
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }
    private void loadImageWithGlide(String url, ImageView imageView) {
        Glide.with(this)
                .load(url)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        imageView.setBackground(resource); // Set as background
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Handle case when the image is cleared
                    }
                });
    }
}
