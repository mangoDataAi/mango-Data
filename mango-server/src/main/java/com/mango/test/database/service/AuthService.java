package com.mango.test.database.service;

import com.mango.test.vo.LoginVO;

public interface AuthService {
    LoginVO login(String username, String password);
    LoginVO getUserInfo();
}
