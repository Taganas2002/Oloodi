package com.oloodi.livestaffing.shift.repository;

import com.oloodi.livestaffing.shift.domain.Shift;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftRepository extends JpaRepository<Shift, UUID> {
}
