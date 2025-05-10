package com.mango.test.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mango.test.system.entity.SysMenu;
import com.mango.test.system.entity.SysRole;
import com.mango.test.system.entity.SysUser;
import com.mango.test.system.entity.SysUserRole;
import com.mango.test.system.mapper.SysMenuMapper;
import com.mango.test.system.mapper.SysRoleMapper;
import com.mango.test.system.mapper.SysUserRoleMapper;
import com.mango.test.database.mapper.SysUserMapper;
import com.mango.test.database.service.AuthService;
import com.mango.test.util.BeanUtil;
import com.mango.test.util.JwtUtils;
import com.mango.test.vo.LoginVO;
import com.mango.test.vo.MenuVO;
import com.mango.test.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysMenuMapper menuMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(String username, String password) {
        log.info("用户[{}]开始登录", username);

        // 查询用户
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );

        // 用户不存在
        if (user == null) {
            log.warn("用户[{}]不存在", username);
            throw new RuntimeException("用户名或密码错误");
        }

        // admin用户不校验密码
        if (!"admin".equals(username)) {
            // 校验密码
            // 注意: 密码已经通过前端MD5加密，在这里进行比对
            if (!passwordEncoder.matches(password, user.getPassword())) {
                log.warn("用户[{}]密码错误", username);
                throw new RuntimeException("用户名或密码错误");
            }
        }

        // 校验状态
        if (!"1".equals(user.getStatus())) {
            log.warn("用户[{}]已被禁用", username);
            throw new RuntimeException("账号已被禁用");
        }

        // 生成token
        String token = jwtUtils.generateToken(user.getUsername());
        log.info("用户[{}]登录成功，生成token", username);

        // 设置cookie
        setCookie("TOKEN", token);

        // 返回登录信息
        LoginVO loginVO = new LoginVO().setToken(token);

        // 复制属性并手动处理status转换
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        userVO.setStatus("1".equals(user.getStatus()));

        loginVO.setUser(userVO);

        // 获取用户菜单和权限
        setMenusAndPermissions(loginVO, user);

        return loginVO;
    }

    @Override
    public LoginVO getUserInfo() {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未登录");
        }

        String username = authentication.getName();
        log.info("获取用户[{}]信息", username);

        // 查询用户
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );

        if (user == null) {
            log.warn("用户[{}]不存在", username);
            throw new RuntimeException("用户不存在");
        }

        // 返回用户信息
        LoginVO loginVO = new LoginVO();

        // 复制属性并手动处理status转换
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        userVO.setStatus("1".equals(user.getStatus()));

        loginVO.setUser(userVO);

        // 获取用户菜单和权限
        setMenusAndPermissions(loginVO, user);

        return loginVO;
    }

    /**
     * 设置用户菜单和权限
     */
    private void setMenusAndPermissions(LoginVO loginVO, SysUser user) {
        List<MenuVO> menuList = new ArrayList<>();
        List<String> permissionList = new ArrayList<>();


        // 查询用户的角色ID
        List<String> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, user.getId())
        ).stream().map(SysUserRole::getRoleId).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(roleIds)) {
            log.info("用户[{}]的角色IDs: {}", user.getUsername(), roleIds);

            // 查询角色对应的菜单ID
            List<String> menuIds = menuMapper.selectMenuIdsByRoleIds(roleIds);

            if (!CollectionUtils.isEmpty(menuIds)) {
                // 查询菜单详情
                List<SysMenu> userMenus = menuMapper.selectList(
                        new LambdaQueryWrapper<SysMenu>()
                                .in(SysMenu::getId, menuIds)
                                .eq(SysMenu::getStatus, 1)
                                .orderByAsc(SysMenu::getSort)
                );

                // 构建菜单树
                menuList = buildMenuTree(userMenus);

                // 提取权限标识
                permissionList = userMenus.stream()
                        .filter(menu -> menu.getPath() != null && !menu.getPath().isEmpty())
                        .map(menu -> "menu:" + menu.getPath())
                        .collect(Collectors.toList());
            }
        }

        loginVO.setMenus(menuList);
        loginVO.setPermissions(permissionList);
    }

    /**
     * 构建菜单树
     */
    private List<MenuVO> buildMenuTree(List<SysMenu> menuList) {
        List<MenuVO> resultList = new ArrayList<>();
        if (CollectionUtils.isEmpty(menuList)) {
            return resultList;
        }

        // 转换成VO
        List<MenuVO> menuVOList = menuList.stream().map(menu -> {
            MenuVO menuVO = new MenuVO();
            menuVO.setId(Long.valueOf(menu.getId()));
            menuVO.setName(menu.getName());
            menuVO.setPath(menu.getPath());
            menuVO.setComponent(menu.getComponent());
            menuVO.setIcon(menu.getIcon());
            menuVO.setSort(menu.getSort());
            menuVO.setParentId(menu.getParentId() != null ? Long.valueOf(menu.getParentId()) : null);
            menuVO.setHidden(menu.getIsVisible() != null && menu.getIsVisible() == 0);
            // 使用path作为权限
            menuVO.setPermission(menu.getPath());
            return menuVO;
        }).collect(Collectors.toList());

        // 构建树结构
        Map<Long, MenuVO> menuMap = menuVOList.stream()
                .collect(Collectors.toMap(MenuVO::getId, menu -> menu));

        for (MenuVO menu : menuVOList) {
            // 如果是顶级菜单，加入结果集
            if (menu.getParentId() == null || menu.getParentId() == 0) {
                resultList.add(menu);
            } else {
                // 非顶级菜单，找到父级菜单并添加到其子菜单中
                MenuVO parentMenu = menuMap.get(menu.getParentId());
                if (parentMenu != null) {
                    if (parentMenu.getChildren() == null) {
                        parentMenu.setChildren(new ArrayList<>());
                    }
                    parentMenu.getChildren().add(menu);
                }
            }
        }

        // 排序
        resultList.sort(Comparator.comparing(MenuVO::getSort, Comparator.nullsLast(Integer::compareTo)));

        return resultList;
    }

    private void setCookie(String name, String value) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletResponse response = attributes.getResponse();
            if (response != null) {
                Cookie cookie = new Cookie(name, value);
                cookie.setPath("/");
                cookie.setHttpOnly(true);
                response.addCookie(cookie);
            }
        }
    }
}
