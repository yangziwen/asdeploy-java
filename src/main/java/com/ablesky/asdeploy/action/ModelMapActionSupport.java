package com.ablesky.asdeploy.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.ServletResponseAware;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;

@SuppressWarnings("serial")
public class ModelMapActionSupport extends ActionSupport 
		implements ModelDriven<Map<String, Object>>, ServletRequestAware, ServletResponseAware {
	
	protected HttpServletRequest request;
	protected HttpServletResponse response;
	
	@Override
	public void setServletResponse(HttpServletResponse response) {
		this.response = response;
	}

	@Override
	public void setServletRequest(HttpServletRequest request) {
		this.request = request;
	}
	
	protected Map<String, Object> modelMap = new HashMap<String, Object>();

	@Override
	public Map<String, Object> getModel() {
		return modelMap;
	}
	
	protected <T> T getFromModel(String key) {
		List<T> list = getListFromModel(key);
		if(list.size() == 0) {
			return null;
		}
		return list.get(0);
	}
	
	@SuppressWarnings("unchecked")
	protected <T> List<T> getListFromModel(String key) {
		Object obj = modelMap.get(key);
		if(obj == null) {
			return Collections.emptyList();
		}
		List<T> list = new ArrayList<T>();
		try {
			Class<?> clazz = obj.getClass();
			if(clazz.isArray()) {
				for(T t: (T[])obj) {
					list.add(t);
				}
			} else {
				list.add((T)obj);
			}
		} catch (Exception e) {
			return Collections.emptyList();
		}
		return list;
	}
	
	protected void clearModel() {
		modelMap.clear();
	}

}
