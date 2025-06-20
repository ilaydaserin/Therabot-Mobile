package com.ilaydas.therabotmobile;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide; // Resim yükleme kütüphanesi
import de.hdodenhof.circleimageview.CircleImageView;
import com.ilaydas.therabotmobile.R;
import com.ilaydas.therabotmobile.Psychologist;
import com.ilaydas.therabotmobile.PsychologistDetail;

import java.util.List;

public class PsychologistAdapter extends RecyclerView.Adapter<PsychologistAdapter.PsychologistViewHolder> {

    private List<Psychologist> psychologistList;
    private Context context;

    public PsychologistAdapter(Context context, List<Psychologist> psychologistList) {
        this.context = context;
        this.psychologistList = psychologistList;
    }

    @NonNull
    @Override
    public PsychologistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_psychologist_card, parent, false);
        return new PsychologistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PsychologistViewHolder holder, int position) {
        Psychologist psychologist = psychologistList.get(position);
        holder.psychologistName.setText(psychologist.getName());
        holder.psychologistSpecialty.setText(psychologist.getSpecialty());
        holder.psychologistShortBio.setText(psychologist.getShortBio());

        // Glide ile resim yükleme
        if (psychologist.getProfileImageUrl() != null && !psychologist.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(psychologist.getProfileImageUrl())
                    .placeholder(R.drawable.psikolog) // Yüklenirken gösterilecek resim
                    .error(R.drawable.psikolog) // Hata durumunda gösterilecek resim
                    .into(holder.psychologistImage);
        } else {
            holder.psychologistImage.setImageResource(R.drawable.psikolog);
        }

        holder.itemView.setOnClickListener(v -> {
            // Detay sayfasına geçiş
            Intent intent = new Intent(context, PsychologistDetail.class);
            intent.putExtra("psychologistId", psychologist.getId()); // Psikolog ID'sini taşı
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return psychologistList.size();
    }

    public static class PsychologistViewHolder extends RecyclerView.ViewHolder {
        CircleImageView psychologistImage;
        TextView psychologistName;
        TextView psychologistSpecialty;
        TextView psychologistShortBio;

        public PsychologistViewHolder(@NonNull View itemView) {
            super(itemView);
            psychologistImage = itemView.findViewById(R.id.psychologist_image);
            psychologistName = itemView.findViewById(R.id.psychologist_name);
            psychologistSpecialty = itemView.findViewById(R.id.psychologist_specialty);
            psychologistShortBio = itemView.findViewById(R.id.psychologist_short_bio);
        }
    }
}