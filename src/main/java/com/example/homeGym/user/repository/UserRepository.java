package com.example.homeGym.user.repository;

import com.example.homeGym.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
  List<User> findAllByOrderByName();

  boolean existsByEmail(String email);

  Optional<User> findByEmail(String email);
}
