package com.ablesky.asdeploy.security;

import java.util.List;

import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.cas.CasRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

import com.ablesky.asdeploy.pojo.Role;
import com.ablesky.asdeploy.pojo.User;
import com.ablesky.asdeploy.service.IAuthorityService;
import com.ablesky.asdeploy.service.IUserService;

public class ShiroCasRealm extends CasRealm {
	
	@Autowired
	private IUserService userService;
	@Autowired
	private IAuthorityService authorityService;
	
	@Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		String username = (String)principals.getPrimaryPrincipal();
		User user = userService.getUserByUsername(username);
		SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
		if(user != null) {
			List<Role> roleList = authorityService.getRoleListResultByUserId(user.getId());
			for(Role role: roleList) {
				info.addRole(role.getName());
			}
		}
		return info;
    }

}
