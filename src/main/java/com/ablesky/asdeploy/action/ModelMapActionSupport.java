package com.ablesky.asdeploy.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
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
	
	protected String getStringParam(String key) {
		List<String> list = getStringParamList(key);
		return list.size() > 0? list.get(0): null;
	}
	
	protected String getStringParam(String key, String defaultValue) {
		String value = getStringParam(key);
		return value != null? value: defaultValue;
	}
	
	protected int getIntParam(String key) {
		return NumberUtils.toInt(getStringParam(key));
	}
	
	protected int getIntParam(String key, int defaultValue) {
		return NumberUtils.toInt(getStringParam(key), defaultValue);
	}
	
	protected long getLongParam(String key) {
		return NumberUtils.toLong(getStringParam(key));
	}
	
	protected long getLongParam(String key, long defaultValue) {
		return NumberUtils.toLong(getStringParam(key), defaultValue);
	}
	
	protected float getFloatParam(String key){
		return NumberUtils.toFloat(getStringParam(key));
	}
	
	protected float getFloatParam(String key, float defaultValue) {
		return NumberUtils.toFloat(getStringParam(key), defaultValue);
	}
	
	protected double getDoubleParam(String key) {
		return NumberUtils.toDouble(getStringParam(key));
	}
	
	protected double getDoubleParam(String key, float defaultValue) {
		return NumberUtils.toDouble(getStringParam(key), defaultValue);
	}
	
	protected boolean getBooleanParam(String key) {
		return BooleanUtils.toBoolean(getStringParam(key));
	}
	
	protected List<String> getStringParamList(String key) {
		Object obj = modelMap.get(key);
		if(obj == null) {
			return Collections.emptyList();
		}
		Class<?> clazz = obj.getClass();
		if(clazz.isArray()) {
			return Arrays.<String>asList((String[]) obj);
		} else {
			return Arrays.<String>asList(new String[]{(String) obj}); 
		}
	}
	
	protected List<Integer> getIntParamList(String key) {
		List<String> strList = getStringParamList(key);
		if(strList.size() == 0) {
			return Collections.emptyList();
		}
		List<Integer> list = new ArrayList<Integer>();
		for(String str: strList) {
			list.add(NumberUtils.toInt(str));
		}
		return list;
	}
	
	protected List<Long> getLongParamList(String key) {
		List<String> strList = getStringParamList(key);
		if(strList.size() == 0) {
			return Collections.emptyList();
		}
		List<Long> list = new ArrayList<Long>();
		for(String str: strList) {
			list.add(NumberUtils.toLong(str));
		}
		return list;
	}
	
	protected List<Float> getFloatParamList(String key) {
		List<String> strList = getStringParamList(key);
		if(strList.size() == 0) {
			return Collections.emptyList();
		}
		List<Float> list = new ArrayList<Float>();
		for(String str: strList) {
			list.add(NumberUtils.toFloat(str));
		}
		return list;
	}
	
	protected List<Double> getDoubleParamList(String key){
		List<String> strList = getStringParamList(key);
		if(strList.size() == 0) {
			return Collections.emptyList();
		}
		List<Double> list = new ArrayList<Double>();
		for(String str: strList) {
			list.add(NumberUtils.toDouble(str));
		}
		return list;
	}
	
	protected List<Boolean> getBooleanParamList(String key) {
		List<String> strList = getStringParamList(key);
		if(strList.size() == 0) {
			return Collections.emptyList();
		}
		List<Boolean> list = new ArrayList<Boolean>();
		for(String str: strList) {
			list.add(BooleanUtils.toBoolean(str));
		}
		return list;
	}
	
	protected void clearModel() {
		modelMap.clear();
	}

}
