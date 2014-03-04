package com.ablesky.asdeploy.controller;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.ablesky.asdeploy.pojo.DeployItem;
import com.ablesky.asdeploy.pojo.DeployLock;
import com.ablesky.asdeploy.pojo.DeployRecord;
import com.ablesky.asdeploy.pojo.PatchGroup;
import com.ablesky.asdeploy.pojo.Project;
import com.ablesky.asdeploy.service.IDeployService;
import com.ablesky.asdeploy.service.IPatchGroupService;
import com.ablesky.asdeploy.service.IProjectService;
import com.ablesky.asdeploy.util.AuthUtil;
import com.ablesky.asdeploy.util.ZipUtil;

@Controller
@RequestMapping("/deploy")
public class DeployController {
	
	@Autowired
	private IProjectService projectService;
	@Autowired
	private IDeployService deployService;
	@Autowired
	private IPatchGroupService patchGroupService;
	
	@RequestMapping("/initOption/{msg}")
	public String initOption(@PathVariable("msg") String msg, Model model) {
		if("paramsError".equals(msg)) {
			model.addAttribute("errorMessage", "输入参数有误!");
		}
		return initOption(model);
	}
	
	@RequestMapping("/initOption")
	public String initOption(Model model) {
		List<Project> projectList = projectService.getProjectListResult(0, 0, Collections.<String, Object>emptyMap());
		model.addAttribute("projectList", projectList);
		return "deploy/initOption";
	}
	
	@RequestMapping(value = "/toDeployPage", method = RequestMethod.POST)
	public String toDeployPage(
			String deployType,
			String version,
			Long projectId,
			@RequestParam(required=false)
			Long patchGroupId,
			Model model) {
		Project project = null;
		PatchGroup patchGroup = null;
		if(StringUtils.isBlank(deployType) || StringUtils.isBlank(version) 
				|| projectId == null || projectId == 0
				|| (project = projectService.getProjectById(projectId)) == null ) {
			return "redirect:/deploy/initOption/paramsError";
		}
		DeployLock lock = deployService.checkCurrentLock();
		if(lock != null) {
			return "redirect:/main";
		}
		if(patchGroupId != null && patchGroupId > 0) {
			patchGroup = patchGroupService.getPatchGroupById(patchGroupId);
		}
		
		DeployRecord deployRecord = buildNewDeployRecord(project);
		deployService.saveOrUpdateDeployRecord(deployRecord);
		DeployLock newLock = buildNewDeployLock(deployRecord);
		deployService.saveOrUpdateDeployLock(newLock);
		
		model.addAttribute("project", project)
			.addAttribute("deployType", deployType)
			.addAttribute("version", version)
			.addAttribute("patchGroup", patchGroup)
			.addAttribute("deployRecord", deployRecord);
		return "deploy/deployPage";
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
	
	@RequestMapping("/unlockDeploy")
	public @ResponseBody Map<String, Object> unlockDeploy() {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		if(!AuthUtil.isSuperAdmin()) {
			resultMap.put("success", false);
			resultMap.put("message", "没有权限!");
			return resultMap;
		}
		deployService.unlockDeploy();
		resultMap.put("success", true);
		return resultMap;
	}
	
	@RequestMapping("/unlockDeployRedirect")
	public String unlockDeployRedirect() {
		deployService.unlockDeploy();
		return "redirect:/main";
	}
	
	@RequestMapping("/uploadItem")
	public @ResponseBody Map<String, Object> uploadItem(
			Long projectId,
			Long deployRecordId,
			@RequestParam(defaultValue = "0")
			Long patchGroupId,
			String deployType,
			String version,
			@RequestParam("deployItemField")
			MultipartFile deployItemFile
			) throws IllegalStateException, IOException {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		String filename = deployItemFile.getOriginalFilename();
		Project project = projectService.getProjectById(projectId);
		DeployRecord deployRecord = deployService.getDeployRecordById(deployRecordId);
		PatchGroup patchGroup = null;
		if(patchGroupId != null && patchGroupId > 0) {
			patchGroup = patchGroupService.getPatchGroupById(patchGroupId);
			if(patchGroup == null) {
				resultMap.put("success", false);
				resultMap.put("message", "补丁组不存在!");
				return resultMap;
			}
			if(!filename.contains(patchGroup.getCheckCode())) {
				resultMap.put("success", false);
				resultMap.put("message", "补丁名称与补丁组的标识号不匹配!");
				return resultMap;
			}
		}
		deployService.persistDeployItem(deployItemFile, project, patchGroup, deployRecord, deployType, version);
		resultMap.put("filename", filename);
		resultMap.put("size", deployItemFile.getSize());
		resultMap.put("success", true);
		return resultMap;
	}
	
	@RequestMapping(value="/decompressItem", method=RequestMethod.POST)
	public @ResponseBody Map<String, Object> decompressItem(
			Long deployRecordId,
			@RequestParam(defaultValue="0")
			Long patchGroupId
			) throws IOException {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		DeployRecord deployRecord = deployService.getDeployRecordById(deployRecordId);
		DeployItem deployItem = deployRecord.getDeployItem();
		if(deployItem == null) {
			resultMap.put("success", false);
			resultMap.put("message", "压缩文件不存在!");
			return resultMap;
		}
		unzipDeployItem(deployItem);
		String targetFolderPath = FilenameUtils.concat(deployItem.getFolderPath(), FilenameUtils.getBaseName(deployItem.getFileName()));
		List<String> filePathList = getDeployItemFilePathList(targetFolderPath);
		resultMap.put("filePathList", filePathList);
		resultMap.put("conflictFileInfoList", Collections.emptyList());
		PatchGroup patchGroup = null;
		if(patchGroupId != null && patchGroupId > 0 && (patchGroup = patchGroupService.getPatchGroupById(patchGroupId)) != null) {
			// TODO
		}
		resultMap.put("success", true);
		return resultMap;
	}
	
	/**
	 * 解压缩文件
	 */
	private void unzipDeployItem(DeployItem deployItem) throws IOException {
		String sourceFilePath = FilenameUtils.concat(deployItem.getFolderPath(), deployItem.getFileName());
		String targetFolderPath = FilenameUtils.concat(deployItem.getFolderPath(), FilenameUtils.getBaseName(deployItem.getFileName()));
		String parentFolderPath = deployItem.getFolderPath();
		File targetFolder = new File(targetFolderPath);
		if(targetFolder.exists()) {
			FileUtils.deleteDirectory(targetFolder);
		}
		ZipUtil.unzip(sourceFilePath, parentFolderPath);
	}
	
	/**
	 * 获取解压后的文件列表(按发布格式)
	 */
	private List<String> getDeployItemFilePathList(String targetFolderPath) {
		targetFolderPath = FilenameUtils.normalize(targetFolderPath);
		File parentFolder = null;
		if(StringUtils.isBlank(targetFolderPath) || !(parentFolder = new File(targetFolderPath)).exists()) {
			return Collections.emptyList();
		}
		Collection<File> fileList = FileUtils.listFiles(parentFolder, null, true);
		List<String> filePathList = new ArrayList<String>(fileList.size());
		targetFolderPath = targetFolderPath.substring(FilenameUtils.getPrefixLength(targetFolderPath));
		for(File file: fileList) {
			String filePath = FilenameUtils.normalize(file.getAbsolutePath());
			filePath = filePath.substring(FilenameUtils.getPrefixLength(filePath));
			filePath = filePath.replace(targetFolderPath + File.separator, "");
			filePathList.add(filePath.replaceAll("\\" + File.separator, "."));
		}
		return filePathList;
	}
}
