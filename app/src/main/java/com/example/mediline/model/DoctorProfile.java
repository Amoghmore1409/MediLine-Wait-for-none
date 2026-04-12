package com.example.mediline.model;

import com.google.firebase.firestore.DocumentId;

public class DoctorProfile {
    @DocumentId
    private String doctorId;
    private String name;
    private String specialization;
    private int experienceYears;

    public DoctorProfile() {}

    public DoctorProfile(String name, String specialization, int experienceYears) {
        this.name = name;
        this.specialization = specialization;
        this.experienceYears = experienceYears;
    }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }
}
