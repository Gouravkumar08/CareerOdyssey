package com.outreach.dsa.repository;

import com.outreach.dsa.model.Sheet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SheetRepository extends JpaRepository<Sheet, Long> {
}
