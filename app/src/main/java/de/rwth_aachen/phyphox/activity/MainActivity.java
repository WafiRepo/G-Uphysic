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


        db.collection("record")
                .orderBy("id", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, String> userNameCache = new HashMap<>();
                    List<DocumentSnapshot> docs = snapshot.getDocuments();
                    final int[] counter = {0};
                    allArtifacts.clear();

                    if (docs.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    for (DocumentSnapshot doc : docs) {
                        String photoUrl = doc.getString("photo");
                        String location = doc.getString("locationName");
                        String idCustomer = doc.getString("idCustomer");

                        if (photoUrl != null && photoUrl.contains(".googleapis.com") &&
                                location != null && !location.isEmpty() &&
                                idCustomer != null && !idCustomer.isEmpty()) {

                            if (userNameCache.containsKey(idCustomer)) {
                                String userName = userNameCache.get(idCustomer);
                                if (userName != null && !userName.isEmpty()) {
                                    allArtifacts.add(new ArtifactSliderAdapter.ArtifactItem(photoUrl, location, userName, ""));
                                }
                                counter[0]++;
                                if (counter[0] == docs.size()) {
                                    adapter.notifyDataSetChanged();
                                }
                                continue;
                            }

                            db.collection("user").document(idCustomer).get().addOnSuccessListener(userDoc -> {
                                String userName = userDoc.getString("name");
                                if (userName != null && !userName.isEmpty()) {
                                    userNameCache.put(idCustomer, userName);
                                    allArtifacts.add(new ArtifactSliderAdapter.ArtifactItem(photoUrl, location, userName, ""));
                                }
                                counter[0]++;
                                if (counter[0] == docs.size()) {
                                    adapter.notifyDataSetChanged();
                                }
                            }).addOnFailureListener(e -> {
                                counter[0]++;
                                if (counter[0] == docs.size()) {
                                    adapter.notifyDataSetChanged();
                                }
                            });

                        } else {
                            counter[0]++;
                            if (counter[0] == docs.size()) {
                                adapter.notifyDataSetChanged();
                            }
                        }
                    }
                    autoScrollHandler.postDelayed(autoScrollRunnable, 3000);

                });
    }

}
