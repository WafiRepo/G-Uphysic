package de.rwth_aachen.phyphox.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.rwth_aachen.phyphox.App;
import de.rwth_aachen.phyphox.Helper.FirestoreUtil;
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.databinding.ActivityImageCarouselBinding;
import de.rwth_aachen.phyphox.model.DataModel;

public class ImageCarouselActivity extends AppCompatActivity implements LocationListener {

    private ActivityImageCarouselBinding binding;
    private ImageCarouselAdapter adapter;
    private DataModel dataModel;
    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private boolean isActivityActive = true; // Flag to check if activity is still active
    private boolean locationSaved = false; // Flag to prevent duplicate location saves

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageCarouselBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("📸 Galeri Gambar AI");
        }

        // Get DataModel from App
        App app = (App) getApplication();
        dataModel = app.getDataModel();

        setupCarousel();
        setupBackButton();
        setupBackToHomeButton();
        setupViewDetailButton();
        
        // Auto save location when page opens
        autoSaveLocation();
    }

    private void setupCarousel() {
        List<ImageItem> imageItems = collectImages();
        
        Log.d("CAROUSEL", "Setting up carousel with " + imageItems.size() + " items");
        
        if (imageItems.isEmpty()) {
            binding.tvNoImages.setVisibility(View.VISIBLE);
            binding.recyclerViewCarousel.setVisibility(View.GONE);
            binding.tvImageCounter.setVisibility(View.GONE);
            Log.d("CAROUSEL", "No images found, showing no images message");
            return;
        }

        binding.tvNoImages.setVisibility(View.GONE);
        binding.recyclerViewCarousel.setVisibility(View.VISIBLE);
        binding.tvImageCounter.setVisibility(View.VISIBLE);

        adapter = new ImageCarouselAdapter(this, imageItems);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        
        Log.d("CAROUSEL", "Setting up RecyclerView...");
        Log.d("CAROUSEL", "RecyclerView object: " + binding.recyclerViewCarousel);
        Log.d("CAROUSEL", "Adapter object: " + adapter);
        Log.d("CAROUSEL", "LayoutManager object: " + layoutManager);
        
        binding.recyclerViewCarousel.setLayoutManager(layoutManager);
        binding.recyclerViewCarousel.setAdapter(adapter);
        
        Log.d("CAROUSEL", "RecyclerView setup completed");
        
        // Immediate check
        Log.d("CAROUSEL", "Immediate adapter item count: " + adapter.getItemCount());
        Log.d("CAROUSEL", "RecyclerView adapter: " + binding.recyclerViewCarousel.getAdapter());
        
        // Force layout and visibility check
        binding.recyclerViewCarousel.post(() -> {
            Log.d("CAROUSEL", "POST: RecyclerView width: " + binding.recyclerViewCarousel.getWidth());
            Log.d("CAROUSEL", "POST: RecyclerView height: " + binding.recyclerViewCarousel.getHeight());
            Log.d("CAROUSEL", "POST: RecyclerView visibility: " + binding.recyclerViewCarousel.getVisibility());
            Log.d("CAROUSEL", "POST: RecyclerView child count: " + binding.recyclerViewCarousel.getChildCount());
            Log.d("CAROUSEL", "POST: Adapter item count: " + binding.recyclerViewCarousel.getAdapter().getItemCount());
        });

        // Add snap helper for smooth scrolling
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(binding.recyclerViewCarousel);
        
        Log.d("CAROUSEL", "Carousel setup completed successfully");

        // Update counter
        updateImageCounter(0, imageItems.size());

        // Add scroll listener for counter
        binding.recyclerViewCarousel.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        int position = layoutManager.findFirstCompletelyVisibleItemPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            updateImageCounter(position, imageItems.size());
                        }
                    }
                }
            }
        });
    }

    private void updateImageCounter(int current, int total) {
        binding.tvImageCounter.setText((current + 1) + " / " + total);
    }

    private List<ImageItem> collectImages() {
        List<ImageItem> imageItems = new ArrayList<>();
        
        Log.d("CAROUSEL", "=== COLLECTING IMAGES ===");
        Log.d("CAROUSEL", "DataModel: " + (dataModel != null ? "exists" : "null"));
        
        if (dataModel == null) {
            Log.e("CAROUSEL", "DataModel is null!");
            return imageItems;
        }

        // Add question text as first item if available
        if (dataModel.getQuestion() != null && !dataModel.getQuestion().isEmpty()) {
            Log.d("CAROUSEL", "Adding question: " + dataModel.getQuestion());
            imageItems.add(new ImageItem("❓ Pertanyaan", "QUESTION_TEXT", "📝 " + dataModel.getQuestion()));
        } else {
            Log.d("CAROUSEL", "No question found");
        }

        // Add user input text if available (like from EditText)
        if (dataModel.getDesc() != null && !dataModel.getDesc().isEmpty()) {
            Log.d("CAROUSEL", "Adding user input: " + dataModel.getDesc());
            imageItems.add(new ImageItem("📝 Input User", "USER_INPUT_TEXT", "✍️ " + dataModel.getDesc()));
        } else {
            Log.d("CAROUSEL", "No user input found");
        }

        // Collect all base64 images from DataModel
        if (dataModel.getBase64() != null && !dataModel.getBase64().isEmpty()) {
            String base64Data = dataModel.getBase64();
            Log.d("CAROUSEL", "Adding base64 image - Length: " + base64Data.length() + " chars");
            Log.d("CAROUSEL", "Base64 format: " + (base64Data.startsWith("/9j/") ? "JPEG" : base64Data.startsWith("iVBOR") ? "PNG" : "Unknown"));
            imageItems.add(new ImageItem("Grafik Analisis", base64Data, "📊 Data dari analisis AI"));
        }
        
        if (dataModel.getBase64_2() != null && !dataModel.getBase64_2().isEmpty()) {
            String base64Data = dataModel.getBase64_2();
            Log.d("CAROUSEL", "Adding base64_2 image - Length: " + base64Data.length() + " chars");
            Log.d("CAROUSEL", "Base64_2 format: " + (base64Data.startsWith("/9j/") ? "JPEG" : base64Data.startsWith("iVBOR") ? "PNG" : "Unknown"));
            imageItems.add(new ImageItem("Gambar Utama", base64Data, "🎯 Visualisasi konsep fisika"));
        }
        
        if (dataModel.getBase64_3() != null && !dataModel.getBase64_3().isEmpty()) {
            String base64Data = dataModel.getBase64_3();
            Log.d("CAROUSEL", "Adding base64_3 image - Length: " + base64Data.length() + " chars");
            Log.d("CAROUSEL", "Base64_3 format: " + (base64Data.startsWith("/9j/") ? "JPEG" : base64Data.startsWith("iVBOR") ? "PNG" : "Unknown"));
            imageItems.add(new ImageItem("Tabel Data 1", base64Data, "📋 Informasi data eksperimen"));
        }
        
        if (dataModel.getBase64_4() != null && !dataModel.getBase64_4().isEmpty()) {
            String base64Data = dataModel.getBase64_4();
            Log.d("CAROUSEL", "Adding base64_4 image - Length: " + base64Data.length() + " chars");
            Log.d("CAROUSEL", "Base64_4 format: " + (base64Data.startsWith("/9j/") ? "JPEG" : base64Data.startsWith("iVBOR") ? "PNG" : "Unknown"));
            imageItems.add(new ImageItem("Tabel Data 2", base64Data, "📈 Analisis lanjutan"));
        }
        
        if (dataModel.getBase64_5() != null && !dataModel.getBase64_5().isEmpty()) {
            String base64Data = dataModel.getBase64_5();
            Log.d("CAROUSEL", "Adding base64_5 image - Length: " + base64Data.length() + " chars");
            Log.d("CAROUSEL", "Base64_5 format: " + (base64Data.startsWith("/9j/") ? "JPEG" : base64Data.startsWith("iVBOR") ? "PNG" : "Unknown"));
            imageItems.add(new ImageItem("Gambar Tambahan", base64Data, "💡 Insight tambahan"));
        }

        // Add drawing images from photoDraw list
        if (dataModel.getPhotoDraw() != null && !dataModel.getPhotoDraw().isEmpty()) {
            for (int i = 0; i < dataModel.getPhotoDraw().size(); i++) {
                String drawingUrl = dataModel.getPhotoDraw().get(i);
                if (drawingUrl != null && !drawingUrl.isEmpty()) {
                    Log.d("CAROUSEL", "Adding drawing URL - Length: " + drawingUrl.length() + " chars");
                    Log.d("CAROUSEL", "Drawing URL " + (i + 1) + ": " + drawingUrl);
                    imageItems.add(new ImageItem("Gambar Jawaban " + (i + 1), drawingUrl, "✍️ Hasil coretan dan jawaban Anda"));
                }
            }
        }

        Log.d("CAROUSEL", "Total collected items: " + imageItems.size());
        return imageItems;
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
        super.onBackPressed();
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

    private void setupViewDetailButton() {
        binding.btnViewDetail.setOnClickListener(v -> {
            // Navigate to DetailRecordActivity to show complete record details
            Intent intent = new Intent(this, DetailRecordActivity.class);
            startActivity(intent);
        });
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
                // Try GPS first
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastKnownLocation != null) {
                        updateLocationInDataModel(lastKnownLocation);
                    }
                }
                // Fallback to network
                else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (lastKnownLocation != null) {
                        updateLocationInDataModel(lastKnownLocation);
                    }
                }
            } catch (SecurityException e) {
                Log.e("LOCATION", "Security exception: " + e.getMessage());
            }
        }
    }

    private void updateLocationInDataModel(Location location) {
        // Check if activity is still active and location not already saved
        if (!isActivityActive) {
            Log.d("LOCATION_SAVE", "Activity is not active, skipping location save");
            return;
        }
        
        if (locationSaved) {
            Log.d("LOCATION_SAVE", "Location already saved, skipping duplicate save");
            return;
        }
        
        if (location != null) {
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
                    locationSaved = true;
                    return;
                }
            }
            
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                String city = (addresses != null && addresses.size() > 0) ? addresses.get(0).getAddressLine(0) : "Unknown Location";
                
                // Only update location fields, don't touch other fields
                dataModel.setLatitude(location.getLatitude());
                dataModel.setLongitude(location.getLongitude());
                dataModel.setLocationName(city);
                // Don't change status when just updating location
                
                // Save to app
                App app = (App) getApplication();
                app.setDataModel(dataModel);
                
                // Mark as saved to prevent duplicate saves
                locationSaved = true;
                
                // Auto save to Firestore
                if (dataModel.getId() != null && !dataModel.getId().isEmpty()) {
                    String userId = SessionManager.getId(this);
                    String userName = SessionManager.getName(this);
                    
                    FirestoreUtil.addOrUpdateDocumentWithVersioning("record", dataModel.getId(), dataModel,
                            userId, userName,
                            () -> {
                                if (isActivityActive) {
                                    Log.d("LOCATION", "Location auto-saved successfully: " + city);
                                    Toast.makeText(this, "📍 Lokasi tersimpan: " + city, Toast.LENGTH_SHORT).show();
                                }
                            },
                            e -> {
                                if (isActivityActive) {
                                    Log.e("LOCATION", "Failed to auto-save location: " + e.getMessage());
                                }
                            });
                }
                
                Log.d("LOCATION", "Location updated: " + city + " (" + location.getLatitude() + ", " + location.getLongitude() + ")");
            } catch (IOException e) {
                Log.e("LOCATION", "Geocoder error: " + e.getMessage());
            }
        }
    }

    // LocationListener methods
    @Override
    public void onLocationChanged(@NonNull Location location) {
        updateLocationInDataModel(location);
        // Stop location updates after first successful update
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {}

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                autoSaveLocation();
            } else {
                Toast.makeText(this, "Permission lokasi diperlukan untuk auto-save", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Mark activity as inactive when paused to prevent saves
        isActivityActive = false;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Mark activity as inactive
        isActivityActive = false;
        
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    // Inner class for image items
    public static class ImageItem {
        public String title;
        public String imageData;
        public String description;

        public ImageItem(String title, String imageData, String description) {
            this.title = title;
            this.imageData = imageData;
            this.description = description;
        }
    }

    // Adapter for carousel
    public static class ImageCarouselAdapter extends RecyclerView.Adapter<ImageCarouselAdapter.ViewHolder> {
        private Context context;
        private List<ImageItem> imageItems;

        public ImageCarouselAdapter(Context context, List<ImageItem> imageItems) {
            this.context = context;
            this.imageItems = imageItems;
            Log.d("CAROUSEL", "Adapter created with " + imageItems.size() + " items");
            for (int i = 0; i < imageItems.size(); i++) {
                ImageItem item = imageItems.get(i);
                String dataType = "";
                String dataInfo = "";
                
                if (item.imageData.equals("QUESTION_TEXT")) {
                    dataType = "TEXT";
                    dataInfo = "Question content";
                } else if (item.imageData.equals("USER_INPUT_TEXT")) {
                    dataType = "TEXT";
                    dataInfo = "User input content";
                } else if (item.imageData.startsWith("http")) {
                    dataType = "URL";
                    dataInfo = "Length: " + item.imageData.length() + " chars";
                    Log.d("CAROUSEL", "Full URL " + i + ": " + item.imageData);
                } else if (item.imageData.startsWith("data:image")) {
                    dataType = "DATA_URI";
                    dataInfo = "Length: " + item.imageData.length() + " chars";
                    Log.d("CAROUSEL", "Data URI " + i + " prefix: " + item.imageData.substring(0, Math.min(100, item.imageData.length())));
                } else if (item.imageData.startsWith("/9j/") || item.imageData.startsWith("iVBOR")) {
                    dataType = "BASE64";
                    dataInfo = "Length: " + item.imageData.length() + " chars, Format: " + 
                        (item.imageData.startsWith("/9j/") ? "JPEG" : "PNG");
                    Log.d("CAROUSEL", "Base64 " + i + " preview: " + item.imageData.substring(0, Math.min(100, item.imageData.length())));
                } else {
                    dataType = "UNKNOWN";
                    dataInfo = "Length: " + item.imageData.length() + " chars";
                    Log.d("CAROUSEL", "Unknown " + i + " preview: " + item.imageData.substring(0, Math.min(100, item.imageData.length())));
                }
                
                Log.d("CAROUSEL", "Item " + i + ": [" + dataType + "] " + item.title + " - " + dataInfo);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Log.d("CAROUSEL", "onCreateViewHolder called for viewType: " + viewType);
            View view = LayoutInflater.from(context).inflate(R.layout.item_image_carousel, parent, false);
            Log.d("CAROUSEL", "View inflated successfully: " + view);
            ViewHolder holder = new ViewHolder(view);
            Log.d("CAROUSEL", "ViewHolder created: " + holder);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ImageItem item = imageItems.get(position);
            
            Log.d("CAROUSEL", "onBindViewHolder called for position " + position + ": " + item.title);
            
            holder.tvTitle.setText(item.title);
            holder.tvDescription.setText(item.description);
            
            // Handle text items (question and user input)
            if (item.imageData.equals("QUESTION_TEXT") || item.imageData.equals("USER_INPUT_TEXT")) {
                // Hide image view and show text content
                holder.ivImage.setVisibility(View.GONE);
                holder.tvContent.setVisibility(View.VISIBLE);
                holder.tvDescription.setVisibility(View.VISIBLE);
                
                // Set the main content in tvContent
                holder.tvContent.setText(item.description);
                holder.tvDescription.setText("Geser untuk melihat konten lainnya");
                
                Log.d("CAROUSEL", "Displaying text item: " + item.title + " - " + item.description.substring(0, Math.min(100, item.description.length())));
                return;
            } else {
                // Show image view for actual images
                holder.ivImage.setVisibility(View.VISIBLE);
                holder.tvContent.setVisibility(View.GONE);
                holder.tvDescription.setVisibility(View.VISIBLE);
                Log.d("CAROUSEL", "Displaying image item: " + item.title);
            }
            
            // Load image
            if (item.imageData.startsWith("http")) {
                // URL image
                Log.d("CAROUSEL", "Loading URL image: " + item.imageData);
                Glide.with(context)
                    .load(item.imageData)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            Log.e("CAROUSEL", "Glide failed to load URL image: " + (e != null ? e.getMessage() : "Unknown error"));
                            return false;
                        }
                        
                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            Log.d("CAROUSEL", "Glide successfully loaded URL image");
                            return false;
                        }
                    })
                    .into(holder.ivImage);
            } else {
                // Base64 image
                Log.d("CAROUSEL", "Loading Base64 image, length: " + item.imageData.length());
                Log.d("CAROUSEL", "Base64 prefix: " + item.imageData.substring(0, Math.min(100, item.imageData.length())));
                
                try {
                    // Try different base64 decoding approaches
                    String base64Data = item.imageData;
                    
                    // Remove data URI prefix if present
                    if (base64Data.startsWith("data:image")) {
                        base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
                        Log.d("CAROUSEL", "Removed data URI prefix, new length: " + base64Data.length());
                    }
                    
                    // Decode to bitmap directly
                    byte[] decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    
                    if (bitmap != null) {
                        holder.ivImage.setImageBitmap(bitmap);
                        Log.d("CAROUSEL", "Successfully loaded base64 image directly. Size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                    } else {
                        Log.e("CAROUSEL", "Failed to decode base64 to bitmap");
                        holder.ivImage.setImageResource(R.drawable.ic_image_error);
                    }
                } catch (Exception e) {
                    Log.e("CAROUSEL", "Exception loading base64 image: " + e.getMessage());
                    holder.ivImage.setImageResource(R.drawable.ic_image_error);
                }
            }
        }

        @Override
        public int getItemCount() {
            int count = imageItems.size();
            Log.d("CAROUSEL", "getItemCount called: " + count);
            return count;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivImage;
            TextView tvTitle, tvDescription, tvContent;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivImage = itemView.findViewById(R.id.iv_carousel_image);
                tvTitle = itemView.findViewById(R.id.tv_carousel_title);
                tvDescription = itemView.findViewById(R.id.tv_carousel_description);
                tvContent = itemView.findViewById(R.id.tv_carousel_content);
            }
        }


    }
} 