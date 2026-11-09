package com.outreach.repository;

import com.outreach.model.Outreach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutreachRepository extends JpaRepository<Outreach, Long>, JpaSpecificationExecutor<Outreach> {
    void deleteByUser_Id(Long userId);

    void deleteByCompany_Id(Long companyId);

    @Query("SELECT o.user.id, MAX(o.sentAt) FROM Outreach o WHERE o.sentAt IS NOT NULL GROUP BY o.user.id")
    List<Object[]> findLastSentAtByUser();
}
