package com.example.mediline.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediline.R;
import com.example.mediline.model.Clinic;

import java.util.List;

public class ClinicAdapter extends RecyclerView.Adapter<ClinicAdapter.ClinicViewHolder> {

    public interface OnClinicClickListener {
        void onClinicClick(Clinic clinic);
        void onBookClick(Clinic clinic);
    }

    private final List<Clinic> clinics;
    private final OnClinicClickListener listener;

    public ClinicAdapter(List<Clinic> clinics, OnClinicClickListener listener) {
        this.clinics = clinics;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ClinicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_clinic, parent, false);
        return new ClinicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClinicViewHolder holder, int position) {
        Clinic clinic = clinics.get(position);
        holder.name.setText(clinic.getName());
        holder.address.setText(clinic.getAddress());
        holder.rating.setText("★ 4.8");
        holder.waitTime.setText("~15 mins");

        holder.itemView.setOnClickListener(v -> listener.onClinicClick(clinic));
        holder.bookBtn.setOnClickListener(v -> listener.onBookClick(clinic));
    }

    @Override
    public int getItemCount() { return clinics.size(); }

    static class ClinicViewHolder extends RecyclerView.ViewHolder {
        TextView name, address, rating, waitTime;
        View bookBtn;

        ClinicViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.clinic_name);
            address = itemView.findViewById(R.id.clinic_address);
            rating = itemView.findViewById(R.id.clinic_rating);
            waitTime = itemView.findViewById(R.id.clinic_wait_time);
            bookBtn = itemView.findViewById(R.id.clinic_book_btn);
        }
    }
}
