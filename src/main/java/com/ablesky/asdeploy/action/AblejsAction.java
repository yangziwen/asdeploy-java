package com.ablesky.asdeploy.action;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.aspectj.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;

import com.ablesky.asdeploy.service.IProjectService;
import com.ablesky.asdeploy.util.cmd.AblejsCmd;

@ParentPackage("base")
@Namespace("/ablejs")
@SuppressWarnings("serial")
public class AblejsAction extends ModelMapActionSupport {
	
	@Autowired
	private IProjectService projectService;
	
	@Action(value="index", results = {
		@Result(name="index", location="index.jsp")
	})
	public String index() {
		model.put("projectList", projectService.getProjectListResult());
		return "index";
	}
	
	@Action(value="query", results = {
		@Result(name="stream", type="stream", params={
			"contentType", "application/json",
			"contentLength", "${contentLength}"
		})
	})
	public String query() {
		String projectName = getStringParam("projectName", "as-web");
		String queryType = getStringParam("queryType");
		String queryValue = getStringParam("queryValue");
		String result = "";
		AblejsCmd ablejs = new AblejsCmd(projectName);
		InputStream in = null;
		try {
			if("relativePath".equals(queryType)) {
				in = ablejs.path().param(queryValue).exec().getInputStream();
				result = new String(FileUtil.readAsByteArray(in), "UTF8");
			} else if("hashcode".equals(queryType)) {
				in = ablejs.fingerprint().param(queryValue).exec().getInputStream();
				result = new String(FileUtil.readAsByteArray(in), "UTF8");
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = "{}";
		} finally {
			IOUtils.closeQuietly(in);
		}
		if("undefined".equals(result) || "null".equals(result)) {
			result = "{}";
		}
		byte[] byteArray = result.getBytes();
		model.put("inputStream", new ByteArrayInputStream(byteArray));
		model.put("contentLength", byteArray.length);
		return "stream";
	}

}
