package com.example.homeGym.instructor.repository;

import com.example.homeGym.instructor.dto.ProgramDto;
import com.example.homeGym.instructor.dto.UserProgramDto;
import com.example.homeGym.instructor.entity.UserProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserProgramRepository extends JpaRepository<UserProgram, Long> {
    List<UserProgram> findByUserIdAndState(Long userId, UserProgram.UserProgramState state);
    List<UserProgram> findByProgramIdAndState(Long programId, UserProgram.UserProgramState state);
    List<UserProgram> findAllByUserId(Long userId);
    List<UserProgram> findAllByProgramId(Long programLongId);
    List<UserProgram> findByProgramId(Long programId);

    UserProgram findByUserIdAndProgramId(Long userId, Long programId);


}
