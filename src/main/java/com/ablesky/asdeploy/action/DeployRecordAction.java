package com.ablesky.asdeploy.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;

import com.ablesky.asdeploy.action.support.ModelMapActionSupport;
import com.ablesky.asdeploy.pojo.ConflictDetail;
import com.ablesky.asdeploy.pojo.DeployItem;
import com.ablesky.asdeploy.pojo.DeployRecord;
import com.ablesky.asdeploy.service.IDeployService;
import com.ablesky.asdeploy.service.IProjectService;
import com.ablesky.asdeploy.util.CommonConstant;
import com.ablesky.asdeploy.util.DeployUtil;

@ParentPackage("base")
@Namespace("/deployRecord")
@SuppressWarnings("serial")
public class DeployRecordAction extends ModelMapActionSupport {
	
	@Autowired
	private IProjectService projectService;
	@Autowired
	private IDeployService deployService;
	
	@Action(value="list", results = {
		@Result(name="list", location="list.jsp")
	})
	public String list() {
		int start = getIntParam("start", DEFAULT_START);
		int limit= getIntParam("limit", DEFAULT_LIMIT);
		String username = getStringParam("username");
		Long projectId = getLongParam("projectId");
		String deployType = getStringParam("deployType");
		String version = getStringParam("version");
		Map<String, Object> param = new HashMap<String, Object>();
		if(StringUtils.isNotBlank(username)) {
			param.put("user_username__contain", username);
		}
		if(projectId != null && projectId > 0) {
			param.put("project_id", projectId);
		}
		if(StringUtils.isNotBlank(deployType)) {
			param.put("deployItem_deployType", deployType);
		}
		if(StringUtils.isNotBlank(version)) {
			param.put("deployItem_version", version);
		}
		param.put(CommonConstant.ORDER_BY, "id desc");
		model.put("projectList", projectService.getProjectListResult(0, 0, Collections.<String, Object>emptyMap()));
		model.put("page", deployService.getDeployRecordPaginateResult(start, limit, param));
		return "list";
	}
	
	@Action(value="/deployRecord/detail/*", results = {
		@Result(name="detail", location="/WEB-INF/views/deployRecord/detail.jsp")
	})
	public String detail() {
		Long id = getLongParam("{1}");
		DeployRecord deployRecord = deployService.getDeployRecordById(id);
		DeployItem deployItem = deployRecord.getDeployItem();
		List<String> filePathList = Collections.emptyList();
		String readme = "";
		if(deployItem != null) {
			String targetFolderPath = FilenameUtils.concat(deployItem.getFolderPath(), FilenameUtils.getBaseName(deployItem.getFileName()));
			filePathList = DeployUtil.getDeployItemFilePathList(targetFolderPath);
			readme = DeployUtil.loadReadmeContent(targetFolderPath);
		}
		List<ConflictDetail> conflictDetailList = deployRecord.getIsConflictWithOthers()
				? deployService.getConflictDetailListResultByParam(new ModelMap().addAttribute("deployRecordId", id))
				: Collections.<ConflictDetail>emptyList();
		
		Collections.sort(conflictDetailList, new Comparator<ConflictDetail>() {
			@Override
			public int compare(ConflictDetail detail1, ConflictDetail detail2) {
				Long patchGroupId1 = detail1.getConflictInfo().getRelatedPatchGroupId();
				Long patchGroupId2 = detail2.getConflictInfo().getRelatedPatchGroupId();
				if(patchGroupId1.equals(patchGroupId2)) {
					String filePath1 = detail1.getConflictInfo().getPatchFile().getFilePath();
					String filePath2 = detail2.getConflictInfo().getPatchFile().getFilePath();
					return filePath1.compareTo(filePath2);
				}
				return patchGroupId1.compareTo(patchGroupId2);
			}
		});
		List<String> originFilePathList = new ArrayList<String>();
		List<String> optimizedFilePathList = new ArrayList<String>();
		for(String filePath: filePathList) {
			if(filePath.contains("_optimize")) {
				optimizedFilePathList.add(filePath);
			} else {
				originFilePathList.add(filePath);
			}
		}
		Collections.sort(originFilePathList);
		Collections.sort(optimizedFilePathList);
		
		model.put("deployRecord", deployRecord);
		model.put("originFilePathList", originFilePathList);
		model.put("optimizedFilePathList", optimizedFilePathList);
		model.put("readme", readme);
		model.put("conflictDetailList", conflictDetailList);
		
		return "detail";
	}

}
