package com.oloodi.livestaffing.worker.repository;

import com.oloodi.livestaffing.worker.domain.Worker;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerRepository extends JpaRepository<Worker, UUID> {
}
