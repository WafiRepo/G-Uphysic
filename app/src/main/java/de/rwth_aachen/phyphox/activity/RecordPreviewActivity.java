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
            String canvasUrl = dataModel.getPhotoDraw().get(dataModel.getPhotoDraw().size() - 1);
            Log.d("PREVIEW_DEBUG", "Loading canvas drawing from: " + canvasUrl);
            loadImage(canvasUrl, binding.ivDraw);
        } else {
            binding.ivDraw.setVisibility(View.GONE);
            Log.d("PREVIEW_DEBUG", "No canvas drawing available");
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

        // Load documentation photo if available
        if (dataModel.getPhotoAnswer() != null && !dataModel.getPhotoAnswer().isEmpty()) {
            binding.llDocumentationPhotoPreview.setVisibility(View.VISIBLE);
            Log.d("PREVIEW_DEBUG", "Loading documentation photo (Base64 length: " + dataModel.getPhotoAnswer().length() + ")");
            loadImage(dataModel.getPhotoAnswer(), binding.ivDocumentationPhotoPreview);
            
            // Configure PhotoView for documentation photo
            if (binding.ivDocumentationPhotoPreview instanceof com.github.chrisbanes.photoview.PhotoView) {
                com.github.chrisbanes.photoview.PhotoView photoView = (com.github.chrisbanes.photoview.PhotoView) binding.ivDocumentationPhotoPreview;
                try {
                    photoView.setMaximumScale(4.0f);
                    photoView.setMediumScale(2.0f);
                    photoView.setMinimumScale(0.8f);
                    photoView.setZoomable(true);
                    Log.d("DOC_PHOTO_PREVIEW", "Documentation photo loaded in preview with zoom capability");
                } catch (Exception e) {
                    Log.e("DOC_PHOTO_PREVIEW", "Error setting zoom for documentation photo preview: " + e.getMessage());
                    photoView.setZoomable(true);
                }
            }
        } else {
            binding.llDocumentationPhotoPreview.setVisibility(View.GONE);
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
        Log.d("LOCATION_DEBUG", "=== AUTO SAVE LOCATION STARTED ===");
        
        // Check DataModel first
        if (dataModel == null) {
            Log.e("LOCATION_DEBUG", "DataModel is null - cannot save location");
            Toast.makeText(this, "❌ Error: Data tidak tersedia untuk menyimpan lokasi", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (dataModel.getId() == null || dataModel.getId().isEmpty()) {
            Log.e("LOCATION_DEBUG", "DataModel ID is null/empty - cannot save location");
            Toast.makeText(this, "❌ Error: ID data kosong", Toast.LENGTH_SHORT).show();
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
                saveLocationToFirestore(lastKnownLocation);
            } else {
                Log.d("LOCATION_DEBUG", "No last known location - requesting fresh location updates");
                Toast.makeText(this, "📍 Mendapatkan lokasi saat ini...", Toast.LENGTH_SHORT).show();
            }

            // Request location updates from best available provider
            if (isGPSEnabled) {
                Log.d("LOCATION_DEBUG", "Requesting GPS updates");
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
            } else if (isNetworkEnabled) {
                Log.d("LOCATION_DEBUG", "Requesting Network updates");
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 10, this);
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
        if (dataModel != null) {
            // Get location name using geocoding
            String locationName = getLocationName(location);
            
            dataModel.setLatitude(location.getLatitude());
            dataModel.setLongitude(location.getLongitude());
            dataModel.setLocationName(locationName); // THIS WAS MISSING!
            
            // Set status based on context - don't always set to true
            // Only set to true if this is a completed experiment, not a custom question
            if (dataModel.getDesc() != null && dataModel.getDesc().contains("Buat Pertanyaan Sendiri")) {
                // For custom questions: keep status as is (false = berlanjut)
                Log.d("LOCATION_SAVE", "Custom question detected - keeping status as berlanjut");
            } else {
                // For completed experiments: set status to selesai
                dataModel.setFinished(true);
                Log.d("LOCATION_SAVE", "Completed experiment - setting status to selesai");
            }
            
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
            
            // Save to Firestore
            FirestoreUtil.addOrUpdateDocument("record", dataModel.getId(), dataModel,
                () -> {
                    Log.d("Location", "Location saved successfully: " + locationName);
                    Toast.makeText(this, "📍 Lokasi tersimpan: " + locationName, Toast.LENGTH_SHORT).show();
                },
                e -> {
                    Log.e("Location", "Error saving location", e);
                    String errorMessage = e.getMessage();
                    if (errorMessage != null && errorMessage.contains("1MB")) {
                        Toast.makeText(this, "Data terlalu besar untuk disimpan. Silakan coba dengan gambar yang lebih kecil.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Gagal menyimpan lokasi: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
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
        
        saveLocationToFirestore(location);
        
        // Stop location updates after getting the first location
        if (locationManager != null) {
            locationManager.removeUpdates(this);
            Log.d("LOCATION_DEBUG", "Location updates stopped after successful location");
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
                autoSaveLocation();
            } else {
                Log.w("LOCATION_DEBUG", "Location permission DENIED");
                Toast.makeText(this, "❌ Izin lokasi diperlukan untuk menyimpan data. Lokasi tidak akan tersimpan.", Toast.LENGTH_LONG).show();
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