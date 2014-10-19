package com.ablesky.asdeploy.action;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import com.ablesky.asdeploy.action.support.ActionSupportMultipartFile;
import com.ablesky.asdeploy.action.support.ModelMapActionSupport;
import com.ablesky.asdeploy.dto.ConflictInfoDto;
import com.ablesky.asdeploy.pojo.DeployItem;
import com.ablesky.asdeploy.pojo.DeployLock;
import com.ablesky.asdeploy.pojo.DeployRecord;
import com.ablesky.asdeploy.pojo.PatchFileRelGroup;
import com.ablesky.asdeploy.pojo.PatchGroup;
import com.ablesky.asdeploy.pojo.Project;
import com.ablesky.asdeploy.service.IDeployService;
import com.ablesky.asdeploy.service.IPatchGroupService;
import com.ablesky.asdeploy.service.IProjectService;
import com.ablesky.asdeploy.util.AuthUtil;
import com.ablesky.asdeploy.util.DeployUtil;
import com.ablesky.asdeploy.util.Deployer;

@ParentPackage("base")
@Namespace("/deploy")
@SuppressWarnings("serial")
@Results({
	@Result(name="json", type="json", params={"root", "model"}),
	@Result(name="redirect", type="redirect", location="${redirectLocation}")
})
public class DeployAction extends ModelMapActionSupport {
	
	@Autowired
	private IProjectService projectService;
	@Autowired
	private IDeployService deployService;
	@Autowired
	private IPatchGroupService patchGroupService;

	@Action(value="/deploy/initOption/*", results = {
		@Result(name="initOption", location="/WEB-INF/views/deploy/initOption.jsp")
	})
	public String initOptionWithErrorMsg() {
		if("paramsError".equals(getPathVariable(1))){
			model.put("errorMessage", "输入参数有误!");
		}
		return initOption();
	}
	
	@Action(value="initOption", results = {
		@Result(name="initOption", location="initOption.jsp")
	})
	public String initOption() {
		List<Project> projectList = projectService.getProjectListResult(0, 0, Collections.<String, Object>emptyMap());
		model.put("projectList", projectList);
		return "initOption";
	}
	
	@Action(value="toDeployPage", results = {
		@Result(name="deployPage", location="deployPage.jsp"),
	})
	public String toDeployPage() {
		String deployType = getStringParam("deployType");
		String version= getStringParam("version");
		Long projectId = getLongParam("projectId");
		Long patchGroupId = getLongParam("patchGroupId");
		
		Project project = null;
		PatchGroup patchGroup = null;
		if(StringUtils.isBlank(deployType) || StringUtils.isBlank(version) 
				|| projectId == null || projectId == 0
				|| (project = projectService.getProjectById(projectId)) == null ) {
			model.put("redirectLocation", "/deploy/initOption/paramsError");
			return "redirect";
		}
		DeployLock lock = deployService.checkCurrentLock();
		if(lock != null) {
			model.put("redirectLocation", "/main");
			return "redirect";
		}
		if(patchGroupId != null && patchGroupId > 0) {
			patchGroup = patchGroupService.getPatchGroupById(patchGroupId);
		}
		
		DeployRecord deployRecord = buildNewDeployRecord(project);
		deployService.saveOrUpdateDeployRecord(deployRecord);
		DeployLock newLock = buildNewDeployLock(deployRecord);
		deployService.saveOrUpdateDeployLock(newLock);
		
		model.put("project", project);
		model.put("deployType", deployType);
		model.put("version", version);
		model.put("patchGroup", patchGroup);
		model.put("deployRecord", deployRecord);
		
		return "deployPage";
	}
	private DeployRecord buildNewDeployRecord(Project project) {
		DeployRecord deployRecord = new DeployRecord();
		deployRecord.setUser(AuthUtil.getCurrentUser());
		deployRecord.setProject(project);
		deployRecord.setCreateTime(new Timestamp(System.currentTimeMillis()));
		deployRecord.setIsConflictWithOthers(false);
		deployRecord.setStatus(DeployRecord.STATUS_PREPARE);
		return deployRecord;
	}
	
	private DeployLock buildNewDeployLock(DeployRecord deployRecord) {
		DeployLock lock = new DeployLock();
		lock.setUser(AuthUtil.getCurrentUser());
		lock.setDeployRecord(deployRecord);
		lock.setLockedTime(new Timestamp(System.currentTimeMillis()));
		lock.setIsLocked(Boolean.TRUE);
		return lock;
	}
	
	@Action("unlockDeploy")
	public String unlockDeploy() {
		deployService.unlockDeploy();
		model.put("success", true);
		return "json";
	}
	
	@Action("unlockDeployRedirect")
	public String unlockDeployRedirect() {
		deployService.unlockDeploy();
		model.put("redirectLocation", "/main");
		return "redirect";
	}
	
	@Action("uploadStaticTar")
	public String uploadStaticTar() throws IllegalStateException, IOException {
		Long projectId = getLongParam("projectId");
		String version = getStringParam("version");
		MultipartFile staticTarFile = new ActionSupportMultipartFile("staticTarFile", this);
		String filename = staticTarFile.getOriginalFilename();
		Project project = projectService.getProjectById(projectId);
		if(project == null) {
			model.put("success", false);
			model.put("message", "项目不存在!");
			return "json";
		}
		staticTarFile.transferTo(new File(DeployUtil.getDeployItemUploadFolder(project.getName(), version) + filename));
		model.put("filename", filename);
		model.put("size", staticTarFile.getSize());
		model.put("success", true);
		return "json";
	}
	
	@Action("uploadItem")
	public String uploadItem() throws IllegalStateException, IOException {
		Long projectId = getLongParam("projectId");
		Long deployRecordId = getLongParam("deployRecordId");
		Long patchGroupId = getLongParam("patchGroupId");
		String deployType = getStringParam("deployType");
		String version = getStringParam("version");
		MultipartFile deployItemFile = new ActionSupportMultipartFile("deployItemField", this);
		
		String filename = deployItemFile.getOriginalFilename();
		Project project = projectService.getProjectById(projectId);
		DeployRecord deployRecord = deployService.getDeployRecordById(deployRecordId);
		PatchGroup patchGroup = null;
		if(patchGroupId != null && patchGroupId > 0) {
			patchGroup = patchGroupService.getPatchGroupById(patchGroupId);
			if(patchGroup == null) {
				model.put("success", false);
				model.put("message", "补丁组不存在!");
				return "json";
			}
			if(!filename.contains(patchGroup.getCheckCode())) {
				model.put("success", false);
				model.put("message", "补丁名称与补丁组的标识号不匹配!");
				return "json";
			}
		}
		deployService.persistDeployItem(deployItemFile, project, patchGroup, deployRecord, deployType, version);
		model.put("filename", filename);
		model.put("size", deployItemFile.getSize());
		model.put("success", true);
		return "json";
	}
	
	@Action("decompressItem")
	public String decompressItem() {
		Long deployRecordId = getLongParam("deployRecordId");
		Long patchGroupId = getLongParam("patchGroupId");
		DeployRecord deployRecord = deployService.getDeployRecordById(deployRecordId);
		DeployItem deployItem = deployRecord.getDeployItem();
		if(deployItem == null) {
			model.put("success", false);
			model.put("message", "压缩文件不存在!");
			return "json";
		}
		try {
			DeployUtil.unzipDeployItem(deployItem);
		} catch (IOException e) {
			e.printStackTrace();
			model.put("success", false);
			model.put("message", "文件解压缩失败!");
			return "json";
		}
		String targetFolderPath = FilenameUtils.concat(deployItem.getFolderPath(), FilenameUtils.getBaseName(deployItem.getFileName()));
		List<String> filePathList = DeployUtil.getDeployItemFilePathList(targetFolderPath);
		if(CollectionUtils.isEmpty(filePathList)) {
			model.put("success", false);
			model.put("message", "解压后的文件夹中无内容! 请确认压缩包文件名与被压缩的目录名是否一致!");
			return "json";
		}
		List<ConflictInfoDto> conflictInfoList = Collections.emptyList();
		if(patchGroupId != null && patchGroupId > 0) {
			final PatchGroup patchGroup = patchGroupService.getPatchGroupById(patchGroupId);
			if(patchGroup != null) {
				List<PatchFileRelGroup> conflictRelList = patchGroupService.getPatchFileRelGroupListWhichConflictWith(patchGroup, filePathList);
				conflictInfoList = new ArrayList<ConflictInfoDto>(CollectionUtils.collect(conflictRelList, new Transformer<PatchFileRelGroup, ConflictInfoDto>() {
					@Override
					public ConflictInfoDto transform(PatchFileRelGroup conflictRel) {
						return new ConflictInfoDto().fillDto(patchGroup, conflictRel);
					}
				}));
			}
		}
		model.put("filePathList", filePathList);
		model.put("conflictInfoList", conflictInfoList);
		model.put("readme", DeployUtil.loadReadmeContent(targetFolderPath));
		model.put("success", true);
		return "json";
	}
	
	@Action("startDeploy")
	public String startDeploy() {
		Long deployRecordId = getLongParam("deployRecordId");
		Long patchGroupId = getLongParam("patchGroupId");
		String deployManner = getStringParam("deployManner");
		String serverGroupParam = getStringParam("serverGroupParam", "a");
		DeployRecord deployRecord = null;
		PatchGroup patchGroup = null;
		File deployPatchScript = new File(DeployUtil.getDeployPatchScriptPath());
		if(!deployPatchScript.isFile()) {	// 粗略的判断下，主要是应对d盘未挂载的情形。
			model.put("success", false);
			model.put("message", "发布脚本不存在!");
			return "json";
		}
		if(deployRecordId == null || deployRecordId <= 0 || (deployRecord = deployService.getDeployRecordById(deployRecordId)) == null) {
			model.put("success", false);
			model.put("message", "参数有误!");
			return "json";
		}
		DeployLock lock = deployService.checkCurrentLock();
		if(lock == null || !lock.getDeployRecord().getId().equals(deployRecordId)) {
			model.put("success", false);
			model.put("message", "本次发布已被解锁!");
			return "json";
		}
		if(DeployRecord.STATUS_PREPARE.equals(deployRecord.getStatus())) {
			model.put("success", false);
			model.put("message", "尚未上传文件");
			return "json";
		}
		if(Boolean.TRUE.equals(Deployer.getLogIsWriting(deployRecordId))) { // 发布仍在继续
			model.put("success", false);
			model.put("message", "发布仍在继续中...");
			return "json";
		}
		if(patchGroupId != null && patchGroupId > 0) {
			patchGroup = patchGroupService.getPatchGroupById(patchGroupId);
		}
		// 开始发布
		doDeploy(deployRecord, patchGroup, deployManner, serverGroupParam);
		model.put("success", true);
		model.put("message", "发布启动成功!");
		return "json";
	}
	
	private void doDeploy(DeployRecord deployRecord, PatchGroup patchGroup, String deployManner, String serverGroupParam) {
		// 1. 记录补丁组及冲突信息
		DeployItem item = deployRecord.getDeployItem();
		String targetFolderPath = FilenameUtils.concat(item.getFolderPath(), FilenameUtils.getBaseName(item.getFileName()));
		List<String> filePathList = DeployUtil.getDeployItemFilePathList(targetFolderPath);
		deployService.persistInfoBeforeDeployStart(deployRecord, patchGroup, filePathList, deployManner);
		Deployer.executor.submit(new Deployer(deployRecord, deployManner, serverGroupParam));
	}
	
	@Action("readDeployLogOnRealtime")
	public String readDeployLogOnRealtime() {
		Long deployRecordId = getLongParam("deployRecordId");
		Boolean isWriting = Deployer.getLogIsWriting(deployRecordId);
		if(isWriting == null) {	// 发布已结束，并且前面已经读完了所有日志
			model.put("isFinished", true);
			model.put("deployResult", Deployer.getDeployResult(deployRecordId));
			Deployer.deleteDeployResult(deployRecordId);
			Deployer.deleteLogLastReadPos(deployRecordId);
			return "json";
		}
		if(Boolean.FALSE.equals(isWriting)) {	// 说明发布结束了，需要最后再读一次日志信息
			Deployer.deleteLogIsWriting(deployRecordId);
		}
		String deployLogContent = DeployUtil.readDeployLogContent(deployRecordId);// 此处读日志的时候，就在不断更新文件指针了
		model.put("isFinished", false);
		model.put("deployLogContent", deployLogContent);
		return "json";
	}
}
