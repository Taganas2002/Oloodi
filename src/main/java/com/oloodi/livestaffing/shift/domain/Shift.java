package com.oloodi.livestaffing.shift.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shifts")
public class Shift {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(nullable = false)
    private String location;

    @Column(name = "required_workers", nullable = false)
    private int requiredWorkers;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Shift() {
    }

    public Shift(
            UUID id,
            String title,
            Instant startsAt,
            Instant endsAt,
            String location,
            int requiredWorkers,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.location = location;
        this.requiredWorkers = requiredWorkers;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public String getLocation() {
        return location;
    }

    public int getRequiredWorkers() {
        return requiredWorkers;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Demo/reset helper: rewrite the shift window without changing identity or capacity. */
    public void reschedule(Instant startsAt, Instant endsAt, Instant updatedAt) {
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.updatedAt = updatedAt;
    }
}
