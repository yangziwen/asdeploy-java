package com.ablesky.asdeploy.action;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;

import com.ablesky.asdeploy.action.support.HttpMethod;
import com.ablesky.asdeploy.action.support.ModelMapActionSupport;
import com.ablesky.asdeploy.pojo.ConflictInfo;
import com.ablesky.asdeploy.pojo.PatchFile;
import com.ablesky.asdeploy.pojo.PatchFileRelGroup;
import com.ablesky.asdeploy.pojo.PatchGroup;
import com.ablesky.asdeploy.pojo.Project;
import com.ablesky.asdeploy.pojo.User;
import com.ablesky.asdeploy.service.IPatchGroupService;
import com.ablesky.asdeploy.service.IProjectService;
import com.ablesky.asdeploy.service.IUserService;
import com.ablesky.asdeploy.util.AuthUtil;
import com.ablesky.asdeploy.util.CommonConstant;

@ParentPackage("base")
@Namespace("/patchGroup")
@SuppressWarnings("serial")
public class PatchGroupAction extends ModelMapActionSupport {
	
	@Autowired
	private IPatchGroupService patchGroupService;
	@Autowired
	private IProjectService projectService;
	@Autowired
	private IUserService userService;
	
	@Action(value="list", results = {
		@Result(name="list", location="list.jsp")
	})
	public String list() {
		
		int start = getIntParam("start", DEFAULT_START);
		int limit= getIntParam("limit", DEFAULT_LIMIT);
		String creatorName = getStringParam("creatorName");
		String patchGroupName = getStringParam("patchGroupName");
		Long projectId = getLongParam("projectId");
		String status = getStringParam("status");
		
		Map<String, Object> param = new HashMap<String, Object>();
		if(StringUtils.isNotBlank(creatorName)) {
			param.put("creator_username__contain", creatorName);
		}
		if(StringUtils.isNotBlank(patchGroupName)) {
			param.put("name__contain", patchGroupName);
		}
		if(projectId != null && projectId > 0) {
			param.put("project_id", projectId);
		}
		if(StringUtils.isNotBlank(status)) {
			param.put("status", status);
		}
		param.put(CommonConstant.ORDER_BY, "id desc");
		
		model.put("projectList", projectService.getProjectListResult(0, 0, Collections.<String, Object>emptyMap()));
		model.put("page", patchGroupService.getPatchGroupPaginateResult(start, limit, param));
		model.put("isSuperAdmin", AuthUtil.isSuperAdmin());
		model.put("currentUser", AuthUtil.getCurrentUser());
		return "list";
	}

	@Action(value="/patchGroup/detail/*", results = {
		@Result(name="detail", location="/WEB-INF/views/patchGroup/detail.jsp")
	})
	public String detail() {
		Long id = NumberUtils.toLong(getPathVariable(1));
		PatchGroup patchGroup = patchGroupService.getPatchGroupById(id);
		List<PatchFileRelGroup> relList = patchGroupService.getPatchFileRelGroupListResult(0, 0, new ModelMap()
			.addAttribute("patchGroupId", id)
		);
		List<PatchFile> patchFileList = new ArrayList<PatchFile>(CollectionUtils.collect(relList, new Transformer<PatchFileRelGroup, PatchFile>() {
			@Override
			public PatchFile transform(PatchFileRelGroup rel) {
				return rel.getPatchFile();
			}
		}));
		Collections.sort(patchFileList);
		
		List<ConflictInfo> conflictInfoList = patchGroupService.getConflictInfoListResultByParam(new ModelMap().addAttribute("patchGroupId", id));
		Collections.sort(conflictInfoList, new Comparator<ConflictInfo>() {
			@Override
			public int compare(ConflictInfo info1, ConflictInfo info2) {
				Long patchGroupId1 = info1.getRelatedPatchGroupId();
				Long patchGroupId2 = info2.getRelatedPatchGroupId();
				if(patchGroupId1.equals(patchGroupId2)) {
					String filePath1 = info1.getPatchFile().getFilePath();
					String filePath2 = info2.getPatchFile().getFilePath();
					return filePath1.compareTo(filePath2);
				}
				return patchGroupId1.compareTo(patchGroupId2);
			}
		});
		model.put("patchGroup", patchGroup);
		model.put("patchFileList", patchFileList);
		model.put("conflictInfoList", conflictInfoList);
		model.put("isSuperAdmin", AuthUtil.isSuperAdmin());
		model.put("currentUser", AuthUtil.getCurrentUser());
		return "detail";
	}
	
	@Action(value="/patchGroup/edit/*", results = {
		@Result(name="edit", location="/WEB-INF/views/patchGroup/edit.jsp"),
		@Result(name="json", type="json", params={"root", "model"})
	})
	public String edit() {
		if(HttpMethod.POST.equals(request.getMethod())) {
			doEdit();
			return "json";
		} else {
			toEdit(NumberUtils.toLong(getPathVariable(1)));
			return "edit";
		}
	}
	
	private void toEdit(Long id) {
		if(id != null && id > 0) {
			model.put("patchGroup", patchGroupService.getPatchGroupById(id));
		}
		model.put("projectList", projectService.getProjectListResult(0, 0, Collections.<String, Object>emptyMap()));
		
	}
	
	private void doEdit() {
		Long id = NumberUtils.toLong(getPathVariable(1));
		Long projectId = getLongParam("projectId");
		String name = getStringParam("name");
		String checkCode = getStringParam("checkCode");
		String status = getStringParam("status");
		PatchGroup patchGroup = null;
		Project project = null;
		User currentUser = AuthUtil.getCurrentUser();
		if(currentUser == null) {
			model.put("success", false);
			model.put("message", "用户未登录或不存在!");
			return;
		}
		if(projectId == null || projectId == 0 || (project = projectService.getProjectById(projectId)) == null) {
			model.put("success", false);
			model.put("message", "项目不存在!");
			return;
		}
		if(id != null && id > 0) {
			patchGroup = patchGroupService.getPatchGroupById(id);
			if(patchGroup == null) {
				model.put("success", false);
				model.put("message", "补丁组不存在!");
				return;
			}
		} else {
			patchGroup = new PatchGroup();
			patchGroup.setCreator(currentUser);
			patchGroup.setProject(project);
			patchGroup.setCreateTime(new Timestamp(System.currentTimeMillis()));
		}
		if(!AuthUtil.isSuperAdmin() && !currentUser.getId().equals(patchGroup.getCreator().getId())) {
			model.put("success", false);
			model.put("message", "没有权限执行此操作!");
			return;
		}
		patchGroup.setName(name);
		patchGroup.setCheckCode(checkCode);
		patchGroup.setStatus(status);
		if(PatchGroup.STATUS_FINISHED.equals(status)) {
			patchGroup.setFinishTime(new Timestamp(System.currentTimeMillis()));
		}
		patchGroupService.saveOrUpdatePatchGroup(patchGroup);
		model.put("success", true);
		model.put("message", "补丁组保存成功!");
	}
	
	@Action(value="edit", results = {
		@Result(name="edit", location="edit.jsp")
	})
	public String editNew() {
		toEdit(0L);
		return "edit";
	}
	
	@Action(value="listData", results = {
		@Result(name="json", type="json", params={"root", "model"})
	})
	public String listData() {
		Long projectId = getLongParam("projectId");
		String status = getStringParam("status");
		int start = getIntParam("start", DEFAULT_START);
		int limit= getIntParam("limit", DEFAULT_LIMIT);
		
		ModelMap param = new ModelMap();
		if(projectId > 0){
			param.put("project_id", projectId);
		}
		if(StringUtils.isNotBlank(status)) {
			param.put("status", status);
		}
		param.put(CommonConstant.ORDER_BY, " id desc ");
		model.put("list", patchGroupService.getPatchGroupListResult(start, limit, param));
		model.put("success", true);
		return "json";
	}
}
