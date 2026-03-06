package com.example.backend.repository;

import com.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUuid(UUID uuid);
    Optional<User> findByEmail(String email);
    List<User>     findByIsProfessor(boolean isProfessor);
    List<User>     findByProfessorId(Integer professorId);
    boolean        existsByEmail(String email);
}