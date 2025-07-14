package de.rwth_aachen.phyphox.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.R;

public class LinearIntroductionFragment extends Fragment {

    buttonClick click;
    private long visitStartTime;

    public interface buttonClick {
        void buttonClicked(View v);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        click = (buttonClick) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_angular_introduction, container, false);
        Button btnStart = view.findViewById(R.id.btn_start);
        visitStartTime = System.currentTimeMillis();
        btnStart.setOnClickListener(v -> {

                    updateTotalVisitingIntroduction(visitStartTime);
                    click.buttonClicked(v);
                }
        );

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void updateTotalVisitingIntroduction(long visitStartTimeMillis) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference userDocRef = firestore.collection("user").document(SessionManager.getId(requireContext()));

        long visitDuration = System.currentTimeMillis() - visitStartTimeMillis;

        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userDocRef);

            Long currentTotal = snapshot.getLong("totalVisitingIntroduction");
            if (currentTotal == null) currentTotal = 0L;
            Long newTotal = currentTotal + 1;

            Long totalDuration = snapshot.getLong("totalVisitDurationMillis");
            if (totalDuration == null) totalDuration = 0L;
            long newTotalDuration = totalDuration + visitDuration;

            // Convert durasi ke format readable
            String lastDurationFormatted = formatDuration(visitDuration);
            String totalDurationFormatted = formatDuration(newTotalDuration);

            // Update Firestore
            transaction.update(userDocRef, "totalVisitingIntroduction", newTotal);
            transaction.update(userDocRef, "totalVisitDurationMillis", newTotalDuration);
            transaction.update(userDocRef, "totalVisitDurationFormatted", totalDurationFormatted);

            // Simpan juga ke SessionManager
            SessionManager.setKeyVisitingInto(requireContext(), newTotal);
            SessionManager.setTotalVisitDuration(requireContext(), newTotalDuration);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d("Firestore", "Berhasil update durasi dan format waktu");
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Gagal update durasi", e);
        });
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d jam %d menit %d detik", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "%d menit %d detik", minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%d detik", seconds);
        }
    }
}
