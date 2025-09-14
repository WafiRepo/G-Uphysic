package de.rwth_aachen.phyphox.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.Menu;
import android.app.AlertDialog;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;
import android.view.inputmethod.EditorInfo;
import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.databinding.ActivityMainBinding;
import de.rwth_aachen.phyphox.fragment.HistoryRecordFragment;
import de.rwth_aachen.phyphox.fragment.HomeFragment;
import de.rwth_aachen.phyphox.fragment.ListUserQuestionsFragment;
import de.rwth_aachen.phyphox.fragment.SearchFragment;
import de.rwth_aachen.phyphox.fragment.ShareLocationFragment;
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.activity.ArtifactSliderAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    ActivityMainBinding binding;
    BottomNavigationView bottomNavigationView;

    private List<ArtifactSliderAdapter.ArtifactItem> allArtifacts = new ArrayList<>();
    private List<ArtifactSliderAdapter.ArtifactItem> filteredArtifacts = new ArrayList<>();
    private ArtifactSliderAdapter adapter;
    private String filterUserName = "";
    private static final int MENU_FILTER = 1001;
    private final android.os.Handler autoScrollHandler = new android.os.Handler();
    private Runnable autoScrollRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Aktifkan Toolbar agar menu bisa muncul
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        loadAllArtifacts();

        // Set listener untuk sinkronisasi slider
        historyRecordFragment.setOnRecordChangedListener(() -> loadAllArtifacts());

        BottomNavigationView bottomNavigationView = binding.bottomNavigationView;
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        bottomNavigationView.setSelectedItemId(R.id.home);

    }

    HomeFragment homeFragment = new HomeFragment();
    ListUserQuestionsFragment listUserQuestionsFragment = new ListUserQuestionsFragment();
    ShareLocationFragment searchFragment = new ShareLocationFragment();
    HistoryRecordFragment historyRecordFragment = new HistoryRecordFragment();

    @Override
    public boolean
    onNavigationItemSelected(@NonNull MenuItem item) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        ViewPager2 artifactSlider = findViewById(R.id.artifactSlider);
        ConstraintLayout clBanner = findViewById(R.id.clBanner);
        switch (item.getItemId()) {
            case R.id.home:
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.flFragment, homeFragment)
                        .commit();
                // Tampilkan toolbar dan slider
                toolbar.setVisibility(View.VISIBLE);
                clBanner.setVisibility(View.VISIBLE);
                return true;
            case R.id.search:
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.flFragment, listUserQuestionsFragment)
                        .commit();
                // Tampilkan toolbar dan slider
                toolbar.setVisibility(View.GONE);
                clBanner.setVisibility(View.GONE);
                return true;
            case R.id.leaderboard:
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.flFragment, historyRecordFragment)
                        .commit();
                // Sembunyikan toolbar dan slider
                toolbar.setVisibility(View.GONE);
                clBanner.setVisibility(View.GONE);
                return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.add(0, MENU_FILTER, 1, "Filter").setIcon(R.drawable.ic_filter_list).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            new AlertDialog.Builder(this)
                .setTitle("Pengaturan")
                .setItems(new CharSequence[]{"Logout"}, (dialog, which) -> {
                    if (which == 0) {
                        try { FirebaseAuth.getInstance().signOut(); } catch (Exception e) {}
                        SessionManager.clearData(this);
                        Intent intentLogin = new Intent(this, de.rwth_aachen.phyphox.activity.LoginActivity.class);
                        intentLogin.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intentLogin);
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
            return true;
        } else if (item.getItemId() == MENU_FILTER) {
            showFilterDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showFilterDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter, null);
        final AutoCompleteTextView etUserName = dialogView.findViewById(R.id.etUserName);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("user").get().addOnSuccessListener(snapshot -> {
            List<String> userNames = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String name = doc.getString("name");
                if (name != null && !name.isEmpty() && !userNames.contains(name)) {
                    userNames.add(name);
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, userNames);
            etUserName.setAdapter(adapter);
        });
        etUserName.setText(filterUserName);
        new AlertDialog.Builder(this)
            .setTitle("Filter Artifact")
            .setView(dialogView)
            .setPositiveButton("Terapkan", (d, w) -> {
                filterUserName = etUserName.getText().toString().trim();
                applyFilter();
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void applyFilter() {
        filteredArtifacts.clear();
        for (ArtifactSliderAdapter.ArtifactItem item : allArtifacts) {
            boolean matchUser = filterUserName.isEmpty() || item.userName.toLowerCase().contains(filterUserName.toLowerCase());
            if (matchUser) filteredArtifacts.add(item);
        }
        adapter.notifyDataSetChanged();
    }

    private void loadAllArtifacts() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        allArtifacts.clear();
        ViewPager2 artifactSlider = findViewById(R.id.artifactSlider);

        if (adapter == null) {
            adapter = new ArtifactSliderAdapter(this, allArtifacts);
            artifactSlider.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
        com.tbuonomo.viewpagerdotsindicator.DotsIndicator dotsIndicator = findViewById(R.id.dotsIndicator);
        dotsIndicator.setViewPager2(binding.artifactSlider);

// Auto-scroll setiap 3 detik
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (adapter != null && adapter.getItemCount() > 0) {
                    int nextItem = (binding.artifactSlider.getCurrentItem() + 1) % adapter.getItemCount();
                    binding.artifactSlider.setCurrentItem(nextItem, true);
                }
                autoScrollHandler.postDelayed(this, 3000);
            }
        };


        // Load artifacts from both "record" and "questions" collections
        loadArtifactsFromRecord();
    }
    
    private void loadArtifactsFromRecord() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("record")
                .orderBy("id", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snapshot -> {
                    android.util.Log.d("Slider", "Record collection loaded: " + snapshot.size() + " documents");
                    processArtifactDocuments(snapshot.getDocuments());
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("Slider", "Failed to load record collection: " + e.getMessage());
                    // Try loading from questions collection if record fails
                    loadArtifactsFromQuestions();
                });
    }
    
    private void loadArtifactsFromQuestions() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("questions")
                .orderBy("id", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snapshot -> {
                    android.util.Log.d("Slider", "Questions collection loaded: " + snapshot.size() + " documents");
                    processArtifactDocuments(snapshot.getDocuments());
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("Slider", "Failed to load questions collection: " + e.getMessage());
                    adapter.notifyDataSetChanged();
                });
    }
    
    private void processArtifactDocuments(List<DocumentSnapshot> docs) {
        android.util.Log.d("Slider", "Processing " + docs.size() + " documents for artifacts");
        
        Map<String, String> userNameCache = new HashMap<>();
        final int[] counter = {0};
        allArtifacts.clear();

        if (docs.isEmpty()) {
            android.util.Log.d("Slider", "No documents found");
            adapter.notifyDataSetChanged();
            return;
        }

        for (DocumentSnapshot doc : docs) {
            String photoUrl = doc.getString("photo");
            String location = doc.getString("locationName");
            String idCustomer = doc.getString("idCustomer");
            
            android.util.Log.d("Slider", "Document: " + doc.getId() + 
                ", photoUrl: " + (photoUrl != null && !photoUrl.trim().isEmpty() ? "exists" : "empty") + 
                ", location: " + (location != null ? location : "null") + 
                ", idCustomer: " + (idCustomer != null ? "exists" : "null"));

            // Enhanced debugging for photoUrl
            if (photoUrl != null && !photoUrl.trim().isEmpty()) {
                android.util.Log.d("Slider", "PhotoUrl details: " + photoUrl.substring(0, Math.min(100, photoUrl.length())) + 
                    (photoUrl.length() > 100 ? "..." : ""));
                android.util.Log.d("Slider", "✅ Photo field contains object detection image - using photo field");
            } else {
                android.util.Log.d("Slider", "❌ Photo field empty - NO object detection image available - skipping");
            }

            // Only show artifacts with object detection images (Firebase Storage URLs)
            if (photoUrl != null && !photoUrl.trim().isEmpty() &&
                    photoUrl.contains(".googleapis.com") &&
                    idCustomer != null && !idCustomer.isEmpty()) {
                
                // Use fallback location if empty
                String displayLocation = (location != null && !location.isEmpty()) ? 
                    location : "Unknown Location";
                
                android.util.Log.d("Slider", "Processing document with location: " + displayLocation);

                if (userNameCache.containsKey(idCustomer)) {
                    String userName = userNameCache.get(idCustomer);
                    if (userName != null && !userName.isEmpty()) {
                        allArtifacts.add(new ArtifactSliderAdapter.ArtifactItem(photoUrl, displayLocation, userName, ""));
                        android.util.Log.d("Slider", "Added artifact from cache: " + userName + " at " + displayLocation);
                    }
                    counter[0]++;
                    if (counter[0] == docs.size()) {
                        android.util.Log.d("Slider", "Item count: " + allArtifacts.size());
                        adapter.notifyDataSetChanged();
                        startAutoScroll();
                    }
                    continue;
                }

                // Make variables effectively final for lambda
                final String finalPhotoUrl = photoUrl;
                final String finalDisplayLocation = displayLocation;
                final String finalIdCustomer = idCustomer;
                
                FirebaseFirestore.getInstance().collection("user").document(finalIdCustomer).get()
                    .addOnSuccessListener(userDoc -> {
                        String userName = userDoc.getString("name");
                        if (userName != null && !userName.isEmpty()) {
                            userNameCache.put(finalIdCustomer, userName);
                            allArtifacts.add(new ArtifactSliderAdapter.ArtifactItem(finalPhotoUrl, finalDisplayLocation, userName, ""));
                            android.util.Log.d("Slider", "Added artifact: " + userName + " at " + finalDisplayLocation);
                        }
                        counter[0]++;
                        if (counter[0] == docs.size()) {
                            android.util.Log.d("Slider", "Item count: " + allArtifacts.size());
                            adapter.notifyDataSetChanged();
                            startAutoScroll();
                        }
                    }).addOnFailureListener(e -> {
                        android.util.Log.w("Slider", "Failed to load user: " + finalIdCustomer);
                        counter[0]++;
                        if (counter[0] == docs.size()) {
                            android.util.Log.d("Slider", "Item count: " + allArtifacts.size());
                            adapter.notifyDataSetChanged();
                            startAutoScroll();
                        }
                    });

            } else {
                android.util.Log.d("Slider", "Document doesn't meet criteria - skipping");
                counter[0]++;
                if (counter[0] == docs.size()) {
                    android.util.Log.d("Slider", "Item count: " + allArtifacts.size());
                    adapter.notifyDataSetChanged();
                    startAutoScroll();
                }
            }
        }
    }
    
    private void startAutoScroll() {
        if (allArtifacts.size() > 0) {
            autoScrollHandler.postDelayed(autoScrollRunnable, 3000);
        }
    }
}
