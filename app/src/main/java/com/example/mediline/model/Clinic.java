package com.example.mediline.model;

import com.google.firebase.firestore.DocumentId;

public class Clinic {
    @DocumentId
    private String clinicId;
    private String doctorId;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String openingTime;
    private String closingTime;
    private double consultationFee;
    private String specialization;

    public Clinic() {}

    public Clinic(String doctorId, String name, String address, double latitude, double longitude,
                  String openingTime, String closingTime, double consultationFee, String specialization) {
        this.doctorId = doctorId;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.consultationFee = consultationFee;
        this.specialization = specialization;
    }

    public String getClinicId() { return clinicId; }
    public void setClinicId(String clinicId) { this.clinicId = clinicId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getOpeningTime() { return openingTime; }
    public void setOpeningTime(String openingTime) { this.openingTime = openingTime; }
    public String getClosingTime() { return closingTime; }
    public void setClosingTime(String closingTime) { this.closingTime = closingTime; }
    public double getConsultationFee() { return consultationFee; }
    public void setConsultationFee(double consultationFee) { this.consultationFee = consultationFee; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
}
