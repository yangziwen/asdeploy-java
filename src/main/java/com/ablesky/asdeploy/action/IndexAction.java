package com.ablesky.asdeploy.action;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Autowired;

import com.ablesky.asdeploy.security.jcaptcha.JCaptcha;
import com.ablesky.asdeploy.service.IDeployService;
import com.ablesky.asdeploy.service.IUserService;
import com.ablesky.asdeploy.util.AuthUtil;

@ParentPackage("base")
@SuppressWarnings("serial")
public class IndexAction extends ModelMapActionSupport {
	
	public static final String DEFAULT_SUCCESS_URL = "/main";
	
	@Autowired
	private IUserService userService;
	
	@Autowired
	private IDeployService deployService;
	
	@Action(results={@Result(location="main.jsp")})
	public String execute() {
		main();
		return SUCCESS;
	}
	
	@Action(value="main", results={
		@Result(name="main", location="main.jsp")
	})
	public String main() {
		modelMap.put("deployLock", deployService.checkCurrentLock());
		modelMap.put("currentUser", AuthUtil.getCurrentUser());
		modelMap.put("isSuperAdmin", AuthUtil.isSuperAdmin());
		return "main";
	}
	
	@Action(value="login", results={
		@Result(name="login", location="login.jsp"),
		@Result(name="json", type="json", params={"root", "model"})
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
		modelMap.put("success", false);
		modelMap.put("needLogin", true);
		modelMap.put("message", "需要登录或重新确认身份!");
		return "json";
	}
	
	private String doLogin() throws Exception {
		if(AuthUtil.isAuthenticated()) {
			org.apache.shiro.web.util.WebUtils.redirectToSavedRequest(request, response, DEFAULT_SUCCESS_URL);
			return NONE;
		} 
		if(AuthUtil.isRemembered()) {
			modelMap.put("errorMessage", "密码错误，请重试!");
		} else {
			modelMap.put("errorMessage", "用户名或密码错误，请重试!");
		}
		return "login";
	}
	
	@Action(value="register", results={
		@Result(name="register", location="register.jsp"),
		@Result(name="toMain", type="redirect", location=DEFAULT_SUCCESS_URL)
	})
	public String register() {
		String method = request.getMethod();
		if(HttpMethod.POST.equals(method)) {
			return doRegister();
		}
		return "register";
	}
	
	private String doRegister() {
		
		String username = getFromModel("username");
		String password = getFromModel("password");
		String confirmedPassword = getFromModel("confirmedPassword");
		String verifyCode = getFromModel("verifyCode");

		clearModel();
		
		if(StringUtils.isBlank(username)) {
			modelMap.put("usernameError", "用户名不能为空!");
		} else if(userService.getUserByUsername(username) != null) {
			modelMap.put("usernameError", "用户名已被占用!");
		}
		if(StringUtils.isBlank(password)) {
			modelMap.put("passwordError", "密码不能为空!");
		} else if (!password.equals(confirmedPassword)) {
			modelMap.put("confirmedPasswordError", "两次输入的密码不一致!");
		}
		if(StringUtils.isBlank(verifyCode)) {
			modelMap.put("verifyCodeError", "验证码不能为空!");
		} else if(!JCaptcha.validateResponse(request.getSession(), verifyCode)) {
			modelMap.put("verifyCodeError", "验证码输入错误!");
		}
		if(modelMap.size() > 0) {
			return "register";
		}
		userService.createNewUser(username, password);
		AuthUtil.login(username, password, true);
		return "toMain";
	}
	
	@Action(value="unauthorized", results={
		@Result(name="unauthorized", location="unauthorized.jsp"),
		@Result(name="json", type="json", params={"root", "model"})
	})
	public String unauthorized() {
		if(isAjax(request)) {
			modelMap.put("success", "false");
			modelMap.put("message", "没有权限!");
			return "json";
		} else {
			return "unauthorized";
		}
	}
	
	private boolean isAjax(HttpServletRequest request) {
		return BooleanUtils.toBoolean(request.getHeader("isAjax"));
	}
}
