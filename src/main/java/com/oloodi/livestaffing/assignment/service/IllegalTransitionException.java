package com.oloodi.livestaffing.assignment.service;

import com.oloodi.livestaffing.assignment.domain.AttendanceStatus;
import com.oloodi.livestaffing.assignment.domain.ParticipationStatus;

public class IllegalTransitionException extends RuntimeException {

    private final ParticipationStatus fromParticipation;
    private final AttendanceStatus fromAttendance;
    private final String action;

    public IllegalTransitionException(
            ParticipationStatus fromParticipation, AttendanceStatus fromAttendance, String action) {
        super("Illegal transition: cannot %s from participation=%s attendance=%s"
                .formatted(action, fromParticipation, fromAttendance));
        this.fromParticipation = fromParticipation;
        this.fromAttendance = fromAttendance;
        this.action = action;
    }

    public ParticipationStatus getFromParticipation() {
        return fromParticipation;
    }

    public AttendanceStatus getFromAttendance() {
        return fromAttendance;
    }

    public String getAction() {
        return action;
    }
}
