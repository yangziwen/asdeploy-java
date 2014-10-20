package com.ablesky.asdeploy.action;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;

import com.ablesky.asdeploy.action.support.HttpMethod;
import com.ablesky.asdeploy.action.support.ModelMapActionSupport;
import com.ablesky.asdeploy.pojo.Role;
import com.ablesky.asdeploy.pojo.User;
import com.ablesky.asdeploy.pojo.UserRelRole;
import com.ablesky.asdeploy.service.IAuthorityService;
import com.ablesky.asdeploy.service.IUserService;
import com.ablesky.asdeploy.util.AuthUtil;

@ParentPackage("base")
@Namespace("/admin/user")
@Results({
	@Result(name="json", type="json", params={"root", "model"}),
	@Result(name="redirect", type="redirect", location="${redirectLocation}")
})
@SuppressWarnings("serial")
public class UserAdminAction extends ModelMapActionSupport {
	
	@Autowired
	private IUserService userService;
	@Autowired
	private IAuthorityService authorityService;
	
	@Action(value="list", results = {
		@Result(name="list", location="list.jsp")
	})
	public String list() {
		List<UserRelRole> superAdminRelList = authorityService.getUserRelRoleListResultByParam(0, 0, new ModelMap()
			.addAttribute("role_name", Role.NAME_SUPER_ADMIN)
		);
		ModelMap superAdminMap = new ModelMap();
		for(UserRelRole rel: superAdminRelList) {
			User user = rel.getUser();
			superAdminMap.put(user.getUsername(), user);
		}
		model.put("list", userService.getUserListResult(0, 0, Collections.<String, Object>emptyMap()));
		model.put("superAdminMap", superAdminMap);
		return "list";
	}
	
	@Action("switchSuperAdmin")
	public String switchSuperAdmin() {
		Long userId = getLongParam("userId");
		Boolean isSuperAdmin = getBooleanParam("isSuperAdmin");
		if(userId == null || isSuperAdmin == null) {
			model.put("success", false);
			model.put("message", "参数有误!");
			return "json";
		}
		if(!AuthUtil.isSuperAdmin()) {
			model.put("success", false);
			model.put("message", "没有权限!");
			return "json";
		}
		if(userId.equals(AuthUtil.getCurrentUser().getId())) {
			model.put("success", false);
			model.put("message", "不允许超级管理员将自身将为普通用户!");
			return "json";
		}
		if(!isSuperAdmin) {
			authorityService.deleteUserRelRoleByUserIdAndRoleName(userId, Role.NAME_SUPER_ADMIN);
			model.put("success", true);
			return "json";
		}
		User user = userService.getUserById(userId);
		Role role = authorityService.getRoleByName(Role.NAME_SUPER_ADMIN);
		UserRelRole superAdminRel = authorityService.addUserRelRoleByUserAndRole(user, role);
		if(superAdminRel == null){
			model.put("success", false);
			model.put("message", "用户或角色不存在!");
			return "json";
		}
		model.put("success", true);
		return "json";
	}
	
	@Action(value="/admin/user/changePassword/*", results = {
		@Result(name="changePassword", location="/WEB-INF/views/admin/user/changePassword.jsp")
	})
	public String changePassword() {
		Long userId = getLongParam("{1}");
		String method = request.getMethod();
		if(HttpMethod.POST.equals(method)) {
			doChangePassword(userId, getStringParam("newPassword"));
			return "json";
		} else {
			model.put("user", userService.getUserById(userId));
			return "changePassword";
		}
	}
	
	private void doChangePassword(Long userId, String newPassword) {
		if(!AuthUtil.isSuperAdmin()) {
			model.put("success", false);
			model.put("message", "没有权限!");
			return;
		}
		if(userId == null || userId <= 0 || StringUtils.isBlank(newPassword)) {
			model.put("success", false);
			model.put("message", "参数有误!");
			return;
		}
		User user = userService.getUserById(userId);
		if(user == null) {
			model.put("success", false);
			model.put("message", "用户不存在!");
			return;
		}
		user.setPassword(AuthUtil.hashPassword(user.getUsername(), newPassword));
		userService.saveOrUpdateUser(user);
		model.put("success", true);
		model.put("message", "修改成功!"); 
		return;
	}
}
