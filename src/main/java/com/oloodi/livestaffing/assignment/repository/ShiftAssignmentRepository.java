package com.oloodi.livestaffing.assignment.repository;

import com.oloodi.livestaffing.assignment.domain.ShiftAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, UUID> {

    @EntityGraph(attributePaths = {"worker", "shift"})
    @Query("SELECT a FROM ShiftAssignment a WHERE a.shift.id = :shiftId ORDER BY a.worker.name")
    List<ShiftAssignment> findAllByShiftIdWithWorker(@Param("shiftId") UUID shiftId);

    @EntityGraph(attributePaths = {"worker", "shift"})
    @Query("SELECT a FROM ShiftAssignment a WHERE a.shift.id = :shiftId AND a.id = :assignmentId")
    Optional<ShiftAssignment> findByShiftIdAndIdWithWorker(
            @Param("shiftId") UUID shiftId, @Param("assignmentId") UUID assignmentId);
}
