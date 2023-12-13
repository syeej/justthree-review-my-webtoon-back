package com.java.JustThree.service;

import com.java.JustThree.config.RedisUtil;
import com.java.JustThree.domain.RefreshToken;
import com.java.JustThree.dto.JoinRequest;
import com.java.JustThree.dto.RoleType;
import com.java.JustThree.domain.Users;
import com.java.JustThree.dto.UsersResponse;
import com.java.JustThree.jwt.JwtProperties;
import com.java.JustThree.jwt.JwtProvider;
import com.java.JustThree.repository.RefreshTokenRepository;
import com.java.JustThree.repository.UsersRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j

public class UsersService {

    private final UsersRepository ur;
    private final RefreshTokenRepository rtr;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtProperties jwtProperties;
    private final JwtProvider jwtProvider;
    private final RedisUtil redisUtil;
    private final EmailService emailService;


    @Transactional(rollbackFor = Exception.class)
    public Long insertUsers(JoinRequest joinDTO){
        Users user = Users.builder()
            .usersEmail(joinDTO.getUsersEmail())
            .usersPw(bCryptPasswordEncoder.encode(joinDTO.getUsersPw()))
            .usersNickname(joinDTO.getUsersNickname())
            .usersRole(RoleType.USER.name())
            .build();
        return  ur.save(user).getUsersId();
    }

    public UsersResponse getUserInfo(String token){

        Users user = ur.findById(jwtProvider.getUserId(token)).get();

        return user.toDto();
    }

    @Transactional
    public void logout(String usersEmail) {
        rtr.deleteByUser_UsersEmail(usersEmail);
    }

    public String something(String token){

            log.info("아이디가 도착했습니다");
            log.info(jwtProvider.getUserId(token)+"");

        return "";
    }

    public String getNewAccessToken(String token){
        String userEmail = jwtProvider.getUserEmail(token);
//        jwtProvider.createAccessToken();
        RefreshToken refreshToken = rtr.findByUser_UsersEmail(userEmail);
        return refreshToken.getRefreshToken();
    }

    public String generateRandomNumber() {
        return String.valueOf((int) (Math.random() * 10000));
    } // 난수 생성

    public String generateToken() {
        return UUID.randomUUID().toString();
    }  // 토큰 생성
    // 이메일 전송 (5분 제한) -> 회원가입, 비밀번호 재설정
    public void sendJoinEmail(String email) {
        String randomNumber = generateRandomNumber();
        redisUtil.setDataExpire(randomNumber, email, 300000);
        emailService.sendNumberEmail(email, randomNumber);

    }

    public void sendResetPasswordEmail(String email) throws MessagingException {
        String token = generateToken();
        redisUtil.setDataExpire(token, email, 300000);
        emailService.sendLinkEmail(email, token);
    }

    // 토큰 및 난수 인증 (Redis)
    public String validateEmailVerification(String key) {
        String email = redisUtil.getData(key);
        if (email == null) { // 잘못된 인증번호(or 토큰)이거나 인증 시간 만료
            throw new IllegalStateException("인증 시간 만료 또는 잘못된 입력입니다.");
        }
        return email;
    }

    // 이메일 중복 체크
    public boolean validateDuplicateEmail(String email) {
        Optional<Users> user = ur.findByUsersEmail(email);
        return user.isPresent();
    }

    public boolean validateDuplicateNickname(String nickname) {
        Optional<Users> user = ur.findByUsersNickname(nickname);
        return user.isPresent();
    }
}
