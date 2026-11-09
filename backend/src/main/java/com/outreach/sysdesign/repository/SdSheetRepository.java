package com.outreach.sysdesign.repository;

import com.outreach.sysdesign.model.SdSheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SdSheetRepository extends JpaRepository<SdSheet, Long> {
    Optional<SdSheet> findByNameIgnoreCase(String name);
}
