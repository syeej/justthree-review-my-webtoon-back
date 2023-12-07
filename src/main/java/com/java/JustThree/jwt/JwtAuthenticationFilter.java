package com.java.JustThree.jwt;

import com.java.JustThree.domain.Users;
import com.java.JustThree.dto.LoginRequest;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.minidev.json.JSONObject;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.util.Date;


public class JwtAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private static final AntPathRequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER =
            new AntPathRequestMatcher("/api/login", "POST");

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, JwtProperties jwtProperties) {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER, authenticationManager);
        this.authenticationManager = authenticationManager;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        System.out.println("JwtAuthenticationFilter -- attemptAuthentication 진입");

        // Login request 정보 받기
        LoginRequest loginReq = LoginRequest.builder()
                .usersEmail(request.getParameter("usersEmail"))
                .usersPw(request.getParameter("usersPw"))
                .build();

        // UsernamePasswordAuthenticationToken 생성
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginReq.getUsersEmail(), loginReq.getUsersPw());

		/*
		=> AuthenticationManager 에게 인증 요청 (UserDetailsService 통해 DB에 존재하는 유저인지 확인)
		1. UserDetailsService 의 loadUserByUsername() 호출
		2. loadUserByUsername() 에서 리턴 받은 UserDetails 객체와 authenticationToken 의
		   principal(사용자 입력 email), credentials(사용자 입력 password) 비교
		3. 비밀번호가 일치하면 Authentication 객체를 만들어서 필터체인으로 리턴, 일치하지 않으면 AuthenticationException 발생
		*/

        return authenticationManager.authenticate(authenticationToken);
    }

    // authenticate 성공
    // 전달된 Authentication 객체를 통해 JWT Token 생성한 후 Response Header 에 담아 전송
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authentication) throws IOException {

        System.out.println("JwtAuthenticationFilter -- successfulAuthentication 진입");

        Users userDetails = (Users) authentication.getPrincipal();

        String jwt = Jwts.builder()
                .header()
                .add("typ", "JWT")
                .and()
                .issuer(jwtProperties.getIssuer())
                .subject(userDetails.getUsersEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getEXPIRATION_TIME()))
                .claim("id", userDetails.getUsersId())
                .signWith(jwtProperties.getKEY())
                .compact();
        response.addHeader(jwtProperties.getHEADER_STRING(), jwtProperties.getTOKEN_PREFIX() + jwt);
        setSuccessResponse(response, userDetails);
    }

    // authenticate 성공 시 Response Body 에 담을 데이터
    private void setSuccessResponse(HttpServletResponse response, Users userDetails) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("nickname", userDetails.getUsersNickname());
        jsonObject.put("profileImg", userDetails.getProfileUrl());

        response.getWriter().print(jsonObject);
        response.getWriter().flush();
    }

    // authenticate 실패
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed)
            throws IOException {
        System.out.println("JwtAuthenticationFilter -- unsuccessfulAuthentication 진입");

        String failedMessage = failed.getMessage();
        setFailureResponse(response, failedMessage);
    }

    // authenticate 실패 시 Response Body 에 담을 데이터
    private void setFailureResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message", message);

        response.getWriter().print(jsonObject);
        response.getWriter().flush();
    }
}