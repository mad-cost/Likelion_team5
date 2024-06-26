package com.example.homeGym.instructor.repository;

import com.example.homeGym.instructor.entity.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InstructorRepository extends JpaRepository<Instructor, Long> {

    boolean existsByEmail(String email);

    List<Instructor> findAllByOrderByName();

    Optional<Instructor> findByEmail(String email);
}