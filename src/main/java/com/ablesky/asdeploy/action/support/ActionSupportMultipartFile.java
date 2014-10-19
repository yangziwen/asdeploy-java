package com.ablesky.asdeploy.action.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 对spring-mvc做的适配
 */
public class ActionSupportMultipartFile implements MultipartFile {
	
	private String inputName;
	private ModelMapActionSupport action;
	
	public ActionSupportMultipartFile(String inputName, ModelMapActionSupport action) {
		this.inputName = inputName;
		this.action = action;
	}

	@Override
	public String getName() {
		return inputName;
	}

	@Override
	public String getOriginalFilename() {
		return action.getStringParam(inputName + "FileName");
	}

	@Override
	public String getContentType() {
		return action.getStringParam(inputName + "ContentType");
	}

	@Override
	public boolean isEmpty() {
		return action.getFileParam(inputName) == null;
	}

	@Override
	public long getSize() {
		return FileUtils.sizeOf(action.getFileParam(inputName));
	}

	@Override
	public byte[] getBytes() throws IOException {
		return FileUtils.readFileToByteArray(action.getFileParam(inputName));
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new FileInputStream(action.getFileParam(inputName));
	}

	@Override
	public void transferTo(File dest) throws IOException, IllegalStateException {
		FileUtils.copyFile(action.getFileParam(inputName), dest);
	}

}
