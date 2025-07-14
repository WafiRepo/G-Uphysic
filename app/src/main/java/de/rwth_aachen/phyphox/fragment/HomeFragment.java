package de.rwth_aachen.phyphox.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import de.rwth_aachen.phyphox.App;
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.activity.AngularActivity;
import de.rwth_aachen.phyphox.activity.GeneratesActivity;
import de.rwth_aachen.phyphox.activity.HistoryRecordActivity;
import de.rwth_aachen.phyphox.activity.IntroductionClassActivity;
import de.rwth_aachen.phyphox.activity.LinearActivity;
import de.rwth_aachen.phyphox.activity.QuestionActivity;
import de.rwth_aachen.phyphox.model.DataModel;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View ivNext1 = view.findViewById(R.id.iv_card_1);
        RelativeLayout rlOutClass = view.findViewById(R.id.rlOutClass);
        RelativeLayout rlInClass = view.findViewById(R.id.rlInClass);
        ivNext1.setOnClickListener(v -> {
            App app = (App) requireActivity().getApplication();
            DataModel dataModel = new DataModel();
            dataModel.setId(String.valueOf(System.currentTimeMillis()));
            dataModel.setTopics("Centripetal Acceleration");
            app.setDataModel(dataModel);
            startActivity(new Intent(getActivity(), LinearActivity.class));

        });

        View ivNext2 = view.findViewById(R.id.iv_card_2);
        ivNext2.setOnClickListener(v -> {
            App app = (App) requireActivity().getApplication();
            DataModel dataModel = new DataModel();
            dataModel.setId(String.valueOf(System.currentTimeMillis()));
            dataModel.setTopics("Linier Movements");
            app.setDataModel(dataModel);
            startActivity(new Intent(getActivity(), AngularActivity.class));
        });
        rlOutClass.setOnClickListener(view1 -> {
            App app = (App) requireActivity().getApplication();
            DataModel dataModel = new DataModel();
            dataModel.setId(String.valueOf(System.currentTimeMillis()));
            dataModel.setTopics("Centripetal Acceleration");
            app.setDataModel(dataModel);
            startActivity(new Intent(getActivity(), LinearActivity.class));
        });
        rlInClass.setOnClickListener(view1 -> {
            App app = (App) requireActivity().getApplication();
            DataModel dataModel = new DataModel();
            dataModel.setId(String.valueOf(System.currentTimeMillis()));
            dataModel.setTopics("In Class");
            app.setDataModel(dataModel);
            startActivity(new Intent(requireContext(), IntroductionClassActivity.class));
        });
            TextView tvName = view.findViewById(R.id.tvName);
        tvName.setText(SessionManager.getName(requireContext()));
    }
}
