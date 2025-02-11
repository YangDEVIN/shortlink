package com.shortlink.admin.controller;

import com.fasterxml.jackson.databind.ser.std.JsonValueSerializer;
import com.shortlink.admin.common.convention.Result;
import com.shortlink.admin.common.convention.Results;
import com.shortlink.admin.dto.respond.UserRespDTO;
import com.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户和管理控制器
 */
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 根据用户名查询用户信息
     */

    @GetMapping("/api/short-link/admin/v1/user/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable("username") String username){
        UserRespDTO result = userService.getUserByUsername(username);
        return Results.success(result);
    }
}
