package com.prog.tsinjo.repository;

import com.prog.tsinjo.domain.Help;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HelpRepository extends JpaRepository<Help, Long> {
    List<Help> findAllByOrderByCreatedAtDesc();
}
