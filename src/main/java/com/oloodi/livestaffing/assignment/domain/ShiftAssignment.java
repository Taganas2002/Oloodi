package com.oloodi.livestaffing.assignment.domain;

import com.oloodi.livestaffing.shift.domain.Shift;
import com.oloodi.livestaffing.worker.domain.Worker;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shift_assignments")
public class ShiftAssignment {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Enumerated(EnumType.STRING)
    @Column(name = "participation_status", nullable = false, length = 32)
    private ParticipationStatus participationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 32)
    private AttendanceStatus attendanceStatus;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "checked_out_at")
    private Instant checkedOutAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ShiftAssignment() {
    }

    public ShiftAssignment(
            UUID id,
            Shift shift,
            Worker worker,
            ParticipationStatus participationStatus,
            AttendanceStatus attendanceStatus,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.shift = shift;
        this.worker = worker;
        this.participationStatus = participationStatus;
        this.attendanceStatus = attendanceStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public Shift getShift() {
        return shift;
    }

    public Worker getWorker() {
        return worker;
    }

    public ParticipationStatus getParticipationStatus() {
        return participationStatus;
    }

    public AttendanceStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }

    public Instant getCheckedOutAt() {
        return checkedOutAt;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void applyState(
            ParticipationStatus participationStatus,
            AttendanceStatus attendanceStatus,
            Instant checkedInAt,
            Instant checkedOutAt,
            Instant updatedAt) {
        this.participationStatus = participationStatus;
        this.attendanceStatus = attendanceStatus;
        this.checkedInAt = checkedInAt;
        this.checkedOutAt = checkedOutAt;
        this.updatedAt = updatedAt;
    }
}
