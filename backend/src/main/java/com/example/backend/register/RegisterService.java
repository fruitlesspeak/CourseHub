package com.example.backend.register;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegisterService {

    private final UserRepo userRepo;
    private final PasswordEncoder encoder;

    public RegisterService(UserRepo userRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    public UserResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();

        if (userRepo.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyInUseException();
        }

        User user = new User(
                req.name().trim(),
                email,
                encoder.encode(req.password()),
                req.role()
        );

        user = userRepo.save(user);
        return UserResponse.from(user);
    }
}
