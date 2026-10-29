package com.hospital.opd.service;

import com.hospital.opd.dto.AppointmentRequest;
import com.hospital.opd.dto.AppointmentResponse;
import com.hospital.opd.model.OpdAppointment;
import com.hospital.opd.model.OpdDoctor;
import com.hospital.opd.repository.OpdAppointmentRepository;
import com.hospital.opd.repository.OpdDoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

@Service
public class OpdAppointmentService {

    private final OpdAppointmentRepository appointmentRepository;
    private final OpdDoctorRepository doctorRepository;

    public OpdAppointmentService(OpdAppointmentRepository appointmentRepository, OpdDoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
    }

    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request, String nurseUsername) {
        
        // 1. Validate that the targeted doctor actually exists and is actively accepting appointments
        OpdDoctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException("Doctor record not found with ID: " + request.getDoctorId()));

        if (!doctor.getIsAvailable()) {
            throw new IllegalStateException("Doctor " + doctor.getName() + " is currently unavailable for bookings.");
        }

        // 2. Parse date string to proper OffsetDateTime object
        OffsetDateTime targetTime = OffsetDateTime.parse(request.getPreferredTime());

        // 3. Build the Database Entity State Model
        OpdAppointment appointment = new OpdAppointment();
        appointment.setPatientUsername(request.getPatientName());
        appointment.setDoctorId(request.getDoctorId());
        appointment.setPreferredTime(targetTime);
        appointment.setStatus("Scheduled"); // Matches default migration value
        appointment.setCreatedAt(OffsetDateTime.now());
        appointment.setRecordedBy(nurseUsername); 


        OpdAppointment savedAppointment = appointmentRepository.save(appointment);

        // 4. Return formatted response payload back to client
        return new AppointmentResponse(
                savedAppointment.getId().toString(),
                savedAppointment.getStatus(),
                savedAppointment.getPatientUsername(),
                savedAppointment.getDoctorId(),
                savedAppointment.getPreferredTime().toString()
        );
    }
}
