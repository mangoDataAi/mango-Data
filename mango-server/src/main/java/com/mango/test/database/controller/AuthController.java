package com.mango.test.database.controller;

import com.mango.test.database.service.AuthService;
import com.mango.test.vo.LoginVO;
import com.mango.test.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Api(tags = "认证接口")
@RestController
@RequestMapping("/api/sso")
@CrossOrigin  // 添加跨域支持
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    @ApiOperation(value = "用户登录", notes = "返回token、用户信息、菜单权限")
    public R<LoginVO> login(
            @ApiParam(value = "用户名", required = true) @RequestParam String username,
            @ApiParam(value = "密码", required = true) @RequestParam String password) {
        try {
            LoginVO loginVO = authService.login(username, password);
            log.info("用户[{}]登录成功", username);
            return R.ok(loginVO);
        } catch (Exception e) {
            log.error("用户[{}]登录失败: {}", username, e.getMessage());
            return R.fail(e.getMessage());
        }
    }
    
    @GetMapping("/info")
    @ApiOperation(value = "获取当前登录用户信息", notes = "返回用户信息、菜单和权限数据")
    public R<LoginVO> getUserInfo() {
        try {
            LoginVO userInfo = authService.getUserInfo();
            return R.ok(userInfo);
        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage());
            return R.fail(e.getMessage());
        }
    }
}
