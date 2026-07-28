package de.rwth_aachen.phyphox.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Calendar;

import de.rwth_aachen.phyphox.App;
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.activity.IntroductionClassActivity;
import de.rwth_aachen.phyphox.activity.LinearActivity;
import de.rwth_aachen.phyphox.model.DataModel;
import de.rwth_aachen.phyphox.activity.PiAnalysisActivity;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get references to the main activity cards
        RelativeLayout rlOutClass = view.findViewById(R.id.rlOutClass);
        RelativeLayout rlInClass = view.findViewById(R.id.rlInClass);
        RelativeLayout rlControlGroup = view.findViewById(R.id.rlControlGroup);
        RelativeLayout rlPiAnalysis = view.findViewById(R.id.rlPiAnalysis);
        TextView tvName = view.findViewById(R.id.tvName);
        TextView tvGreeting = view.findViewById(R.id.tvGreeting);

        // Set user name
        if (tvName != null) {
            tvName.setText(SessionManager.getName(requireContext()));
        }

        // Set greeting based on time of day
        if (tvGreeting != null) {
            tvGreeting.setText(getGreetingBasedOnTime());
        }

        // Out of Class click handler
        if (rlOutClass != null) {
            rlOutClass.setOnClickListener(view1 -> {
                App app = (App) requireActivity().getApplication();
                DataModel dataModel = new DataModel();
                dataModel.setId(String.valueOf(System.currentTimeMillis()));
                dataModel.setTopics("SA3");
                app.setDataModel(dataModel);
                startActivity(new Intent(getActivity(), LinearActivity.class));
            });
        }

        // In Class click handler
        if (rlInClass != null) {
            rlInClass.setOnClickListener(view1 -> {
                App app = (App) requireActivity().getApplication();
                DataModel dataModel = new DataModel();
                dataModel.setId(String.valueOf(System.currentTimeMillis()));
                dataModel.setTopics("In Class");
                app.setDataModel(dataModel);
                startActivity(new Intent(requireContext(), IntroductionClassActivity.class));
            });
        }

        // Control Group click handler
        if (rlControlGroup != null) {
            rlControlGroup.setOnClickListener(view1 -> {
                App app = (App) requireActivity().getApplication();
                DataModel dataModel = new DataModel();
                dataModel.setId(String.valueOf(System.currentTimeMillis()));
                dataModel.setTopics("SA1");
                app.setDataModel(dataModel);
                startActivity(new Intent(getActivity(), LinearActivity.class));
            });
        }

        // Pi Analysis click handler
        if (rlPiAnalysis != null) {
            rlPiAnalysis.setOnClickListener(view1 -> {
                startActivity(new Intent(requireContext(), PiAnalysisActivity.class));
            });
        }
    }

    /**
     * Get greeting message based on current time of day
     * @return Greeting message with emoji
     */
    private String getGreetingBasedOnTime() {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        if (hourOfDay >= 5 && hourOfDay < 12) {
            return "Good Morning";
        } else if (hourOfDay >= 12 && hourOfDay < 15) {
            return "Good Afternoon";
        } else if (hourOfDay >= 15 && hourOfDay < 19) {
            return "Good Evening";
        } else {
            return "Good Night";
        }
    }
}
