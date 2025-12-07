package org.chandra.dmabackend.service.impl;

import org.chandra.dmabackend.dto.request.RegisterRequest;
import org.chandra.dmabackend.dto.response.RegisterResponse;
import org.chandra.dmabackend.model.User;
import org.chandra.dmabackend.repository.UserRepository;
import org.chandra.dmabackend.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Override
    public RegisterResponse register(RegisterRequest request){

        if(userRepository.existsByEmail(request.getEmail())){
            throw new IllegalArgumentException("Email already in use");
        }

        User user =  new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(bCryptPasswordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        RegisterResponse response = mapToResponse(savedUser);

        return response;

    }

    private RegisterResponse mapToResponse(User user){

        RegisterResponse response = new RegisterResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());

        return response;

    }

}
