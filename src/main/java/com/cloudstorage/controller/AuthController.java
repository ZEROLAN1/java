package com.cloudstorage.controller;

import com.cloudstorage.config.JwtUtil;
import com.cloudstorage.dto.AuthResponse;
import com.cloudstorage.dto.LoginRequest;
import com.cloudstorage.dto.RegisterRequest;
import com.cloudstorage.dto.ApiResponse;
import com.cloudstorage.model.User;
import com.cloudstorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import javax.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EntityManager entityManager;

    @Transactional
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            log.info("收到注册请求: username={}, email={}", request.getUsername(), request.getEmail());
            
            if (userRepository.existsByUsername(request.getUsername())) {
                log.warn("用户名已存在: {}", request.getUsername());
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "用户名已存在"));
            }
            
            if (userRepository.existsByEmail(request.getEmail())) {
                log.warn("邮箱已被注册: {}", request.getEmail());
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "邮箱已被注册"));
            }

            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            
            log.info("保存用户到数据库: {}", request.getUsername());
            User savedUser = userRepository.save(user);
            entityManager.flush(); // 强制刷新到数据库
            log.info("用户保存成功，ID: {}", savedUser.getId());
            
            // 验证用户确实已保存
            User verifyUser = userRepository.findByUsername(request.getUsername()).orElse(null);
            if (verifyUser == null) {
                log.error("验证失败：用户未保存到数据库！");
                throw new RuntimeException("数据保存失败");
            }
            log.info("验证成功：用户已存在于数据库，ID: {}", verifyUser.getId());

            String token = jwtUtil.generateToken(user.getUsername());
            AuthResponse response = new AuthResponse(token, user.getUsername(), user.getEmail());
            
            log.info("注册成功: {}", request.getUsername());
            return ResponseEntity.ok(new ApiResponse(true, "注册成功", response));
        } catch (Exception e) {
            log.error("注册失败: username={}, error={}", request.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "注册失败: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            log.info("收到登录请求: username={}", request.getUsername());
            
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

            String token = jwtUtil.generateToken(user.getUsername());
            AuthResponse response = new AuthResponse(token, user.getUsername(), user.getEmail());
            
            log.info("登录成功: {}", request.getUsername());
            return ResponseEntity.ok(new ApiResponse(true, "登录成功", response));
        } catch (Exception e) {
            log.error("登录失败: username={}, error={}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, "用户名或密码错误"));
        }
    }
}
