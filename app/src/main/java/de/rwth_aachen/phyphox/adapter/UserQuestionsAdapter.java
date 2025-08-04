package de.rwth_aachen.phyphox.adapter;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.rwth_aachen.phyphox.App;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.activity.GeneratesActivity;
import de.rwth_aachen.phyphox.model.DataModel;

public class UserQuestionsAdapter extends RecyclerView.Adapter<UserQuestionsAdapter.ViewHolder> {
    private Context context;
    private Application application;
    private List<DataModel> progressList;

    public UserQuestionsAdapter(Context context, Application application) {
        this.context = context;
        this.application = application;
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

        // Set topic with fallback
        String topic = item.getTopics();
        if (topic == null || topic.isEmpty()) {
            topic = "Fisika Umum";
        }
        holder.tvTopic.setText(topic);

        // Set difficulty level with emoji
        String difficulty = item.getTypeQuestion();
        if (difficulty == null || difficulty.isEmpty()) {
            difficulty = "Medium";
        }
        
        String difficultyWithEmoji = getDifficultyWithEmoji(difficulty);
        holder.tvType.setText(difficultyWithEmoji);

        // Set question preview
        String question = item.getQuestion();
        if (question == null || question.isEmpty()) {
            question = "Tidak ada pertanyaan yang tersedia.";
        }
        holder.tvQuestion.setText(question);

        // Set creator name
        String creator = item.getCustomerName();
        if (creator == null || creator.isEmpty()) {
            creator = "Anonim";
        }
        holder.tvCreator.setText(creator);

        // Set views count
        holder.tvViews.setText(item.getViews() + " views");

        // Set date with real data
        String formattedDate = formatDate(item.getDateTime());
        holder.tvDate.setText(formattedDate);

        // Set click listener for the entire card
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, GeneratesActivity.class);
            intent.putExtra("isFromMainMenu", false);
            intent.putExtra("isCustomQuestion", true);
            App app = (App) application;
            app.setDataModel(item);
            context.startActivity(intent);
        });
    }

    private String getDifficultyWithEmoji(String difficulty) {
        switch (difficulty.toLowerCase()) {
            case "easy":
            case "mudah":
                return "🟢 Mudah";
            case "medium":
            case "sedang":
            case "intermediate":
                return "🟡 Sedang";
            case "hard":
            case "sulit":
            case "advanced":
            case "lanjutan":
                return "🔴 Sulit";
            default:
                return "🟡 Sedang"; // Default to medium
        }
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "Baru saja";
        }
        
        try {
            // Try multiple date formats
            String[] formats = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "dd/MM/yyyy HH:mm:ss",
                "MM/dd/yyyy HH:mm:ss",
                "yyyy-MM-dd",
                "dd/MM/yyyy"
            };
            
            Date date = null;
            for (String format : formats) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat(format, Locale.getDefault());
                    date = inputFormat.parse(dateString);
                    break;
                } catch (Exception ignored) {
                    // Continue to next format
                }
            }
            
            if (date != null) {
                // Calculate time difference
                long timeDiff = System.currentTimeMillis() - date.getTime();
                long daysDiff = timeDiff / (24 * 60 * 60 * 1000);
                long hoursDiff = timeDiff / (60 * 60 * 1000);
                long minutesDiff = timeDiff / (60 * 1000);
                
                if (daysDiff > 0) {
                    if (daysDiff == 1) {
                        return "1 hari lalu";
                    } else if (daysDiff < 7) {
                        return daysDiff + " hari lalu";
                    } else {
                        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                        return outputFormat.format(date);
                    }
                } else if (hoursDiff > 0) {
                    return hoursDiff + " jam lalu";
                } else if (minutesDiff > 0) {
                    return minutesDiff + " menit lalu";
                } else {
                    return "Baru saja";
                }
            }
            
            // If parsing fails, return original string
            return dateString;
            
        } catch (Exception e) {
            return "Baru saja";
        }
    }

    @Override
    public int getItemCount() {
        return progressList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTopic, tvType, tvQuestion, tvCreator, tvDate, tvViews;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTopic = itemView.findViewById(R.id.tvTopic);
            tvType = itemView.findViewById(R.id.tvType);
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            tvCreator = itemView.findViewById(R.id.tvCreator);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvViews = itemView.findViewById(R.id.tvViews);
        }
    }
}

