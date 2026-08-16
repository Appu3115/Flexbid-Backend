package com.example.flexbid.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
//import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
//import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flexbid.dto.UserDTO;
import com.example.flexbid.model.EmailVerificationToken;
import com.example.flexbid.model.LoginRequest;
import com.example.flexbid.model.LoginResponse;
import com.example.flexbid.model.PasswordResetToken;
import com.example.flexbid.model.RegisterRequest;
import com.example.flexbid.model.Role;
import com.example.flexbid.model.User;
import com.example.flexbid.model.UserRole;
import com.example.flexbid.repository.BidRepository;
import com.example.flexbid.repository.EmailVerificationTokenRepository;
import com.example.flexbid.repository.OrderRepository;
import com.example.flexbid.repository.PasswordResetRepository;
import com.example.flexbid.repository.ServiceBuyerRepository;
import com.example.flexbid.repository.UserRepository;
import com.example.flexbid.repository.UserRoleRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordResetRepository passResetRepo;
    
    @Autowired
    private EmailVerificationTokenRepository tokenRepo;
    
    @Autowired
    private UserRoleRepository userRoleRepo;

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    private WebSocketNotificationService wsNotificationService;

    

 // ✅ REGISTER USER
    public ResponseEntity<?> registerUser(RegisterRequest request) {
        if (userRepo.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        if (!isValidEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Invalid email format");
        }

        String contactStr = String.valueOf(request.getContact());
        if (contactStr.length() != 10 || !contactStr.matches("\\d{10}")) {
            return ResponseEntity.badRequest().body("Contact must be exactly 10 digits");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body("Passwords do not match");
        }

        Set<String> roles = request.getRoles();
        Set<Role> roleSet = new HashSet<>();
        for (String role : roles) {
            String roleStr = role.toUpperCase();
            if (!roleStr.equals("BUYER") && !roleStr.equals("SELLER") && !roleStr.equals("ADMIN")) {
                return ResponseEntity.badRequest().body("Invalid role. Allowed roles: BUYER, SELLER, ADMIN");
            }
            if (roleStr.equals("ADMIN") && userRoleRepo.existsByRole(Role.ADMIN)) {
                return ResponseEntity.badRequest().body("Admin already exists");
            }
            roleSet.add(Role.valueOf(roleStr));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setContact(request.getContact());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false);
        user.setCreatedAt(LocalDateTime.now());

        user = userRepo.save(user);

        for (Role role : roleSet) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepo.save(userRole);
        }

        String otp = generateOtp();
        EmailVerificationToken evToken = new EmailVerificationToken();
        evToken.setToken(otp);
        evToken.setUserId(user.getId());
        evToken.setExpiryTime(LocalDateTime.now().plusMinutes(15));
        evToken.setUsed(false);

        tokenRepo.save(evToken);
        try {
            sendVerificationOtpEmail(user.getEmail(), otp);
        } catch (Exception e) {

            e.printStackTrace();

            tokenRepo.delete(evToken);
            userRepo.delete(user);

            return ResponseEntity
                    .internalServerError()
                    .body(e.getClass().getName() + " : " + e.getMessage());
        }

        wsNotificationService.notifyTopic("admin", Map.of(
            "type", "NEW_USER_REGISTRATION",
            "message", "A new user has registered: " + user.getUsername(),
            "email", user.getEmail(),
            "username", user.getUsername(),
            "roles", roleSet.stream().map(Enum::name).toArray(),
            "timestamp", LocalDateTime.now().toString()
        ));

        return ResponseEntity.ok("OTP sent to your email. Please verify to complete registration.");
    }

//    private void sendVerificationOtpEmail(String to, String otp) throws MessagingException {
//        String subject = "Your FlexBid OTP for Email Verification";
//
//        String content = """
//            <p>Hello,</p>
//            <p>Your OTP for email verification is:</p>
//
//            <div style="padding: 10px; background-color: #f4f4f4; border: 1px dashed #999; display: inline-block; border-radius: 6px; margin: 10px 0;">
//                <span style="font-family: monospace; font-size: 24px; letter-spacing: 3px; color: #333;">%s</span>
//            </div>
//
//            <p style="margin-top: 15px;"><i>You can copy the OTP above and paste it into the app.</i></p>
//            <p>This OTP is valid for 15 minutes.</p>
//            <p>If you did not request this, please ignore this email.</p>
//        """.formatted(otp);
//
//        MimeMessage message = mailSender.createMimeMessage();
//        MimeMessageHelper helper = new MimeMessageHelper(message, true);
//        helper.setTo(to);
//        helper.setSubject(subject);
//        helper.setText(content, true); // HTML content
//
//        // Optional: Debug logs
//        System.out.println("Sending OTP to: " + to);
//        System.out.println("OTP: " + otp);
//
//        mailSender.send(message);
//    }

    private void sendVerificationOtpEmail(String to, String otp) throws MessagingException {

        System.out.println("STEP 1");

        String subject = "Your FlexBid OTP";

        MimeMessage message = mailSender.createMimeMessage();

        System.out.println("STEP 2");

        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        System.out.println("STEP 3");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText("OTP : " + otp);

        System.out.println("STEP 4");

        try {
            mailSender.send(message);
            System.out.println("STEP 5 EMAIL SENT");
        } catch (Exception e) {
            e.printStackTrace();

            System.out.println("MAIL ERROR = " + e.getClass().getName());
            System.out.println("MESSAGE = " + e.getMessage());

            throw e;
        }
    }

    private String generateOtp() {
        SecureRandom secureRandom = new SecureRandom();
        int otp = 100000 + secureRandom.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }


    public ResponseEntity<?> verifyEmail(String token) {
        Optional<EmailVerificationToken> optionalToken = tokenRepo.findByToken(token);
        
        if (optionalToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "error", "Invalid verification token."
            ));
        }

        if (optionalToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "error", "Invalid or expired OTP."
            ));
        }

        EmailVerificationToken evToken = optionalToken.get();

        if (evToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "error", "OTP has expired."
            ));
        }

        Optional<User> optionalUser = userRepo.findById(evToken.getUserId());
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "error", "User not found."
            ));
        }

        User user = optionalUser.get();
        user.setEmailVerified(true);
        userRepo.save(user);

        evToken.setUsed(true);
        tokenRepo.save(evToken);

        wsNotificationService.sendToUser(
            user.getEmail(),
            Map.of(
                "type", "EMAIL_VERIFIED",
                "message", "Your email has been successfully verified.",
                "timestamp", LocalDateTime.now().toString()
            )
        );

        return ResponseEntity.ok(Map.of(
            "message", "Email verified successfully. You can now login.",
            "success", true,
            "userId", user.getId(),
            "email", user.getEmail()
        ));
    }




    // ✅ LOGIN
    public ResponseEntity<?> loginUser(LoginRequest request) {
        Optional<User> optionalUser = userRepo.findByEmail(request.getEmail());

        if (!isValidEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Invalid email format");
        }

        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        User user = optionalUser.get();

        if (!user.isEmailVerified()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Please verify your email before logging in.");
        }

        // Compare raw password with hashed password using BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        String roleStr = request.getRole().toUpperCase();
        Role requestedRole;

        try {
            requestedRole = Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid role. Allowed roles: ADMIN, BUYER, SELLER");
        }

        // Check if user has the requested role
        boolean hasRole = userRoleRepo.existsByUserIdAndRole(user.getId(), requestedRole);
        if (!hasRole) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User does not have the " + roleStr + " role");
        }

        // Success response
        LoginResponse response = new LoginResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setUsername(user.getUsername());
        response.setRoles(Set.of(roleStr)); // single role used during this session
        response.setMessage("Login successful as " + roleStr);
        
        // ✅ Send WebSocket notification (e.g., to "/topic/login")
        wsNotificationService.notifyTopic("login", Map.of(
            "type", "USER_LOGIN",
            "userId", user.getId(),
            "username", user.getUsername(),
            "role", roleStr,
            "email", user.getEmail(),
            "timestamp", LocalDateTime.now().toString()
        ));

        return ResponseEntity.ok(response);
    }


    // ✅ FORGOT PASSWORD with Email Sending
    public ResponseEntity<?> forgotPassword(String email) {
        Optional<User> optionalUser = userRepo.findByEmail(email);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().body("Email not registered");
        }

        User user = optionalUser.get();

        // Mark all previous tokens as used
        List<PasswordResetToken> existingTokens = passResetRepo.findByUserId(user.getId());
        for (PasswordResetToken token : existingTokens) {
            token.setUsed(true);
        }
        passResetRepo.saveAll(existingTokens);

        // Generate OTP
        String otp = generateOtp(); // 6-digit OTP

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(user.getId());
        resetToken.setToken(otp);
        resetToken.setExpiryTime(LocalDateTime.now().plusMinutes(15));
        resetToken.setUsed(false);

        passResetRepo.save(resetToken);

        // Send OTP via email
        try {
        	sendResetOtpEmail(user.getEmail(), otp);
        } catch (MessagingException e) {
            return ResponseEntity.internalServerError().body("Error sending email: " + e.getMessage());
        }

        wsNotificationService.notifyTopic("security", Map.of(
            "type", "FORGOT_PASSWORD",
            "userId", user.getId(),
            "email", user.getEmail(),
            "timestamp", LocalDateTime.now().toString()
        ));

        return ResponseEntity.ok("OTP sent to your email for password reset");
    }


    // ✅ RESET PASSWORD
    public ResponseEntity<?> resetPassword(String token, String newPassword, String confirmPassword) {
        Optional<PasswordResetToken> optionalToken = passResetRepo.findByToken(token);

        if (optionalToken.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid token");
        }

        PasswordResetToken resetToken = optionalToken.get();

        if (resetToken.isUsed()) {
            return ResponseEntity.badRequest().body("Token already used");
        }

        if (resetToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Token expired");
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body("Passwords do not match");
        }

        Optional<User> optionalUser = userRepo.findById(resetToken.getUserId());
        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = optionalUser.get();

        // Encrypt the new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passResetRepo.save(resetToken);
        
        wsNotificationService.notifyTopic("security", Map.of(
                "type", "PASSWORD_RESET",
                "userId", user.getId(),
                "email", user.getEmail(),
                "timestamp", LocalDateTime.now().toString()
            ));

        return ResponseEntity.ok("Password reset successful");
    }
    // ✅ LIST ALL USERS
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepo.findAll();

        List<UserDTO> userDTOs = new ArrayList<>();

        for (User user : users) {
            List<UserRole> rolesList = userRoleRepo.findByUserId(user.getId());
            Set<String> roles = rolesList.stream()
                    .map(role -> role.getRole().name())
                    .collect(Collectors.toSet());

            UserDTO dto = new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getContact(),
                roles,
                user.isEmailVerified()
            );

            userDTOs.add(dto);
        }
        
        wsNotificationService.notifyTopic("ADMIN", Map.of(
                "type", "USER_LIST_ACCESSED",
                "timestamp", LocalDateTime.now().toString(),
                "userCount", userDTOs.size()
            ));

        return ResponseEntity.ok(userDTOs);
    }

    // ✅ SEND EMAIL 
    private void sendResetOtpEmail(String to, String otp) throws MessagingException {
        String subject = "FlexBid Password Reset OTP";

        String content = """
            <p>Hello,</p>
            <p>Your OTP to reset your password is:</p>

            <div style="padding: 10px; background-color: #f4f4f4; border: 1px dashed #999; display: inline-block; border-radius: 6px; margin: 10px 0;">
                <span style="font-family: monospace; font-size: 24px; letter-spacing: 3px; color: #2c3e50;">%s</span>
            </div>

            <p style="margin-top: 15px;"><i>You can copy the OTP above and paste it into the app.</i></p>
            <p>This OTP will expire in 15 minutes.</p>
            <p>If you didn’t request this, please ignore this email.</p>
        """.formatted(otp);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true); // Enable HTML content

        mailSender.send(message);
    }



    

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email != null && email.matches(emailRegex);
    }

//    @Scheduled(cron = "0 */15 * * * *") // Every 15 minutes
//    @Transactional
//    public void removeUnverifiedUsers() {
//        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(15);
//
//        // Fetch unverified users created more than 15 minutes ago
//        List<User> unverifiedUsers = userRepo.findByEmailVerifiedFalseAndCreatedAtBefore(cutoffTime);
//
//        List<String> deletedEmails = new ArrayList<>();
//
//        for (User user : unverifiedUsers) {
//            deletedEmails.add(user.getEmail());
//
//            // Delete any email verification tokens related to this user
//            tokenRepo.deleteByUserId(user.getId());
//
//            // Delete the user
//            userRepo.delete(user);
//        }
//
//        // Notify admins if any users were removed
//        if (!deletedEmails.isEmpty()) {
//            wsNotificationService.notifyTopic("admin", Map.of(
//                "type", "UNVERIFIED_USERS_REMOVED",
//                "count", deletedEmails.size(),
//                "emails", deletedEmails,
//                "timestamp", LocalDateTime.now().toString()
//            ));
//        }
//    }
//    
    
    @Autowired
    private BidRepository bidRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ServiceBuyerRepository serviceBuyerRepo;
    
    @Transactional
    public ResponseEntity<?> deleteUserByAdmin(Integer userId) {
        Optional<User> optionalUser = userRepo.findById(userId);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        User user = optionalUser.get();
        String deletedEmail = user.getEmail();

        // Step 1: Delete or detach dependent data (adjust based on your schema)
        serviceBuyerRepo.deleteByBuyerId(userId);
        
		bidRepository.deleteByBuyerId(userId);// DELETE requests where buyer_id = userId
        orderRepository.deleteByUserId(userId);           // DELETE orders
        // ... add more if needed

        // Step 2: Delete tokens/roles
        tokenRepo.deleteByUserId(userId);
        passResetRepo.deleteByUserId(userId);
        userRoleRepo.deleteByUserId(userId);

        // Step 3: Delete the user
        userRepo.delete(user);

        // Step 4: WebSocket notification
        wsNotificationService.notifyTopic("admin", Map.of(
            "type", "USER_DELETED_BY_ADMIN",
            "userId", userId,
            "email", deletedEmail,
            "timestamp", LocalDateTime.now().toString()
        ));

        return ResponseEntity.ok("User deleted successfully.");
    }


}
