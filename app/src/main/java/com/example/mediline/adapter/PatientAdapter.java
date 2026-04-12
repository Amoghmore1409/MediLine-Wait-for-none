package com.example.mediline.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediline.R;
import com.example.mediline.model.Appointment;

import java.util.List;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {

    private final List<Appointment> appointments;

    public PatientAdapter(List<Appointment> appointments) {
        this.appointments = appointments;
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient, parent, false);
        return new PatientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        Appointment appt = appointments.get(position);
        holder.token.setText(String.format("%02d", appt.getTokenNumber()));
        holder.name.setText(appt.getPatientName() != null ? appt.getPatientName() : "Walk-in Patient");
        holder.visitType.setText(appt.getVisitType() != null ? appt.getVisitType() : "Consultation");

        String status = appt.getStatus();
        holder.status.setText(status);
        if ("WAITING".equals(status)) {
            holder.status.setTextColor(holder.itemView.getContext().getColor(R.color.outline));
        } else if ("IN_PROGRESS".equals(status)) {
            holder.status.setTextColor(holder.itemView.getContext().getColor(R.color.primary));
        } else {
            holder.status.setTextColor(holder.itemView.getContext().getColor(R.color.secondary));
        }
    }

    @Override
    public int getItemCount() { return appointments.size(); }

    static class PatientViewHolder extends RecyclerView.ViewHolder {
        TextView token, name, visitType, status;

        PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            token = itemView.findViewById(R.id.patient_token);
            name = itemView.findViewById(R.id.patient_name);
            visitType = itemView.findViewById(R.id.patient_visit_type);
            status = itemView.findViewById(R.id.patient_status);
        }
    }
}
