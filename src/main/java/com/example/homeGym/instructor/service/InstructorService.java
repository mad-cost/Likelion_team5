package com.example.homeGym.instructor.service;


import com.example.homeGym.auth.dto.CustomInstructorDetails;
import com.example.homeGym.auth.jwt.JwtTokenUtils;
import com.example.homeGym.auth.service.InstructorDetailsManager;
import com.example.homeGym.auth.utils.CookieUtil;


import com.example.homeGym.instructor.dto.InstructorCreateDto;
import com.example.homeGym.instructor.dto.InstructorDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import com.example.homeGym.instructor.dto.ProgramDto;
import com.example.homeGym.instructor.dto.InstructorReviewDto;
import com.example.homeGym.instructor.dto.InstructorUpdateDto;
import com.example.homeGym.instructor.entity.*;
import com.example.homeGym.instructor.repository.*;
import com.example.homeGym.user.entity.Review;
import com.example.homeGym.user.entity.User;
import com.example.homeGym.user.repository.ReviewRepository;
import com.example.homeGym.user.repository.UserRepository;
import com.example.homeGym.user.utils.FileHandlerUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@ToString
public class InstructorService {
    private final InstructorRepository instructorRepository;
    private final InstructorAddressRepository instructorAddressRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProgramRepository programRepository;
    private final CookieUtil cookieUtil;
    private final JwtTokenUtils jwtTokenUtils;
    private final InstructorDetailsManager instructorDetailsManager;
    private final FileHandlerUtils fileHandlerUtils;

    //강사 회원 가입
    //REGISTRATION_PENDING 상태로 DB에 저장
    public void createInstructor(InstructorCreateDto dto, List<MultipartFile> images) {
        log.info("Creating instructor with name: {}", dto.getName());
        List<String> imagePaths = new ArrayList<>();
        int count = 0;
        for (MultipartFile image :
                images) {
            if (image.getSize() != 0) {
                String imgPath = fileHandlerUtils.saveFile("instructor",
                        String.format("profile_image_instructor_%s_%d", "1", count), image);
                imagePaths.add(imgPath);
                count++;
            }
        }

        Instructor instructor = dto.toEntity();
        instructor.setPassword(dto.getPassword(), passwordEncoder); // 비밀번호 설정
        instructor.setProfileImageUrl(imagePaths);
        instructorRepository.save(instructor);
    }

    public boolean signIn(HttpServletResponse res, String email, String password) throws Exception {
        Optional<Instructor> nowInstructor = instructorRepository.findByEmail(email);
        Instructor instructor = nowInstructor.get();
        Boolean pwCheck = passwordEncoder.matches(password, instructor.getPassword());
        if (pwCheck) {
            CustomInstructorDetails customInstructorDetails = (CustomInstructorDetails) instructorDetailsManager.loadUserByUsername(email);
            String jwtToken = jwtTokenUtils.generateToken(customInstructorDetails);

            cookieUtil.createCookie(res, "Authorization", jwtToken);
            return true;
        }
        return false;
    }


    //이메일 존재 확인
    public boolean isEmailAvailable(String email) {
        return !instructorRepository.existsByEmail(email);
    }

    //회원탈퇴 신청
    public String withdrawalProposal(Long instructorId, String withdrawalReason) {
        Optional<Instructor> instructorOpt = instructorRepository.findById(instructorId);

        if (!instructorOpt.isPresent()) {
            return "강사 정보를 찾을 수 없습니다.";
        }

        Instructor instructor = instructorOpt.get();
        if (instructor.getState() == Instructor.InstructorState.WITHDRAWAL_PENDING) {
            return "이미 탈퇴 대기 상태인 회원입니다.";
        }

        instructor.setState(Instructor.InstructorState.WITHDRAWAL_PENDING);
        instructor.setWithdrawalReason(withdrawalReason);
        instructorRepository.save(instructor);
        return "탈퇴 신청이 완료되었습니다.";
    }

    // 강사 이름 순서로 나열
    public List<InstructorDto> findAllByOrderByName() {
        List<InstructorDto> instructorDtos = new ArrayList<>();
        for (Instructor instructor : instructorRepository.findAllByOrderByName()) {
            instructorDtos.add(InstructorDto.fromEntity(instructor));
        }
        return instructorDtos;
    }

    public InstructorDto findById(Long instructorId) {
        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new NoSuchElementException("Instructor not found with ID: " + instructorId));
        return InstructorDto.fromEntity(instructor);
    }


    //강사 정보 수정
    @Transactional
    public void updateInstructor(InstructorUpdateDto dto, List<MultipartFile> images) {
        Optional<Instructor> instructorOpt = instructorRepository.findById(dto.getId());

        if (instructorOpt.isPresent()) {
            Instructor instructor = instructorOpt.get();

            //저장된 사진 삭제
            //리뷰에 연관된 이미지 파일 경로를 가져오기.
            List<String> imagePaths = instructor.getProfileImageUrl();

            //이미지 파일이 존재하면 삭제
            if (!imagePaths.isEmpty()) {
                for (String imagePath :
                        imagePaths) {
                    String mediaPath = "media/";
                    String fullPath = mediaPath + imagePath.replace("/static/", "");
                    try {
                        Files.deleteIfExists(Path.of(fullPath));
                    } catch (IOException e) {
                        log.error(e.getMessage());
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
            }

            List<String> newImagePaths = new ArrayList<>();
            int count = 0;
            if (images != null) {
                for (MultipartFile image :
                        images) {
                    if (image.getSize() != 0) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
                        String formattedDateTime = LocalDateTime.now().format(formatter);
                        String imgPath = fileHandlerUtils.saveFile("instructor",
                                String.format("profile_image_instructor_%s_%d", formattedDateTime, count), image);
                        newImagePaths.add(imgPath);
                        count++;
                    }
                }
            }
            dto.setProfileImageUrl(newImagePaths);
            dto.updateEntity(instructor); // DTO를 사용하여 엔티티 업데이트
            instructorRepository.save(instructor);
            log.info("Updated instructor with id: {}", dto.getId());
        } else {
            throw new RuntimeException("Instructor not found with id: " + dto.getId());
        }
    }

    //강사 리뷰 확인 페이지
    @Transactional(readOnly = true)
    public Page<InstructorReviewDto> findReviewsByInstructorId(Long instructorId, Pageable pageable) {
        // 강사 ID로 리뷰들을 페이지 단위로 가져오기
        Page<Review> reviews = reviewRepository.findByInstructorId(instructorId, pageable);
        // 리뷰 ID List 가져오기
        List<Long> reviewIds = reviews.getContent().stream().map(Review::getId).collect(Collectors.toList());
        // 답글 조회
        List<Comment> comments = commentRepository.findByReviewIdIn(reviewIds);
        // ID를 키로 하고 Comment 객체를 값으로 하는 맵 생성
        Map<Long, Comment> commentMap = comments.stream()
                .collect(Collectors.toMap(Comment::getReviewId, comment -> comment));
        //리뷰 페이지 스트림으로 변환, 각 리뷰는 InstructorReviewDto로 변환
        return reviews.map(review -> convertToDto(review, commentMap.get(review.getId())));
    }

    //Dto 생성 메소드
    private InstructorReviewDto convertToDto(Review review, Comment comment) {
        User user = userRepository.findById(review.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
        Long commentId = comment != null ? comment.getId() : null;
        String commentContent = comment != null ? comment.getContent() : null;
        return
                new InstructorReviewDto(
                        review.getId(),
                        user.getName(),
                        review.getMemo(),
                        review.getStars(),
                        review.getCreatedAt(),
                        commentContent,
                        commentId,
                        review.getImageUrl()  // Assume this is properly handled
                );
    }


    //프로그램 상태에 따라 분리해서 보여주기
    public Map<String, List<ProgramDto>> findProgramsByInstructorIdSeparatedByState(Long instructorId) {
        List<Program> programs = programRepository.findByInstructorId(instructorId);
        Map<String, List<ProgramDto>> separatedPrograms = new HashMap<>();
        List<ProgramDto> inProgressPrograms = programs.stream()
                .filter(p -> p.getState() == Program.ProgramState.IN_PROGRESS)
                .map(ProgramDto::fromEntity)
                .collect(Collectors.toList());
        List<ProgramDto> otherPrograms = programs.stream()
                .filter(p -> p.getState() != Program.ProgramState.IN_PROGRESS)
                .map(ProgramDto::fromEntity)
                .collect(Collectors.toList());

        separatedPrograms.put("inProgress", inProgressPrograms);
        separatedPrograms.put("other", otherPrograms);
        return separatedPrograms;
    }




//=============================   관리자    ==============================================================

    public void saveMedal(Long instructorId, String medal) {
        Instructor instructor = instructorRepository.findById(instructorId).orElseThrow();
        instructor.setMedal(medal);
        instructorRepository.save(instructor);
        InstructorDto.fromEntity(instructor);
    }

    public String findRank(String Gold, String Silver, String Bronze, String Unranked) {
        String myRank = null;
        // rank값 찾아서 저장
        if (Gold != null) {
            myRank = Gold;
        } else if (Silver != null) {
            myRank = Silver;
        } else if (Bronze != null) {
            myRank = Bronze;
        } // Unranked는 myRank에 null을 넣어줬으므로 만들어줄 필요가 없다
        return myRank;
    }

    public Instructor findByLongId(Long instructorId) {
        return instructorRepository.findById(instructorId).orElseThrow();
    }

    // 강사 신청시 처리 로직 REGISTRATION_PENDING만 가져온다
    public List<InstructorDto> findAllByStateIsRegistration() {
        List<InstructorDto> instructorDto = new ArrayList<>();
        // state가 REGISTRATION인 강사 모두 가져오기
        List<Instructor> instructors = instructorRepository.findAll();
        for (Instructor instructor : instructors) {
            if (instructor.getState() == Instructor.InstructorState.REGISTRATION_PENDING) {
                instructorDto.add(InstructorDto.fromEntity(instructor));
            }
        }
        return instructorDto;
    }

    //    강사 신청 승인
    public void accept(Long instructorId) {
        Instructor instructor = instructorRepository.findById(instructorId).orElseThrow();
        instructor.setRoles("ROLE_INSTRUCTOR");
        instructor.setState(Instructor.InstructorState.ACTIVE);
        instructorRepository.save(instructor);
        InstructorDto.fromEntity(instructor);
    }

    //    강사 신청 거절
    public void delete(Long instructorId) {
        instructorRepository.deleteById(instructorId);
    }

    //
    public List<InstructorDto> findAllByStateIsWithdrawalComplete() {
        List<InstructorDto> instructorDto = new ArrayList<>();
        // state가 WITHDRAWAL_PENDING 강사 모두 가져오기
        List<Instructor> instructors = instructorRepository.findAll();
        for (Instructor instructor : instructors) {
            if (instructor.getState() == Instructor.InstructorState.WITHDRAWAL_PENDING) {
                instructorDto.add(InstructorDto.fromEntity(instructor));
            }
        }
        return instructorDto;
    }

    //    강사 회원 탈퇴 승인
    public void withdraw(Long instructorId) {
        Instructor instructor = instructorRepository.findById(instructorId).orElseThrow();
        //이미지 path 가져오기
        List<String> imagePaths = instructor.getProfileImageUrl();
        //강사 status 변경
        instructor.setState(Instructor.InstructorState.WITHDRAWAL_COMPLETE);

        //이미지가 존재하면 삭제
        if (!imagePaths.isEmpty()) {
            for (String imagePath :
                    imagePaths) {
                String mediaPath = "media/";
                String fullPath = mediaPath + imagePath.replace("/static/", "");
                try {
                    Files.deleteIfExists(Path.of(fullPath));
                } catch (IOException e) {
                    log.error(e.getMessage());
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }
        instructor.setProfileImageUrl(new ArrayList<>());
        instructorRepository.save(instructor);
        InstructorDto.fromEntity(instructor);
    }

    //     강사 회원 탈퇴 거절
    public void withdrawCancel(Long instructorId) {
        Instructor instructor = instructorRepository.findById(instructorId).orElseThrow();
        instructor.setState(Instructor.InstructorState.ACTIVE);
        instructorRepository.save(instructor);
        InstructorDto.fromEntity(instructor);
    }

    //    MainController에서 사용
    public List<Instructor> findAll() {
        return instructorRepository.findAll();
    }

    public List<InstructorDto> findByThreeGoldMedals(List<Instructor> instructors) {
        List<InstructorDto> instructorDtos = new ArrayList<>();

        // 상태가 ACTIVE이고 메달이 Gold인 강사 가져오기
        for (Instructor instructor : instructors) {
            if (instructor.getMedal().equals("Gold") && instructor.getState() == Instructor.InstructorState.ACTIVE) {
                instructorDtos.add(InstructorDto.fromEntity(instructor));
            }
        }

        // 골드인 강사가 없을 때 ID가 1인 강사 가져오기
        if (instructorDtos.isEmpty()) {
            Instructor defaultInstructor = instructorRepository.findById(1L)
                    .orElseThrow(() -> new IllegalStateException("No instructor with ID 1 found"));
            instructorDtos.add(InstructorDto.fromEntity(defaultInstructor));
        } else {
            // 리스트를 랜덤으로 섞기
            Collections.shuffle(instructorDtos);
        }

        // 상위 3개만 선택
        List<InstructorDto> result = instructorDtos.stream().limit(3).collect(Collectors.toList());
        return result;

    }

}
