package com.ablesky.asdeploy.action;


import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.springframework.beans.factory.annotation.Autowired;

import com.ablesky.asdeploy.action.support.HttpMethod;
import com.ablesky.asdeploy.action.support.ModelMapActionSupport;
import com.ablesky.asdeploy.pojo.Project;
import com.ablesky.asdeploy.service.IProjectService;

@ParentPackage("base")
@Namespace("/admin/project")
@Results({
	@Result(name="json", type="json", params={"root", "model"}),
	@Result(name="redirect", type="redirect", location="${redirectLocation}")
})
@SuppressWarnings("serial")
public class ProjectAdminAction extends ModelMapActionSupport {
	
	@Autowired
	private IProjectService projectService;
	
	@Action(value="list", results = {
		@Result(name="list", location="list.jsp")
	})
	public String list() {
		model.put("list", projectService.getProjectListResult(0, 0, Collections.<String, Object>emptyMap()));
		return "list";
	}
	
	@Action(value="/admin/project/edit/*", results = {
		// 非常令人费解，struts有bug么？
		@Result(name="edit", location="/WEB-INF/views/admin/project/edit.jsp")
	})
	public String toEdit() {
		if(HttpMethod.POST.equals(request.getMethod())) {
			doEdit();
			return "json";
		} else {
			Long id = NumberUtils.toLong(getPathVariable(1));
			if(id != null && id > 0){
				// to edit existed project
				model.put("project", projectService.getProjectById(id));
			}
			return "edit";
		}
	}
	
	@Action(value="edit", results = {
		@Result(name="edit", location="edit.jsp"),
	})
	public String edit() {
		// to edit for new project
		return "edit";
	}
	
	private void doEdit() {
		Long id = getLongParam("id");
		String name = getStringParam("name");
		String warName = getStringParam("warName");
		if(StringUtils.isBlank(name) || StringUtils.isBlank(warName)) {
			model.put("success", false);
			model.put("message", "项目名称和包名称不能为空!");
			return;
		}
		Project project = null;
		if(id != null && id > 0) {
			project = projectService.getProjectById(id);
			if(project == null) {
				model.put("success", false);
				model.put("message", "项目不存在!");
				return;
			}
		} else {
			project = new Project();
		}
		project.setName(name);
		project.setWarName(warName);
		if(project.getDeployScriptType() == null) {
			project.setDeployScriptType(1);
		}
		projectService.saveOrUpdateProject(project);
		model.put("success", true);
	}
	
	@Action("/admin/project/delete/*")
	public String delete() {
		Long id = NumberUtils.toLong(getPathVariable(1));
		projectService.deleteProjectById(id);
		model.put("success", "true");
		model.put("message", "删除成功!");
		return "json";
	}
	
	@Action("/admin/project/switch/*")
	public String switchDeployScriptType() {
		Long id = NumberUtils.toLong(getPathVariable(1));
		Integer deployScriptType = getIntParam("deployScriptType");
		Project project = projectService.getProjectById(id);
		if(!deployScriptType.equals(project.getDeployScriptType())) {
			project.setDeployScriptType(deployScriptType);
			projectService.saveOrUpdateProject(project);
		}
		model.put("success", true);
		model.put("deployScriptType", deployScriptType);
		return "json";
	}

}
