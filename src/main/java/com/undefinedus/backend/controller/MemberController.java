package com.undefinedus.backend.controller;

import com.undefinedus.backend.domain.entity.AladinBook;
import com.undefinedus.backend.dto.MemberSecurityDTO;
import com.undefinedus.backend.dto.request.book.BookRequestDTO;
import com.undefinedus.backend.dto.request.member.PasswordUpdateRequestDTO;
import com.undefinedus.backend.dto.request.social.RegisterRequestDTO;
import com.undefinedus.backend.dto.response.ApiResponseDTO;
import com.undefinedus.backend.service.EmailService;
import com.undefinedus.backend.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/member")
@Tag(name = "Member", description = "회원 관리 API")
public class MemberController {
    
    private final MemberService memberService;
    private final EmailService emailService;
    
    @Operation(
            summary = "일반 회원가입",
            description = "이메일과 비밀번호를 사용하여 일반 회원가입을 진행합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원가입 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    type = "object",
                                    example = """
                                            {
                                                "result": "success",
                                                "member": {
                                                    "username": "user@example.com",
                                                    "password": "[PROTECTED]",
                                                    "nickname": "사용자닉네임",
                                                    "socialLoginDTO": null,
                                                    "memberRoleList": ["USER"]
                                                }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "회원가입 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    type = "object",
                                    example = """
                                            {
                                                "result": "fail"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/register")
    public Map<String, Object> memberRegister(
            @Parameter(description = "회원가입 요청 정보", required = true)
            @RequestBody RegisterRequestDTO requestDTO) {
        Map<String, Object> result = new HashMap<>();
        try {
            MemberSecurityDTO memberSecurityDTO = memberService.regularRegister(requestDTO);
            result.put("member", memberSecurityDTO);
            result.put("result", "success");
            return result;
        } catch (Exception e) {
            return Map.of("result", "fail");
        }
    }
    
    @PostMapping("/email/send-verification")
    public Map<String, String> sendVerificationEmail(
            @RequestParam String email) {
        try {
            emailService.sendVerificationEmail(email);
            return Map.of(
                    "result", "success",
                    "message", "인증 메일이 발송되었습니다."
            );
        } catch (Exception e) {
            return Map.of(
                    "result", "error",
                    "message", "메일 발송 중 오류가 발생했습니다."
            );
        }
    }
    
    @PostMapping("/email/verify")
    public Map<String, String> verifyEmail(
            @RequestParam String email,
            @RequestParam String code) {
        if (emailService.verifyCode(email, code)) {
            return Map.of(
                    "status", "success",
                    "message", "이메일 인증이 완료되었습니다."
            );
        }
        return Map.of(
                "status", "error",
                "message", "잘못된 인증 코드이거나 만료되었습니다."
        );
    }
    
    @DeleteMapping
    public ResponseEntity<ApiResponseDTO<Void>> deleteMember(@AuthenticationPrincipal MemberSecurityDTO memberSecurityDTO) {
        memberService.deleteMember(memberSecurityDTO.getId());
        return ResponseEntity.ok(ApiResponseDTO.success(null));
    }
    
    @PatchMapping("/updatePassword")
    public ResponseEntity<ApiResponseDTO<Void>> updatePassword(@RequestBody PasswordUpdateRequestDTO requestDTO) {
        memberService.updatePassword(requestDTO);
        
        return ResponseEntity.ok(ApiResponseDTO.success(null));
    }
}
