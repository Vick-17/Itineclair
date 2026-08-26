package com.projectspring.api.Controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectspring.api.Dto.UserDto;
import com.projectspring.api.Generic.GenericController;
import com.projectspring.api.Services.UserService;


@RestController
@RequestMapping("/users")
public class UserController extends GenericController<UserDto, Integer, UserService> {
    public UserController(UserService service) {
        super(service);
    }
    
    @PostMapping("/register")
    public UserDto regiUser(@RequestBody UserDto user) {
        return service.createUser(user);
    }

}
