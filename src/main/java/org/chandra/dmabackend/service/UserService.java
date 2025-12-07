package org.chandra.dmabackend.service;

import org.chandra.dmabackend.dto.request.LoginRequest;
import org.chandra.dmabackend.dto.request.RegisterRequest;
import org.chandra.dmabackend.dto.response.LoginResponse;
import org.chandra.dmabackend.dto.response.RegisterResponse;

public interface UserService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

}
