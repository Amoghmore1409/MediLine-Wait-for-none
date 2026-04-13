package com.example.mediline.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Appointment {
    @DocumentId
    private String appointmentId;
    private String clinicId;
    private int tokenNumber;
    private String status; // WAITING, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW
    private String source; // APP, WALK_IN
    private String patientId;
    private String patientName;
    private String walkInName;
    private String walkInPhone;
    private String visitType;
    @ServerTimestamp
    private Date estimatedTime;
    @ServerTimestamp
    private Date createdAt;
    
    private String patientMedicalRecordUrl;
    private String prescriptionUrl;

    public Appointment() {}

    public Appointment(String clinicId, int tokenNumber, String status, String source,
                       String patientId, String patientName, String visitType) {
        this.clinicId = clinicId;
        this.tokenNumber = tokenNumber;
        this.status = status;
        this.source = source;
        this.patientId = patientId;
        this.patientName = patientName;
        this.visitType = visitType;
    }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    public String getClinicId() { return clinicId; }
    public void setClinicId(String clinicId) { this.clinicId = clinicId; }
    public int getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(int tokenNumber) { this.tokenNumber = tokenNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getWalkInName() { return walkInName; }
    public void setWalkInName(String walkInName) { this.walkInName = walkInName; }
    public String getWalkInPhone() { return walkInPhone; }
    public void setWalkInPhone(String walkInPhone) { this.walkInPhone = walkInPhone; }
    public String getVisitType() { return visitType; }
    public void setVisitType(String visitType) { this.visitType = visitType; }
    public Date getEstimatedTime() { return estimatedTime; }
    public void setEstimatedTime(Date estimatedTime) { this.estimatedTime = estimatedTime; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public String getPatientMedicalRecordUrl() { return patientMedicalRecordUrl; }
    public void setPatientMedicalRecordUrl(String patientMedicalRecordUrl) { this.patientMedicalRecordUrl = patientMedicalRecordUrl; }
    public String getPrescriptionUrl() { return prescriptionUrl; }
    public void setPrescriptionUrl(String prescriptionUrl) { this.prescriptionUrl = prescriptionUrl; }
}
