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
import android.location.Address;
import android.location.Geocoder;
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
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.databinding.ActivityRecordPreviewBinding;
import de.rwth_aachen.phyphox.model.DataModel;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class RecordPreviewActivity extends AppCompatActivity implements LocationListener {

    private ActivityRecordPreviewBinding binding;
    private DataModel dataModel;
    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private boolean isActivityActive = true; // Flag to check if activity is still active
    private boolean locationSaved = false; // Flag to prevent duplicate location saves

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
        if (dataModel == null) {
            Log.e("PREVIEW_UI", "DataModel is null!");
            return;
        }

        Log.d("PREVIEW_UI", "Setting up UI for record ID: " + dataModel.getId());

        // Set question and type
        binding.tvQuestion.setText(dataModel.getQuestion());
        binding.tvType.setText(dataModel.getTypeData());

        // Load canvas drawing if available (preview of last canvas)
        if (dataModel.getPhotoDraw() != null && !dataModel.getPhotoDraw().isEmpty()) {
            binding.ivDraw.setVisibility(View.VISIBLE);
            String canvasUrl = dataModel.getPhotoDraw().get(dataModel.getPhotoDraw().size() - 1);
            Log.d("PREVIEW_DEBUG", "Loading canvas drawing from: " + canvasUrl);
            loadImage(canvasUrl, binding.ivDraw);
        } else {
            binding.ivDraw.setVisibility(View.GONE);
            Log.d("PREVIEW_DEBUG", "No canvas drawing available");
        }

        // Load question images - prefer URL fields (questionImageUrl*), fallback to legacy base64*
        // Only display if there is a valid value
        Log.d("PREVIEW_DEBUG", "=== LOADING QUESTION IMAGES ===");
        
        // iv - same approach as Preview Jawaban
        String questionUrl1 = dataModel.getQuestionImageUrl1();
        String base64_1 = dataModel.getBase64();
        Log.d("PREVIEW_DEBUG", "questionImageUrl1: " + (questionUrl1 != null ? (questionUrl1.length() > 50 ? questionUrl1.substring(0, 50) + "..." : questionUrl1) : "null"));
        Log.d("PREVIEW_DEBUG", "base64_1: " + (base64_1 != null ? (base64_1.length() > 50 ? base64_1.substring(0, 50) + "..." : base64_1) : "null"));
        
        String img1 = null;
        if (questionUrl1 != null && !questionUrl1.trim().isEmpty()) {
            img1 = questionUrl1;
            Log.d("PREVIEW_DEBUG", "Using questionImageUrl1 for image 1");
        } else if (base64_1 != null && !base64_1.trim().isEmpty()) {
            img1 = base64_1;
            Log.d("PREVIEW_DEBUG", "Using base64_1 (legacy) for image 1");
        } else {
            Log.d("PREVIEW_DEBUG", "No image 1 data available");
        }
        
        if (img1 != null && !img1.trim().isEmpty() && binding.iv != null) {
            Log.d("PREVIEW_DEBUG", "Loading question image 1 - URL length: " + (img1.length() > 100 ? img1.substring(0, 100) + "..." : img1));
            binding.iv.setVisibility(View.VISIBLE);
            loadImage(img1, binding.iv);
        } else {
            Log.d("PREVIEW_DEBUG", "Hiding image 1 - img1: " + (img1 != null ? "not null but empty" : "null") + ", binding.iv: " + (binding.iv != null ? "not null" : "null"));
            if (binding.iv != null) {
                binding.iv.setVisibility(View.GONE);
            }
        }
        
        // iv2 - same approach as Preview Jawaban
        String questionUrl2 = dataModel.getQuestionImageUrl2();
        String base64_2 = dataModel.getBase64_2();
        String img2 = null;
        if (questionUrl2 != null && !questionUrl2.trim().isEmpty()) {
            img2 = questionUrl2;
            Log.d("PREVIEW_DEBUG", "Using questionImageUrl2 for image 2");
        } else if (base64_2 != null && !base64_2.trim().isEmpty()) {
            img2 = base64_2;
            Log.d("PREVIEW_DEBUG", "Using base64_2 (legacy) for image 2");
        }
        if (img2 != null && !img2.trim().isEmpty() && binding.iv2 != null) {
            Log.d("PREVIEW_DEBUG", "Loading question image 2");
            binding.iv2.setVisibility(View.VISIBLE);
            loadImage(img2, binding.iv2);
        } else {
            if (binding.iv2 != null) {
                binding.iv2.setVisibility(View.GONE);
            }
        }
        
        // iv3 - same approach as Preview Jawaban
        String questionUrl3 = dataModel.getQuestionImageUrl3();
        String base64_3 = dataModel.getBase64_3();
        String img3 = null;
        if (questionUrl3 != null && !questionUrl3.trim().isEmpty()) {
            img3 = questionUrl3;
        } else if (base64_3 != null && !base64_3.trim().isEmpty()) {
            img3 = base64_3;
        }
        if (img3 != null && !img3.trim().isEmpty() && binding.iv3 != null) {
            Log.d("PREVIEW_DEBUG", "Loading question image 3");
            binding.iv3.setVisibility(View.VISIBLE);
            loadImage(img3, binding.iv3);
        } else {
            if (binding.iv3 != null) {
                binding.iv3.setVisibility(View.GONE);
            }
        }
        
        // iv4 - same approach as Preview Jawaban
        String questionUrl4 = dataModel.getQuestionImageUrl4();
        String base64_4 = dataModel.getBase64_4();
        String img4 = null;
        if (questionUrl4 != null && !questionUrl4.trim().isEmpty()) {
            img4 = questionUrl4;
        } else if (base64_4 != null && !base64_4.trim().isEmpty()) {
            img4 = base64_4;
        }
        if (img4 != null && !img4.trim().isEmpty() && binding.iv4 != null) {
            Log.d("PREVIEW_DEBUG", "Loading question image 4");
            binding.iv4.setVisibility(View.VISIBLE);
            loadImage(img4, binding.iv4);
        } else {
            if (binding.iv4 != null) {
                binding.iv4.setVisibility(View.GONE);
            }
        }
        
        // iv5 - same approach as Preview Jawaban
        String questionUrl5 = dataModel.getQuestionImageUrl5();
        String base64_5 = dataModel.getBase64_5();
        String img5 = null;
        if (questionUrl5 != null && !questionUrl5.trim().isEmpty()) {
            img5 = questionUrl5;
        } else if (base64_5 != null && !base64_5.trim().isEmpty()) {
            img5 = base64_5;
        }
        if (img5 != null && !img5.trim().isEmpty() && binding.iv5 != null) {
            Log.d("PREVIEW_DEBUG", "Loading question image 5");
            binding.iv5.setVisibility(View.VISIBLE);
            loadImage(img5, binding.iv5);
        } else {
            if (binding.iv5 != null) {
                binding.iv5.setVisibility(View.GONE);
            }
        }
        
        Log.d("PREVIEW_DEBUG", "=== FINISHED LOADING QUESTION IMAGES ===");

        // Load documentation photo if available - ONLY from student's work (record collection), NOT from questions collection
        // Priority: photoAnswerUrl (Firebase Storage) > photoAnswer (Base64 from student)
        // Do NOT show photoAnswer from questions collection (that's a reference, not student's work)
        String photoAnswerUrl = null;
        String photoAnswerBase64 = null;
        
        // Only show photoAnswer if it's from the student's work (will be saved to record collection)
        // Check if photoAnswerUrl exists and is NOT from questions collection
        if (dataModel.getPhotoAnswerUrl() != null && !dataModel.getPhotoAnswerUrl().isEmpty()) {
            // Check if this is from questions collection by checking if photoAnswerPath contains "questions"
            // If photoAnswerPath is null or doesn't contain "questions", it's from student's work
            String photoAnswerPath = dataModel.getPhotoAnswerPath();
            if (photoAnswerPath == null || !photoAnswerPath.contains("questions")) {
                photoAnswerUrl = dataModel.getPhotoAnswerUrl();
                Log.d("PREVIEW_DEBUG", "Loading student's documentation photo from URL: " + photoAnswerUrl);
            } else {
                Log.d("PREVIEW_DEBUG", "Ignoring photoAnswerUrl from questions collection (reference, not student's work)");
            }
        }
        
        // Fallback to Base64 photoAnswer (student's work)
        if (photoAnswerUrl == null || photoAnswerUrl.isEmpty()) {
            photoAnswerBase64 = dataModel.getPhotoAnswer();
            if (photoAnswerBase64 != null && !photoAnswerBase64.isEmpty()) {
                Log.d("PREVIEW_DEBUG", "Loading student's documentation photo from Base64 (length: " + photoAnswerBase64.length() + ")");
            }
        }
        
        // Display photo if available (from student's work only)
        String photoToDisplay = (photoAnswerUrl != null && !photoAnswerUrl.isEmpty()) ? photoAnswerUrl : photoAnswerBase64;
        if (photoToDisplay != null && !photoToDisplay.isEmpty()) {
            binding.llDocumentationPhotoPreview.setVisibility(View.VISIBLE);
            loadImage(photoToDisplay, binding.ivDocumentationPhotoPreview);
            
            // Configure PhotoView for documentation photo
            if (binding.ivDocumentationPhotoPreview instanceof com.github.chrisbanes.photoview.PhotoView) {
                com.github.chrisbanes.photoview.PhotoView photoView = (com.github.chrisbanes.photoview.PhotoView) binding.ivDocumentationPhotoPreview;
                try {
                    photoView.setMaximumScale(4.0f);
                    photoView.setMediumScale(2.0f);
                    photoView.setMinimumScale(0.8f);
                    photoView.setZoomable(true);
                    Log.d("DOC_PHOTO_PREVIEW", "Student's documentation photo loaded in preview with zoom capability");
                } catch (Exception e) {
                    Log.e("DOC_PHOTO_PREVIEW", "Error setting zoom for documentation photo preview: " + e.getMessage());
                    photoView.setZoomable(true);
                }
            }
        } else {
            binding.llDocumentationPhotoPreview.setVisibility(View.GONE);
            Log.d("PREVIEW_DEBUG", "No student's documentation photo available (hiding section)");
        }
    }

    private void setupBackToHomeButton() {
        binding.btnBackToHome.setOnClickListener(v -> {
            // Mark activity as inactive to prevent any pending saves
            isActivityActive = false;
            
            // Just navigate to home, don't save (data already saved by autoSaveLocation if needed)
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupBackButton() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            // Mark activity as inactive to prevent any pending saves
            isActivityActive = false;
            finish();
        });
    }
    
    @Override
    public void onBackPressed() {
        // Mark activity as inactive to prevent any pending saves
        isActivityActive = false;
        
        // Navigate to home page (same behavior as "Kembali ke Beranda" button)
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void autoSaveLocation() {
        Log.d("LOCATION_DEBUG", "=== AUTO SAVE LOCATION STARTED ===");
        
        // Check if activity is still active
        if (!isActivityActive) {
            Log.d("LOCATION_DEBUG", "Activity is not active, skipping auto save location");
            return;
        }
        
        // Check DataModel first
        if (dataModel == null) {
            Log.e("LOCATION_DEBUG", "DataModel is null - cannot save location");
            Toast.makeText(this, "❌ Error: Data tidak tersedia untuk menyimpan lokasi", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // CRITICAL: Don't create new document if ID is empty
        if (dataModel.getId() == null || dataModel.getId().isEmpty()) {
            Log.w("LOCATION_DEBUG", "DataModel ID is null/empty - skipping auto save to prevent creating new document");
            // Don't show error, just skip silently
            return;
        }
        
        Log.d("LOCATION_DEBUG", "DataModel OK - ID: " + dataModel.getId());
        
        // Check permissions
        boolean hasFineLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarseLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        
        Log.d("LOCATION_DEBUG", "Permissions - Fine: " + hasFineLocation + ", Coarse: " + hasCoarseLocation);
        
        if (!hasFineLocation && !hasCoarseLocation) {
            Log.w("LOCATION_DEBUG", "No location permissions - requesting...");
            Toast.makeText(this, "🔐 Meminta izin lokasi...", Toast.LENGTH_SHORT).show();
            
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 
                LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            Log.e("LOCATION_DEBUG", "LocationManager is null");
            Toast.makeText(this, "❌ Error: Location service tidak tersedia", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if GPS is enabled
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
        Log.d("LOCATION_DEBUG", "Providers - GPS: " + isGPSEnabled + ", Network: " + isNetworkEnabled);
        
        if (!isGPSEnabled && !isNetworkEnabled) {
            Log.w("LOCATION_DEBUG", "No location providers enabled");
            Toast.makeText(this, "⚠️ GPS dan Network location disabled. Mohon aktifkan location service.", Toast.LENGTH_LONG).show();
        }

        try {
            // Try to get last known location first (try both providers)
            Location lastKnownLocation = null;
            
            if (isGPSEnabled) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Log.d("LOCATION_DEBUG", "GPS last known location: " + (lastKnownLocation != null ? "Found" : "Not found"));
            }
            
            if (lastKnownLocation == null && isNetworkEnabled) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                Log.d("LOCATION_DEBUG", "Network last known location: " + (lastKnownLocation != null ? "Found" : "Not found"));
            }
            
            if (lastKnownLocation != null) {
                Log.d("LOCATION_DEBUG", "Using last known location: " + lastKnownLocation.getLatitude() + ", " + lastKnownLocation.getLongitude());
                // Only save if activity is still active
                if (isActivityActive) {
                    saveLocationToFirestore(lastKnownLocation);
                }
            } else {
                Log.d("LOCATION_DEBUG", "No last known location - requesting fresh location updates");
                // Only request updates if activity is still active
                if (isActivityActive) {
                    Toast.makeText(this, "📍 Mendapatkan lokasi saat ini...", Toast.LENGTH_SHORT).show();
                }
            }

            // Request location updates from best available provider
            // Only if activity is still active
            if (isActivityActive) {
                if (isGPSEnabled) {
                    Log.d("LOCATION_DEBUG", "Requesting GPS updates");
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
                } else if (isNetworkEnabled) {
                    Log.d("LOCATION_DEBUG", "Requesting Network updates");
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 10, this);
                }
            }
            
        } catch (SecurityException e) {
            Log.e("LOCATION_DEBUG", "SecurityException: " + e.getMessage());
            Toast.makeText(this, "❌ Error: Permission ditolak - " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e("LOCATION_DEBUG", "Unexpected error: " + e.getMessage());
            Toast.makeText(this, "❌ Error getting location: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveLocationToFirestore(Location location) {
        // Check if activity is still active and location not already saved
        if (!isActivityActive) {
            Log.d("LOCATION_SAVE", "Activity is not active, skipping location save");
            return;
        }
        
        if (locationSaved) {
            Log.d("LOCATION_SAVE", "Location already saved, skipping duplicate save");
            return;
        }
        
        // CRITICAL: Don't create new document if ID is empty
        if (dataModel == null) {
            Log.w("LOCATION_SAVE", "DataModel is null, skipping save");
            return;
        }
        
        if (dataModel.getId() == null || dataModel.getId().trim().isEmpty()) {
            Log.w("LOCATION_SAVE", "DataModel ID is empty, skipping save to prevent creating new document");
            return;
        }
        
        if (dataModel != null) {
            // Check if location already exists and is the same (prevent unnecessary updates)
            if (dataModel.getLatitude() != 0.0 && dataModel.getLongitude() != 0.0) {
                // Calculate distance between existing and new location
                float[] results = new float[1];
                android.location.Location.distanceBetween(
                    dataModel.getLatitude(), dataModel.getLongitude(),
                    location.getLatitude(), location.getLongitude(),
                    results
                );
                float distanceInMeters = results[0];
                
                // If location is very close (within 10 meters), don't update
                if (distanceInMeters < 10) {
                    Log.d("LOCATION_SAVE", "Location is very close to existing location (" + distanceInMeters + "m), skipping update");
                    locationSaved = true; // Mark as saved to prevent future saves
                    return;
                }
            }
            
            // Get location name using geocoding
            String locationName = getLocationName(location);
            
            // Only update location fields, don't touch other fields
            dataModel.setLatitude(location.getLatitude());
            dataModel.setLongitude(location.getLongitude());
            dataModel.setLocationName(locationName);
            
            // Don't change status when just updating location
            // Status should only be set when explicitly saving from other activities
            
            Log.d("LOCATION_SAVE", "Location: " + locationName + " (" + location.getLatitude() + ", " + location.getLongitude() + ")");
            
            // Save to app
            App app = (App) getApplication();
            app.setDataModel(dataModel);
            
            // Validate and compress Base64 data before saving
            try {
                validateAndCompressDataModel(dataModel);
                Log.d("LOCATION_SAVE", "DataModel validated and compressed successfully");
            } catch (Exception e) {
                Log.e("LOCATION_SAVE", "Error validating/compressing data: " + e.getMessage());
                Toast.makeText(this, "Error: Data terlalu besar. " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            
            // CRITICAL: Check if document exists in Firestore before saving
            // Only update existing documents, don't create new ones in Preview Activity
            FirestoreUtil.getDocument("record", dataModel.getId(), DataModel.class,
                existingData -> {
                    // Only save if document already exists
                    if (existingData == null) {
                        Log.w("LOCATION_SAVE", "Document does not exist in Firestore, skipping save to prevent creating new document");
                        // Don't show error, just skip silently
                        return;
                    }
                    
                    // Document exists, proceed with save
                    // Mark as saved to prevent duplicate saves
                    locationSaved = true;
                    
                    // Save to Firestore with versioning and history tracking
                    String userId = SessionManager.getId(this);
                    String userName = SessionManager.getName(this);
                    
                    FirestoreUtil.addOrUpdateDocumentWithVersioning(
                        "record",
                        dataModel.getId(),
                        dataModel,
                        userId,
                        userName,
                        () -> {
                            if (isActivityActive) {
                                Log.d("Location", "Location saved successfully with versioning: " + locationName);
                                Toast.makeText(this, "📍 Lokasi tersimpan: " + locationName, Toast.LENGTH_SHORT).show();
                            }
                        },
                        e -> {
                            if (isActivityActive) {
                                Log.e("Location", "Error saving location", e);
                                String errorMessage = e.getMessage();
                                if (errorMessage != null && errorMessage.contains("1MB")) {
                                    Toast.makeText(this, "Data terlalu besar untuk disimpan. Silakan coba dengan gambar yang lebih kecil.", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this, "Gagal menyimpan lokasi: " + errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                },
                e -> {
                    // If get fails, don't create new document
                    Log.w("LOCATION_SAVE", "Error checking document existence: " + e.getMessage() + ", skipping save");
                }
            );
        }
    }
    
    /**
     * Get location name using geocoding (reverse geocoding)
     */
    private String getLocationName(Location location) {
        Log.d("GEOCODING_DEBUG", "=== REVERSE GEOCODING STARTED ===");
        Log.d("GEOCODING_DEBUG", "Input coordinates: " + location.getLatitude() + ", " + location.getLongitude());
        
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            
            // Check if geocoder is available
            if (!Geocoder.isPresent()) {
                Log.w("GEOCODING_DEBUG", "Geocoder not available on this device");
                String fallback = location.getLatitude() + ", " + location.getLongitude();
                Log.d("GEOCODING_DEBUG", "Using coordinate fallback: " + fallback);
                return fallback;
            }
            
            Log.d("GEOCODING_DEBUG", "Geocoder available - attempting reverse geocoding...");
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            
            Log.d("GEOCODING_DEBUG", "Geocoding result: " + (addresses != null ? addresses.size() : 0) + " addresses found");
            
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                String locationName = address.getAddressLine(0);
                
                Log.d("GEOCODING_DEBUG", "Address line 0: " + locationName);
                Log.d("GEOCODING_DEBUG", "Locality: " + address.getLocality());
                Log.d("GEOCODING_DEBUG", "Admin area: " + address.getAdminArea());
                Log.d("GEOCODING_DEBUG", "Country: " + address.getCountryName());
                
                if (locationName != null && !locationName.trim().isEmpty()) {
                    Log.d("GEOCODING_DEBUG", "✅ SUCCESS: Location name found: " + locationName);
                    return locationName;
                } else {
                    // Try alternative address fields
                    String fallbackName = address.getLocality();
                    if (fallbackName == null || fallbackName.trim().isEmpty()) {
                        fallbackName = address.getAdminArea();
                    }
                    if (fallbackName == null || fallbackName.trim().isEmpty()) {
                        fallbackName = address.getCountryName();
                    }
                    
                    if (fallbackName != null && !fallbackName.trim().isEmpty()) {
                        Log.d("GEOCODING_DEBUG", "✅ Using fallback name: " + fallbackName);
                        return fallbackName;
                    }
                }
            }
            
            Log.w("GEOCODING_DEBUG", "No address found, using coordinates");
            String coordString = location.getLatitude() + ", " + location.getLongitude();
            Log.d("GEOCODING_DEBUG", "Using coordinate string: " + coordString);
            return coordString;
            
        } catch (IOException e) {
            Log.e("GEOCODING_DEBUG", "IOException during geocoding: " + e.getMessage());
            String coordString = location.getLatitude() + ", " + location.getLongitude();
            Log.d("GEOCODING_DEBUG", "IOException fallback: " + coordString);
            return coordString;
        } catch (Exception e) {
            Log.e("GEOCODING_DEBUG", "Unexpected error during geocoding: " + e.getMessage());
            Log.d("GEOCODING_DEBUG", "Exception fallback: Unknown Location");
            return "Unknown Location";
        }
    }
    
    /**
     * Validate and compress Base64 data to ensure it fits within Firestore limits
     */
    private void validateAndCompressDataModel(DataModel dataModel) throws Exception {
        final int FIRESTORE_LIMIT = 900000; // ~900KB safety margin
        
        // Check and compress base64 fields
        String[] base64Fields = {
            dataModel.getBase64(),
            dataModel.getBase64_2(),
            dataModel.getBase64_3(),
            dataModel.getBase64_4(),
            dataModel.getBase64_5(),
            dataModel.getPhotoAnswer()
        };
        
        for (int i = 0; i < base64Fields.length; i++) {
            String base64Data = base64Fields[i];
            if (base64Data != null && base64Data.length() > FIRESTORE_LIMIT) {
                Log.w("BASE64_COMPRESS", "Base64 field " + (i + 1) + " is too large: " + base64Data.length() + " bytes, compressing...");
                
                // Compress the base64 data
                String compressedBase64 = compressBase64ForFirestore(base64Data);
                
                // Update the field with compressed data
                switch (i) {
                    case 0: dataModel.setBase64(compressedBase64); break;
                    case 1: dataModel.setBase64_2(compressedBase64); break;
                    case 2: dataModel.setBase64_3(compressedBase64); break;
                    case 3: dataModel.setBase64_4(compressedBase64); break;
                    case 4: dataModel.setBase64_5(compressedBase64); break;
                    case 5: dataModel.setPhotoAnswer(compressedBase64); break;
                }
                
                Log.d("BASE64_COMPRESS", "Field " + (i + 1) + " compressed from " + base64Data.length() + " to " + compressedBase64.length() + " bytes");
            }
        }
        
        // Check photoDraw ArrayList size
        if (dataModel.getPhotoDraw() != null) {
            java.util.ArrayList<String> photoDraw = dataModel.getPhotoDraw();
            int totalPhotoDrawSize = 0;
            for (String url : photoDraw) {
                if (url != null) {
                    totalPhotoDrawSize += url.length();
                }
            }
            
            Log.d("PHOTO_DRAW_CHECK", "PhotoDraw ArrayList has " + photoDraw.size() + " items, total size: " + totalPhotoDrawSize + " bytes");
            
            // If photoDraw is too large, limit the number of items
            if (totalPhotoDrawSize > FIRESTORE_LIMIT) {
                Log.w("PHOTO_DRAW_COMPRESS", "PhotoDraw ArrayList too large (" + totalPhotoDrawSize + " bytes), limiting items...");
                
                java.util.ArrayList<String> limitedPhotoDraw = new java.util.ArrayList<>();
                int currentSize = 0;
                
                for (String url : photoDraw) {
                    if (url != null && (currentSize + url.length()) < FIRESTORE_LIMIT) {
                        limitedPhotoDraw.add(url);
                        currentSize += url.length();
                    } else {
                        Log.w("PHOTO_DRAW_COMPRESS", "Skipping URL to stay under limit: " + (url != null ? url.substring(0, Math.min(50, url.length())) + "..." : "null"));
                        break;
                    }
                }
                
                dataModel.setPhotoDraw(limitedPhotoDraw);
                Log.d("PHOTO_DRAW_COMPRESS", "PhotoDraw reduced from " + photoDraw.size() + " to " + limitedPhotoDraw.size() + " items");
            }
        }
        
        // Check other potentially large string fields
        String[] otherFields = {
            dataModel.getPhoto(),
            dataModel.getQuestion(),
            dataModel.getDesc(),
            dataModel.getPhotoAcceleration()
        };
        
        String[] fieldNames = {"photo", "question", "desc", "photoAcceleration"};
        
        for (int i = 0; i < otherFields.length; i++) {
            String fieldData = otherFields[i];
            if (fieldData != null && fieldData.length() > FIRESTORE_LIMIT) {
                Log.w("FIELD_SIZE_CHECK", "Field " + fieldNames[i] + " is too large: " + fieldData.length() + " bytes");
                
                // For non-base64 fields, truncate with ellipsis
                String truncated = fieldData.substring(0, Math.min(FIRESTORE_LIMIT - 100, fieldData.length())) + "...[TRUNCATED]";
                
                switch (i) {
                    case 0: dataModel.setPhoto(truncated); break;
                    case 1: dataModel.setQuestion(truncated); break;
                    case 2: dataModel.setDesc(truncated); break;
                    case 3: dataModel.setPhotoAcceleration(truncated); break;
                }
                
                Log.d("FIELD_SIZE_CHECK", "Field " + fieldNames[i] + " truncated to " + truncated.length() + " bytes");
            }
        }
        
        Log.d("BASE64_VALIDATION", "All data validated and compressed successfully");

        // After individual compression, enforce total document size budget
        enforceFirestoreSizeBudget(dataModel);
    }
    
    /**
     * Compress base64 string to fit within Firestore limits
     */
    private String compressBase64ForFirestore(String base64String) {
        final int FIRESTORE_LIMIT = 900000; // ~900KB safety margin
        
        if (base64String == null || base64String.length() <= FIRESTORE_LIMIT) {
            return base64String;
        }
        
        try {
            // Remove Base64 headers if any
            String cleanBase64 = base64String;
            if (cleanBase64.contains(",")) {
                cleanBase64 = cleanBase64.split(",")[1];
            }
            cleanBase64 = cleanBase64.replaceAll("\\s+", "").trim();
            
            // Decode to bitmap
            byte[] decodedBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.NO_WRAP);
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            
            if (bitmap == null) {
                Log.e("BASE64_COMPRESS", "Failed to decode bitmap");
                return base64String; // Return original if can't decode
            }
            
            // Optimize bitmap for storage
            android.graphics.Bitmap optimizedBitmap = optimizeBitmapForStorage(bitmap);
            
            // Convert back to base64 with compression
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            int quality = 85;
            
            // Try different quality levels until size is acceptable
            do {
                baos.reset();
                optimizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, baos);
                quality -= 10;
            } while (baos.size() > FIRESTORE_LIMIT && quality > 10);
            
            String compressedBase64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP);
            
            // Clean up
            if (optimizedBitmap != bitmap) {
                optimizedBitmap.recycle();
            }
            bitmap.recycle();
            
            Log.d("BASE64_COMPRESS", "Compressed from " + base64String.length() + " to " + compressedBase64.length() + " bytes");
            return compressedBase64;
            
        } catch (Exception e) {
            Log.e("BASE64_COMPRESS", "Error compressing base64: " + e.getMessage());
            return base64String; // Return original if compression fails
        }
    }

    /**
     * Ensure the total serialized size of the document stays under Firestore's 1MB limit.
     * If still too large after basic compression, apply aggressive compression and
     * progressively drop lowest-priority Base64 fields.
     */
    private void enforceFirestoreSizeBudget(DataModel dataModel) {
        final int HARD_LIMIT = 800_000; // Strongly below 1,048,576 to be safe
        final int SOFT_LIMIT = 700_000;   // Aggressive preferred target

        int estimatedSize = estimateDocumentSizeBytes(dataModel);
        Log.d("DOC_SIZE", "Estimated document size before enforcement: " + estimatedSize + " bytes");

        if (estimatedSize <= SOFT_LIMIT) {
            return;
        }

        // Apply aggressive compression first on largest Base64 fields
        String[] fields = new String[] {
                dataModel.getBase64(),
                dataModel.getBase64_2(),
                dataModel.getBase64_3(),
                dataModel.getBase64_4(),
                dataModel.getBase64_5(),
                dataModel.getPhotoAnswer()
        };

        // Try aggressive compression to ~80KB each to leave more room for other fields
        final int TARGET_PER_IMAGE = 80_000;
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] != null && fields[i].length() > 0) {
                String compressed = compressBase64ToTargetBytes(fields[i], TARGET_PER_IMAGE);
                switch (i) {
                    case 0: dataModel.setBase64(compressed); break;
                    case 1: dataModel.setBase64_2(compressed); break;
                    case 2: dataModel.setBase64_3(compressed); break;
                    case 3: dataModel.setBase64_4(compressed); break;
                    case 4: dataModel.setBase64_5(compressed); break;
                    case 5: dataModel.setPhotoAnswer(compressed); break;
                }
            }
        }

        estimatedSize = estimateDocumentSizeBytes(dataModel);
        Log.d("DOC_SIZE", "Estimated size after aggressive compression: " + estimatedSize + " bytes");
        if (estimatedSize <= SOFT_LIMIT) {
            return;
        }

        // Drop lowest-priority Base64 fields until under limit
        // Priority order (low to high): base64_5, base64_4, base64_3, base64_2, base64, photoAnswer
        Runnable[] droppers = new Runnable[] {
                () -> dataModel.setBase64_5(""),
                () -> dataModel.setBase64_4(""),
                () -> dataModel.setBase64_3(""),
                () -> dataModel.setBase64_2(""),
                () -> dataModel.setBase64(""),
                () -> dataModel.setPhotoAnswer("")
        };

        String[] dropNames = new String[] {"base64_5","base64_4","base64_3","base64_2","base64","photoAnswer"};

        for (int i = 0; i < droppers.length; i++) {
            if (estimateDocumentSizeBytes(dataModel) <= SOFT_LIMIT) break;
            droppers[i].run();
            Log.w("DOC_SIZE", "Dropping field to fit size budget: " + dropNames[i]);
        }

        estimatedSize = estimateDocumentSizeBytes(dataModel);
        Log.d("DOC_SIZE", "Estimated size after dropping fields: " + estimatedSize + " bytes");

        if (estimatedSize > SOFT_LIMIT) {
            // Limit photoDraw list (keep latest few items within budget)
            limitPhotoDrawToBudget(dataModel, 80_000, 2);
            estimatedSize = estimateDocumentSizeBytes(dataModel);
            Log.w("DOC_SIZE", "Estimated size after limiting photoDraw: " + estimatedSize + " bytes");
        }

        if (estimatedSize > SOFT_LIMIT) {
            // Trim long textual fields to stay within budget
            trimStringFieldToMax(() -> dataModel.getQuestion(), s -> dataModel.setQuestion(s), 20_000, "question");
            trimStringFieldToMax(() -> dataModel.getDesc(), s -> dataModel.setDesc(s), 12_000, "desc");
            trimStringFieldToMax(() -> dataModel.getPhotoAcceleration(), s -> dataModel.setPhotoAcceleration(s), 12_000, "photoAcceleration");
            estimatedSize = estimateDocumentSizeBytes(dataModel);
            Log.w("DOC_SIZE", "Estimated size after trimming text fields: " + estimatedSize + " bytes");
        }

        // Final guard: ensure under hard limit; if still above, clear all Base64
        if (estimatedSize > HARD_LIMIT) {
            dataModel.setBase64("");
            dataModel.setBase64_2("");
            dataModel.setBase64_3("");
            dataModel.setBase64_4("");
            dataModel.setBase64_5("");
            dataModel.setPhotoAnswer("");
            // Keep only the last canvas URL to minimize size
            limitPhotoDrawToBudget(dataModel, 30_000, 1);
            // Aggressively trim textual fields
            trimStringFieldToMax(() -> dataModel.getQuestion(), s -> dataModel.setQuestion(s), 8_000, "question");
            trimStringFieldToMax(() -> dataModel.getDesc(), s -> dataModel.setDesc(s), 6_000, "desc");
            trimStringFieldToMax(() -> dataModel.getPhotoAcceleration(), s -> dataModel.setPhotoAcceleration(s), 6_000, "photoAcceleration");
            Log.e("DOC_SIZE", "Cleared heavy fields as last resort. New estimate: " + estimateDocumentSizeBytes(dataModel));
        }
    }

    /**
     * Estimate serialized size of Firestore document by summing lengths of major string fields.
     * This is an approximation but sufficient to avoid exceeding the 1MB limit.
     */
    private int estimateDocumentSizeBytes(DataModel dataModel) {
        int size = 0;
        java.util.function.Function<String, Integer> len = s -> s == null ? 0 : s.length();

        size += len.apply(dataModel.getId());
        size += len.apply(dataModel.getIdCustomer());
        size += len.apply(dataModel.getCustomerName());
        size += len.apply(dataModel.getTypeData());
        size += len.apply(dataModel.getQuestion());
        size += len.apply(dataModel.getDesc());
        size += len.apply(dataModel.getPhoto());
        size += len.apply(dataModel.getPhotoAcceleration());

        size += len.apply(dataModel.getBase64());
        size += len.apply(dataModel.getBase64_2());
        size += len.apply(dataModel.getBase64_3());
        size += len.apply(dataModel.getBase64_4());
        size += len.apply(dataModel.getBase64_5());
        size += len.apply(dataModel.getPhotoAnswer());

        if (dataModel.getPhotoDraw() != null) {
            for (String url : dataModel.getPhotoDraw()) {
                size += len.apply(url);
            }
        }

        // Add small overhead for keys/metadata
        size += 2048;
        return size;
    }

    private void limitPhotoDrawToBudget(DataModel dataModel, int budgetBytes, int keepLastCount) {
        if (dataModel.getPhotoDraw() == null || dataModel.getPhotoDraw().isEmpty()) return;
        java.util.ArrayList<String> original = dataModel.getPhotoDraw();
        java.util.ArrayList<String> limited = new java.util.ArrayList<>();
        int total = 0;
        // Keep last N items first
        int start = Math.max(0, original.size() - keepLastCount);
        for (int i = start; i < original.size(); i++) {
            String url = original.get(i);
            if (url == null) continue;
            if (total + url.length() > budgetBytes) break;
            limited.add(url);
            total += url.length();
        }
        dataModel.setPhotoDraw(limited);
        Log.w("PHOTO_DRAW_LIMIT", "photoDraw reduced to " + limited.size() + " items within budget " + budgetBytes + " bytes");
    }

    private interface Getter { String get(); }
    private interface Setter { void set(String v); }
    private void trimStringFieldToMax(Getter getter, Setter setter, int maxBytes, String fieldName) {
        String v = getter.get();
        if (v == null) return;
        if (v.length() <= maxBytes) return;
        String truncated = v.substring(0, Math.min(maxBytes - 20, v.length())) + "...[TRUNCATED]";
        setter.set(truncated);
        Log.w("FIELD_TRIM", fieldName + " trimmed to " + truncated.length() + " bytes");
    }

    /**
     * Aggressively compress a Base64 image to target byte size by reducing quality and scaling.
     */
    private String compressBase64ToTargetBytes(String base64String, int targetBytes) {
        if (base64String == null || base64String.isEmpty()) return base64String;
        try {
            String clean = base64String;
            if (clean.contains(",")) clean = clean.split(",")[1];
            clean = clean.replaceAll("\\s+", "").trim();

            byte[] decoded = android.util.Base64.decode(clean, android.util.Base64.NO_WRAP);
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            if (bitmap == null) return base64String;

            int attempt = 0;
            float scale = 1.0f;
            int quality = 70;
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

            while (attempt < 5) {
                baos.reset();
                android.graphics.Bitmap working = bitmap;
                if (scale < 1.0f) {
                    int w = Math.max(1, Math.round(bitmap.getWidth() * scale));
                    int h = Math.max(1, Math.round(bitmap.getHeight() * scale));
                    working = android.graphics.Bitmap.createScaledBitmap(bitmap, w, h, true);
                }

                working.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, baos);

                if (baos.size() <= targetBytes) {
                    String out = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP);
                    if (working != bitmap) working.recycle();
                    bitmap.recycle();
                    return out;
                }

                // Tighten: lower quality first, then scale down
                if (quality > 30) {
                    quality -= 15;
                } else {
                    scale *= 0.75f; // downscale progressively
                }

                if (working != bitmap) working.recycle();
                attempt++;
            }

            // Fallback return best-effort compressed even if over target
            String out = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP);
            bitmap.recycle();
            return out;
        } catch (Exception e) {
            Log.e("BASE64_COMPRESS", "Aggressive compression failed: " + e.getMessage());
            return base64String;
        }
    }
    
    /**
     * Optimize bitmap for storage (resize and format conversion)
     */
    private android.graphics.Bitmap optimizeBitmapForStorage(android.graphics.Bitmap originalBitmap) {
        final int maxDimension = 1200; // Max width or height
        
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();
        
        // Calculate scale factor
        float scale = Math.min((float) maxDimension / width, (float) maxDimension / height);
        
        if (scale >= 1.0f) {
            // No need to resize, but ensure RGB_565 format for smaller size
            if (originalBitmap.getConfig() != android.graphics.Bitmap.Config.RGB_565) {
                return originalBitmap.copy(android.graphics.Bitmap.Config.RGB_565, false);
            }
            return originalBitmap;
        }
        
        // Resize bitmap
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        
        return android.graphics.Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d("LOCATION_DEBUG", "=== NEW LOCATION RECEIVED ===");
        Log.d("LOCATION_DEBUG", "Location: " + location.getLatitude() + ", " + location.getLongitude());
        Log.d("LOCATION_DEBUG", "Accuracy: " + location.getAccuracy() + "m, Provider: " + location.getProvider());
        
        // Only save if activity is still active
        if (isActivityActive) {
            saveLocationToFirestore(location);
        } else {
            Log.d("LOCATION_DEBUG", "Activity is not active, skipping location save");
        }
        
        // Stop location updates after getting the first location
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
                Log.d("LOCATION_DEBUG", "Location updates stopped after successful location");
            } catch (SecurityException e) {
                Log.e("LOCATION_DEBUG", "SecurityException stopping location updates: " + e.getMessage());
            }
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
        Log.d("LOCATION_DEBUG", "Permission result - Request code: " + requestCode + ", Results length: " + grantResults.length);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("LOCATION_DEBUG", "Location permission GRANTED - retrying autoSaveLocation");
                Toast.makeText(this, "✅ Izin lokasi diberikan", Toast.LENGTH_SHORT).show();
                // Only retry if activity is still active
                if (isActivityActive) {
                    autoSaveLocation();
                }
            } else {
                Log.w("LOCATION_DEBUG", "Location permission DENIED");
                Toast.makeText(this, "❌ Izin lokasi diperlukan untuk menyimpan data. Lokasi tidak akan tersimpan.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadImage(String url, android.widget.ImageView imageView) {
        android.util.Log.d("ImageLoading", "=== loadImage() START ===");
        android.util.Log.d("ImageLoading", "URL: " + (url != null ? (url.length() > 100 ? url.substring(0, 100) + "..." : url) : "null"));
        android.util.Log.d("ImageLoading", "ImageView: " + (imageView != null ? "not null" : "null"));
        
        if (url == null || url.isEmpty()) {
            android.util.Log.w("ImageLoading", "URL is null or empty - RETURNING");
            return;
        }
        
        if (imageView == null) {
            android.util.Log.e("ImageLoading", "ImageView is null - RETURNING");
            return;
        }

        try {
            if (url.startsWith("http")) {
                // Load from URL - use direct ImageView loading for better compatibility
                android.util.Log.d("ImageLoading", "URL starts with http - loading from URL");
                android.util.Log.d("ImageLoading", "Full URL: " + url);
                android.util.Log.d("ImageLoading", "Context: " + (this != null ? "not null" : "null"));
                android.util.Log.d("ImageLoading", "ImageView visibility: " + (imageView.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE/HIDDEN"));
                android.util.Log.d("ImageLoading", "ImageView dimensions: " + imageView.getWidth() + "x" + imageView.getHeight());
                
                // Ensure ImageView is visible before loading
                imageView.setVisibility(View.VISIBLE);
                
                android.util.Log.d("ImageLoading", "About to call Glide.with()");
                com.bumptech.glide.Glide.with(this)
                    .load(url)
                    .placeholder(android.R.drawable.ic_menu_gallery) // Placeholder while loading
                    .error(android.R.drawable.ic_dialog_alert) // Error placeholder
                    .listener(new com.bumptech.glide.request.RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<Drawable> target, boolean isFirstResource) {
                            android.util.Log.e("ImageLoading", "Glide onLoadFailed - URL: " + url.substring(0, Math.min(100, url.length())) + "...", e);
                            if (e != null && e.getRootCauses() != null) {
                                for (Throwable cause : e.getRootCauses()) {
                                    android.util.Log.e("ImageLoading", "Root cause: " + cause.getMessage());
                                }
                            }
                            imageView.setVisibility(View.VISIBLE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, com.bumptech.glide.request.target.Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            android.util.Log.d("ImageLoading", "Glide onResourceReady - size: " + resource.getIntrinsicWidth() + "x" + resource.getIntrinsicHeight());
                            return false; // Let Glide handle setting the drawable
                        }
                    })
                    .into(imageView); // Load directly into ImageView
                
                android.util.Log.d("ImageLoading", "Glide.into() called successfully");
            } else {
                // Load from Base64
                Log.d("ImageLoading", "Loading image from Base64 - Length: " + url.length());
                
                // Clean Base64 string - remove data URI prefix if present
                String cleanBase64 = url;
                if (cleanBase64.contains(",")) {
                    cleanBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);
                    Log.d("ImageLoading", "Removed data URI prefix, new length: " + cleanBase64.length());
                }
                
                // Remove whitespace
                cleanBase64 = cleanBase64.replaceAll("\\s+", "").trim();

                // Remove surrounding quotes if any (defensive)
                if (cleanBase64.length() >= 2 && cleanBase64.startsWith("\"") && cleanBase64.endsWith("\"")) {
                    cleanBase64 = cleanBase64.substring(1, cleanBase64.length() - 1).trim();
                    Log.d("ImageLoading", "Removed surrounding quotes, new length: " + cleanBase64.length());
                }

                // Pad to multiple of 4 if needed (defensive; helps when '=' stripped)
                int mod = cleanBase64.length() % 4;
                if (mod != 0) {
                    int pad = 4 - mod;
                    StringBuilder sb = new StringBuilder(cleanBase64.length() + pad);
                    sb.append(cleanBase64);
                    for (int i = 0; i < pad; i++) sb.append('=');
                    cleanBase64 = sb.toString();
                    Log.w("ImageLoading", "Padded Base64 with " + pad + " '=' chars to fix length%4, new length: " + cleanBase64.length());
                }
                
                try {
                    // Try multiple decode flags (NO_WRAP/DEFAULT/URL_SAFE) for robustness
                    byte[] decodedBytes = null;
                    Bitmap bitmap = null;

                    int[] flagsToTry = new int[] { Base64.NO_WRAP, Base64.DEFAULT, Base64.URL_SAFE };
                    for (int i = 0; i < flagsToTry.length; i++) {
                        try {
                            decodedBytes = Base64.decode(cleanBase64, flagsToTry[i]);
                            Log.d("ImageLoading", "Decoded Base64 with flag=" + flagsToTry[i] + " to " + (decodedBytes != null ? decodedBytes.length : 0) + " bytes");
                            if (decodedBytes != null && decodedBytes.length > 0) {
                                // Inspect image header first
                                BitmapFactory.Options bounds = new BitmapFactory.Options();
                                bounds.inJustDecodeBounds = true;
                                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length, bounds);
                                Log.d("ImageLoading", "Bounds decode (flag=" + flagsToTry[i] + "): mime=" + bounds.outMimeType + ", w=" + bounds.outWidth + ", h=" + bounds.outHeight);

                                // First try decodeByteArray
                                BitmapFactory.Options opts = new BitmapFactory.Options();
                                opts.inPreferredConfig = Bitmap.Config.RGB_565; // lower memory
                                bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length, opts);

                                // If that fails, try decodeStream (sometimes more tolerant)
                                if (bitmap == null) {
                                    java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(decodedBytes);
                                    bitmap = BitmapFactory.decodeStream(bais, null, opts);
                                }

                                Log.d("ImageLoading", "Bitmap decode result (flag=" + flagsToTry[i] + "): " + (bitmap != null ? ("OK " + bitmap.getWidth() + "x" + bitmap.getHeight()) : "NULL"));
                            }
                        } catch (IllegalArgumentException ignore) {
                            Log.w("ImageLoading", "Base64 decode failed with flag=" + flagsToTry[i] + ": " + ignore.getMessage());
                        }

                        if (bitmap != null) break;
                    }
                    
                    if (bitmap != null) {
                        // Make final copy for use in inner class
                        final Bitmap finalBitmap = bitmap;
                        final android.widget.ImageView finalImageView = imageView;
                        // Use Activity UI thread to ensure render
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                finalImageView.setImageBitmap(finalBitmap);
                                // Clear placeholder background so it doesn't visually dominate
                                finalImageView.setBackground(null);
                                finalImageView.setVisibility(View.VISIBLE);
                                finalImageView.requestLayout();
                                finalImageView.invalidate();
                                String viewName = "";
                                try {
                                    viewName = getResources().getResourceEntryName(finalImageView.getId());
                                } catch (Exception ignore) {}
                                Log.d("ImageLoading", "SET bitmap to ImageView OK - Bitmap: " + finalBitmap.getWidth() + "x" + finalBitmap.getHeight() +
                                        ", viewId=" + finalImageView.getId() +
                                        ", viewName=" + viewName +
                                        ", visibility=" + (finalImageView.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
                            }
                        });
                    } else {
                        Log.e("ImageLoading", "Failed to decode bitmap from Base64 after trying multiple flags. Base64 length=" + cleanBase64.length());
                    }
                } catch (IllegalArgumentException e) {
                    Log.e("ImageLoading", "IllegalArgumentException decoding Base64: " + e.getMessage());
                    // Try with DEFAULT flag as fallback
                    try {
                        byte[] decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap);
                            imageView.setVisibility(View.VISIBLE);
                            Log.d("ImageLoading", "Image loaded successfully with DEFAULT flag after IllegalArgumentException");
                        }
                    } catch (Exception e2) {
                        Log.e("ImageLoading", "Error loading image with DEFAULT flag: " + e2.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            Log.e("ImageLoading", "Error loading image: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Mark activity as inactive when paused to prevent saves
        isActivityActive = false;
        
        // Stop location updates immediately when paused
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
                Log.d("LOCATION_DEBUG", "Location updates stopped in onPause()");
            } catch (SecurityException e) {
                Log.e("LOCATION_DEBUG", "SecurityException stopping location updates: " + e.getMessage());
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Mark activity as inactive
        isActivityActive = false;
        
        // Stop location updates
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
                Log.d("LOCATION_DEBUG", "Location updates stopped in onDestroy()");
            } catch (SecurityException e) {
                Log.e("LOCATION_DEBUG", "SecurityException stopping location updates: " + e.getMessage());
            }
        }
    }
} 