package com.example.mediline.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediline.R;
import com.example.mediline.R;
import com.example.mediline.model.Clinic;
import com.example.mediline.repository.AppointmentRepository;

import android.location.Location;

import java.util.List;

public class ClinicAdapter extends RecyclerView.Adapter<ClinicAdapter.ClinicViewHolder> {

    public interface OnClinicClickListener {
        void onClinicClick(Clinic clinic);
        void onBookClick(Clinic clinic);
    }

    private final List<Clinic> clinics;
    private final OnClinicClickListener listener;
    private Location patientLocation;
    private final AppointmentRepository appointmentRepo;

    public ClinicAdapter(List<Clinic> clinics, OnClinicClickListener listener) {
        this.clinics = clinics;
        this.listener = listener;
        this.appointmentRepo = new AppointmentRepository();
    }

    public void setPatientLocation(Location location) {
        this.patientLocation = location;
        notifyDataSetChanged();
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
        
        if (patientLocation != null && clinic.getLatitude() != 0 && clinic.getLongitude() != 0) {
            Location clinicLoc = new Location("");
            clinicLoc.setLatitude(clinic.getLatitude());
            clinicLoc.setLongitude(clinic.getLongitude());
            float distanceMiles = patientLocation.distanceTo(clinicLoc) * 0.000621371f;
            holder.address.setText(String.format("%.1f miles away \u2022 %s", distanceMiles, clinic.getAddress()));
        } else {
            holder.address.setText(clinic.getAddress());
        }
        
        holder.rating.setText("★ 4.8");
        holder.waitTime.setText("Calculating...");

        appointmentRepo.getQueueForClinic(clinic.getClinicId(), querySnapshot -> {
            if (querySnapshot != null) {
                int queueSize = querySnapshot.size();
                int baseAvgTime = clinic.getAverageVisitTimeMinutes() > 0 ? clinic.getAverageVisitTimeMinutes() : 15;
                int totalWaitMins = queueSize * baseAvgTime;
                
                if (queueSize == 0) {
                    holder.waitTime.setText("No wait time");
                } else {
                    holder.waitTime.setText("~" + totalWaitMins + " mins wait");
                }
            } else {
                holder.waitTime.setText("Unknown wait");
            }
        });

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
