package com.hospital.opd.dto;

public class AppointmentResponse {
    private String bookingId;
    private String status;
    private String assignedToPatient;
    private String doctorId;
    private String timeSlot;

    // Explicit constructor wiring to bypass Lombok limitations
    public AppointmentResponse(String bookingId, String status, String assignedToPatient, String doctorId, String timeSlot) {
        this.bookingId = bookingId;
        this.status = status;
        this.assignedToPatient = assignedToPatient;
        this.doctorId = doctorId;
        this.timeSlot = timeSlot;
    }

    // Explicit Getters for JSON serialization mapping pipelines
    public String getBookingId() { return bookingId; }
    public String getStatus() { return status; }
    public String getAssignedToPatient() { return assignedToPatient; }
    public String getDoctorId() { return doctorId; }
    public String getTimeSlot() { return timeSlot; }
}
