package com.ablesky.asdeploy.action;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.springframework.beans.factory.annotation.Autowired;

import com.ablesky.asdeploy.action.support.HttpMethod;
import com.ablesky.asdeploy.action.support.ModelMapActionSupport;
import com.ablesky.asdeploy.security.jcaptcha.JCaptcha;
import com.ablesky.asdeploy.service.IDeployService;
import com.ablesky.asdeploy.service.IUserService;
import com.ablesky.asdeploy.util.AuthUtil;

@ParentPackage("base")
@Results({
	@Result(name="json", type="json", params={"root", "model"}),
	@Result(name="redirect", type="redirect", location="${redirectLocation}")
})
@SuppressWarnings("serial")
public class IndexAction extends ModelMapActionSupport {
	
	public static final String DEFAULT_SUCCESS_URL = "/main";
	
	@Autowired
	private IUserService userService;
	
	@Autowired
	private IDeployService deployService;
	
	@Action(results={
		@Result(name="mainRedirect", type="redirectAction", location="main"),
		@Result(name="loginRedirect", type="redirectAction", location="login")
	})
	public String execute() {
		return AuthUtil.isUser()
			? "mainRedirect"
			: "loginRedirect";
	}
	
	@Action(value="main", results={
		@Result(name="main", location="main.jsp")
	})
	public String main() {
		model.put("deployLock", deployService.checkCurrentLock());
		model.put("currentUser", AuthUtil.getCurrentUser());
		model.put("isSuperAdmin", AuthUtil.isSuperAdmin());
		return "main";
	}
	
	@Action(value="login", results={
		@Result(name="login", location="login.jsp"),
	})
	public String login() throws Exception {
		String method = request.getMethod();
		
		if(HttpMethod.POST.equals(method)) {
			
			return doLogin();
			
		} else if (HttpMethod.GET.equals(method) && isAjax(request)) {
			
			return toLoginByAjax();
			
		}
		return toLogin();
	}
	
	private String toLogin() throws Exception {
		if(AuthUtil.isAuthenticated()) {
			org.apache.shiro.web.util.WebUtils.redirectToSavedRequest(request, response, DEFAULT_SUCCESS_URL);
			return NONE;
		}
		return "login";
	}
	
	private String toLoginByAjax() {
		model.put("success", false);
		model.put("needLogin", true);
		model.put("message", "需要登录或重新确认身份!");
		return "json";
	}
	
	private String doLogin() throws Exception {
		if(AuthUtil.isAuthenticated()) {
			org.apache.shiro.web.util.WebUtils.redirectToSavedRequest(request, response, DEFAULT_SUCCESS_URL);
			return NONE;
		} 
		if(AuthUtil.isRemembered()) {
			model.put("errorMessage", "密码错误，请重试!");
		} else {
			model.put("errorMessage", "用户名或密码错误，请重试!");
		}
		return "login";
	}
	
	@Action(value="register", results={
		@Result(name="register", location="register.jsp")
	})
	public String register() {
		String method = request.getMethod();
		if(HttpMethod.POST.equals(method)) {
			return doRegister();
		}
		return "register";
	}
	
	private String doRegister() {
		
		String username = getStringParam("username");
		String password = getStringParam("password");
		String confirmedPassword = getStringParam("confirmedPassword");
		String verifyCode = getStringParam("verifyCode");

		clearModel();
		
		if(StringUtils.isBlank(username)) {
			model.put("usernameError", "用户名不能为空!");
		} else if(userService.getUserByUsername(username) != null) {
			model.put("usernameError", "用户名已被占用!");
		}
		if(StringUtils.isBlank(password)) {
			model.put("passwordError", "密码不能为空!");
		} else if (!password.equals(confirmedPassword)) {
			model.put("confirmedPasswordError", "两次输入的密码不一致!");
		}
		if(StringUtils.isBlank(verifyCode)) {
			model.put("verifyCodeError", "验证码不能为空!");
		} else if(!JCaptcha.validateResponse(request.getSession(), verifyCode)) {
			model.put("verifyCodeError", "验证码输入错误!");
		}
		if(model.size() > 0) {
			return "register";
		}
		userService.createNewUser(username, password);
		AuthUtil.login(username, password, true);
		model.put("redirectLocation", DEFAULT_SUCCESS_URL);
		return "redirect";
	}
	
	@Action(value="unauthorized", results={
		@Result(name="unauthorized", location="unauthorized.jsp")
	})
	public String unauthorized() {
		if(isAjax(request)) {
			model.put("success", "false");
			model.put("message", "没有权限!");
			return "json";
		} else {
			return "unauthorized";
		}
	}
	
	private boolean isAjax(HttpServletRequest request) {
		return BooleanUtils.toBoolean(request.getHeader("isAjax"));
	}
}
