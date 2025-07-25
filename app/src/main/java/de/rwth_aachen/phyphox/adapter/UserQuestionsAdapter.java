package de.rwth_aachen.phyphox.adapter;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import de.rwth_aachen.phyphox.App;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.activity.DetailRecordActivity;
import de.rwth_aachen.phyphox.activity.GeneratesActivity;
import de.rwth_aachen.phyphox.activity.GeneratesClassQuestionActivity;
import de.rwth_aachen.phyphox.model.DataModel;

public class UserQuestionsAdapter extends RecyclerView.Adapter<UserQuestionsAdapter.ViewHolder> {
    private Context context;
    private Application application;
    private List<DataModel> progressList;

    public UserQuestionsAdapter(Context context, Application application) {
        this.context = context;
        this.application= application;
        this.progressList = new ArrayList<>();
    }
    public void addData(List<DataModel> progressList) {
        this.progressList = progressList;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_questions, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataModel item = progressList.get(position);

        holder.tvTopic.setText(item.getTopics());
        holder.tvType.setText(item.getTypeQuestion());
        holder.tvQuestions.setText(item.getQuestion());
        holder.tvCreator.setText("Creator : "+item.getCustomerName());


            holder.itemView.setOnClickListener(view -> {
                Intent intent = new Intent(context, GeneratesActivity.class);
                    intent.putExtra("isFromMainMenu", false);
                    intent.putExtra("isCustomQuestion", true);
                    App app = (App) application;
                    app.setDataModel(item);
                    context.startActivity(intent);
//                App app = (App) ((Application) context.getApplicationContext());
//                app.setDataModel(item);
//                Intent intent = new Intent(context, DetailRecordActivity.class);
//                context.startActivity(intent);
            });
//            holder.tvStatus.setText("In Progress");
//            holder.tvStatus.setBackgroundResource(R.drawable.background_8_orange);
//            holder.tvInProgressAction.setOnClickListener(view -> {
//                if (item.getTopics().equals("Out Class") || item.getTopics().equals("In Class")) {
//                    Intent intent = new Intent(context, GeneratesClassQuestionActivity.class);
//                    intent.putExtra("isFromMainMenu", false);
//                    App app = (App) application;
//                    app.setDataModel(item);
//                    context.startActivity(intent);
//                }else{
//                    Intent intent = new Intent(context, GeneratesActivity.class);
//                    intent.putExtra("isFromMainMenu", false);
//                    App app = (App) application;
//                    app.setDataModel(item);
//                    context.startActivity(intent);
//                }
//
//            });
//        }
    }

    @Override
    public int getItemCount() {
        return progressList.size();
    }



    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTopic, tvType, tvQuestions, tvCreator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTopic = itemView.findViewById(R.id.tvTopic);
            tvQuestions = itemView.findViewById(R.id.tvQuestion);
            tvType = itemView.findViewById(R.id.tvType);
            tvCreator = itemView.findViewById(R.id.tvCreator);
        }
    }
}

