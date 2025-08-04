package de.rwth_aachen.phyphox.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.firestore.FirebaseFirestore;

import de.rwth_aachen.phyphox.App;
import de.rwth_aachen.phyphox.Helper.FirestoreUtil;
import de.rwth_aachen.phyphox.databinding.ActivityRecordPreviewBinding;
import de.rwth_aachen.phyphox.model.DataModel;

public class RecordPreviewActivity extends AppCompatActivity implements LocationListener {

    private ActivityRecordPreviewBinding binding;
    private DataModel dataModel;
    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecordPreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("📋 Preview Rekam");
        }

        // Get DataModel from App singleton
        App app = (App) getApplication();
        dataModel = app.getDataModel();

        setupUI();
        setupBackToHomeButton();
        setupBackButton();
        
        // Auto save location when page opens
        autoSaveLocation();
    }

    private void setupUI() {
        if (dataModel == null) return;

        // Set question and type
        binding.tvQuestion.setText(dataModel.getQuestion());
        binding.tvType.setText(dataModel.getTypeData());

        // Load canvas drawing if available (preview of last canvas)
        if (dataModel.getPhotoDraw() != null && !dataModel.getPhotoDraw().isEmpty()) {
            binding.ivDraw.setVisibility(View.VISIBLE);
            loadImage(dataModel.getPhotoDraw().get(dataModel.getPhotoDraw().size() - 1), binding.ivDraw);
        } else {
            binding.ivDraw.setVisibility(View.GONE);
        }

        // Handle Graph Images
        if (dataModel.getBase64() != null && !dataModel.getBase64().isEmpty()) {
            loadImage(dataModel.getBase64(), binding.iv);
            binding.iv.setVisibility(View.VISIBLE);
        }
        if (dataModel.getBase64_2() != null && !dataModel.getBase64_2().isEmpty()) {
            loadImage(dataModel.getBase64_2(), binding.iv2);
            binding.iv2.setVisibility(View.VISIBLE);
        }
        if (dataModel.getBase64_3() != null && !dataModel.getBase64_3().isEmpty()) {
            loadImage(dataModel.getBase64_3(), binding.iv3);
            binding.iv3.setVisibility(View.VISIBLE);
        }
        if (dataModel.getBase64_4() != null && !dataModel.getBase64_4().isEmpty()) {
            loadImage(dataModel.getBase64_4(), binding.iv4);
            binding.iv4.setVisibility(View.VISIBLE);
        }
        if (dataModel.getBase64_5() != null && !dataModel.getBase64_5().isEmpty()) {
            loadImage(dataModel.getBase64_5(), binding.iv5);
            binding.iv5.setVisibility(View.VISIBLE);
        }
    }

    private void setupBackToHomeButton() {
        binding.btnBackToHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupBackButton() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void autoSaveLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 
                LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            try {
                // Try to get last known location first
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    saveLocationToFirestore(lastKnownLocation);
                }

                // Request location updates
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            } catch (SecurityException e) {
                Log.e("Location", "Error requesting location updates", e);
            }
        }
    }

    private void saveLocationToFirestore(Location location) {
        if (dataModel != null) {
            dataModel.setLatitude(location.getLatitude());
            dataModel.setLongitude(location.getLongitude());
            dataModel.setFinished(true);
            
            // Save to app
            App app = (App) getApplication();
            app.setDataModel(dataModel);
            
            // Save to Firestore
            FirestoreUtil.addOrUpdateDocument("record", dataModel.getId(), dataModel,
                () -> {
                    Log.d("Location", "Location saved successfully");
                    Toast.makeText(this, "Lokasi berhasil disimpan", Toast.LENGTH_SHORT).show();
                },
                e -> {
                    Log.e("Location", "Error saving location", e);
                    Toast.makeText(this, "Gagal menyimpan lokasi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        saveLocationToFirestore(location);
        // Stop location updates after getting the first location
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Required for LocationListener interface
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        // Required for LocationListener interface
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        // Required for LocationListener interface
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                autoSaveLocation();
            } else {
                Toast.makeText(this, "Izin lokasi diperlukan untuk menyimpan data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadImage(String url, android.widget.ImageView imageView) {
        if (url == null || url.isEmpty()) return;

        try {
            if (url.startsWith("http")) {
                // Load from URL
                Glide.with(this)
                    .load(url)
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            imageView.setImageDrawable(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // Handle cleared state
                        }
                    });
            } else {
                // Load from Base64
                byte[] decodedBytes = Base64.decode(url, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        } catch (Exception e) {
            Log.e("ImageLoading", "Error loading image", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }
} 