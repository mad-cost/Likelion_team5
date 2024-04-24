package com.example.homeGym.instructor.service;


import com.example.homeGym.common.exception.CustomGlobalErrorCode;
import com.example.homeGym.common.exception.GlobalExceptionHandler;
import com.example.homeGym.common.util.AuthenticationFacade;
import com.example.homeGym.instructor.dto.ProgramDto;
import com.example.homeGym.instructor.entity.Instructor;
import com.example.homeGym.instructor.entity.Program;
import com.example.homeGym.instructor.repository.ProgramRepository;
import com.example.homeGym.instructor.repository.UserProgramRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.homeGym.instructor.entity.Program.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProgramService {
  private final ProgramRepository programRepository;
  private final UserProgramRepository userProgramRepository;
  private final AuthenticationFacade facade;

  // 프로그램 목록
  public List<ProgramDto> findByProgramId(List<Long> id){
    List<ProgramDto> programDtos = new ArrayList<>();
    for (Program program : programRepository.findAllById(id)){
      programDtos.add(ProgramDto.fromEntity(program));
    }
    return programDtos;
  }

  public List<ProgramDto> findAllByInstructorIdConvertId(Long instructorId){
    List<ProgramDto> programs = new ArrayList<>();
    for (Program program : programRepository.findAllByInstructorId(instructorId)){
      programs.add(ProgramDto.fromEntity(program));
    }
    return programs;
  }

  public List<Long> ConvertLong(List<ProgramDto> programs){
    List<Long> programIds = new ArrayList<>();

    for (ProgramDto program : programs){
      programIds.add(program.getId());
    }
    return programIds;
  }

//  public List<ProgramDto> findAllByInstructorIdConvertProgramId(Long instructorId){
//    List<Program> programs = new ArrayList<>();
//    for (Program program : programRepository.findAllByInstructorId(instructorId)){
//      programs.add(ProgramDto.fromEntity(program.getId()));
//    }
//  }

  // 프로그램 생성
  @Transactional
  public Long createProgram(ProgramDto programDto) {
    Instructor currentInstructor = facade.getCurrentInstructor();

    Program program = builder()
            .instructorId(currentInstructor.getId())
            .category(programDto.getCategory())
            .title(programDto.getTitle())
            .description(programDto.getDescription())
            .supplies(programDto.getSupplies())
            .curriculum(programDto.getCurriculum())
            .price1(programDto.getPrice1())
            .price10(programDto.getPrice10())
            .price20(programDto.getPrice20())
            .build();

    Program savedProgram = programRepository.save(program);
    return savedProgram.getId(); // 저장된 프로그램의 ID 반환
  }

  // 프로그램 수정
  @Transactional
  public void updateProgram(Long programId, ProgramDto programDto) {
    Optional<Program> optionalProgram = programRepository.findById(programId);
    if (optionalProgram.isEmpty()) {
      throw new GlobalExceptionHandler(CustomGlobalErrorCode.PROGRAM_NOT_EXISTS);
    }

    Program program = optionalProgram.get();
    Instructor currentInstructor = facade.getCurrentInstructor();

    if (!program.getInstructorId().equals(currentInstructor.getId())) {
      throw new GlobalExceptionHandler(CustomGlobalErrorCode.PROGRAM_FORBIDDEN);
    }

    program.setTitle(programDto.getTitle());
    program.setDescription(programDto.getDescription());
    program.setSupplies(programDto.getSupplies());
    program.setCurriculum(programDto.getCurriculum());

    programRepository.save(program);
  }
//  모든 프로그램 가져오기
  public List<Program> findAll(){
    return programRepository.findAll();
  }
//  state가 CREATION_PENDING 가져오기
  public List<ProgramDto> findAllByStateIsCreation(List<Program> programs){
    List<ProgramDto> stateCreateDto = new ArrayList<>();
    for (Program program : programs){
      if (program.getState() == Program.ProgramState.CREATION_PENDING){
        stateCreateDto.add(ProgramDto.fromEntity(program));
      }
    }
    return stateCreateDto;
  }
//  신규 등록 프로그램 State -> IN_PROGRESS
  public void stateConvertInProgress(Long programId){
    Program program = programRepository.findById(programId).orElseThrow();

    program.setState(Program.ProgramState.IN_PROGRESS);
    programRepository.save(program);
  }
//  신규 등록 프로그램 거절
  public void deleteInProgress(Long programId){
    Program program = programRepository.findById(programId).orElseThrow();
    programRepository.delete(program);
  }
  //  state가 MODIFICATION_PENDING 가져오기
  public List<ProgramDto> findAllByStateIsModification(List<Program> programs){
    List<ProgramDto> stateCreateDto = new ArrayList<>();
    for (Program program : programs){
      if (program.getState() == Program.ProgramState.MODIFICATION_PENDING){
        stateCreateDto.add(ProgramDto.fromEntity(program));
      }
    }
    return stateCreateDto;
  }
  //  state가 DELETION_PENDING 가져오기
  public List<ProgramDto> findAllByStateIsDeletion(List<Program> programs){
    List<ProgramDto> stateCreateDto = new ArrayList<>();
    for (Program program : programs){
      if (program.getState() == Program.ProgramState.DELETION_PENDING){
        stateCreateDto.add(ProgramDto.fromEntity(program));
      }
    }
    return stateCreateDto;
  }

  // 프로그램 삭제
  @Transactional
  public void deleteProgram(Long programId) {
    Optional<Program> optionalProgram = programRepository.findById(programId);
    if (optionalProgram.isEmpty()) {
      throw new GlobalExceptionHandler(CustomGlobalErrorCode.PROGRAM_NOT_EXISTS);
    }

    Program program = optionalProgram.get();
    Instructor currentInstructor = facade.getCurrentInstructor();
    if (!program.getInstructorId().equals(currentInstructor.getId())) {
      throw new GlobalExceptionHandler(CustomGlobalErrorCode.PROGRAM_FORBIDDEN);
    }

    programRepository.deleteById(programId);
  }
}
