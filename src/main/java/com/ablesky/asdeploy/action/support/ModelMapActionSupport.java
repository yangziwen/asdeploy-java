package com.ablesky.asdeploy.action.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.ServletResponseAware;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;

@SuppressWarnings("serial")
public abstract class ModelMapActionSupport extends ActionSupport 
		implements ModelDriven<Map<String, Object>>, ServletRequestAware, ServletResponseAware {
	
	public static final int DEFAULT_START = 0;
	public static final int DEFAULT_LIMIT = 30;
	
	private static final ConcurrentHashMap<String, String[]> actionPathMap = new ConcurrentHashMap<String, String[]>();
	
	private static final String REQUEST_PATH_SEPARATOR = "/";
	/* pathVariableList用于保存路径中的变量 */
	private List<String> pathVariableList = null;
	
	protected HttpServletRequest request;
	protected HttpServletResponse response;
	
	@Override
	public void setServletRequest(HttpServletRequest request) {
		this.request = request;
	}
	
	@Override
	public void setServletResponse(HttpServletResponse response) {
		this.response = response;
	}
	
	protected final Map<String, Object> model = new HashMap<String, Object>();

	@Override
	public final Map<String, Object> getModel() {
		return model;
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
		Object obj = model.get(key);
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
		model.clear();
	}
	
	protected String getPathVariable(int index) {
		List<String> list = getPathVariableList();
		if(index <= 0 || index > list.size()) {	// 从1开始计数，跟struts保持一致
			return null;
		}
		return list.get(index - 1);
	}
	
	protected List<String> getPathVariableList() {
		if(pathVariableList == null) {
			initPathVariableList();
		}
		return pathVariableList;
	}

	/**
	 * 一个action只对应一次请求
	 * 此处是懒加载，只有在调用getPathVariable方法的时候，才会调用
	 */
	private void initPathVariableList() {
		
		String requestPath = request.getRequestURI();
		if(StringUtils.isBlank(requestPath) || requestPath.trim().equals(REQUEST_PATH_SEPARATOR)) {
			pathVariableList = Collections.emptyList();
			return;
		}
		
		Class<? extends ModelMapActionSupport> currentClass = this.getClass();
		String currentClassName = currentClass.getName();
		StackTraceElement trace = null;
		StackTraceElement[] traceArray = Thread.currentThread().getStackTrace();
		// 找寻声明@Action的那个方法
		Action currentAction = null;
		for(StackTraceElement t: traceArray) {
			if(!currentClassName.equals(t.getClassName())) {
				continue;
			}
			try {
				Method invokedMethod = currentClass.getDeclaredMethod(t.getMethodName());
				if((currentAction = invokedMethod.getAnnotation(Action.class)) == null) {
					continue;
				} else {
					trace = t;
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		assert trace != null;	// 不可能发生trace为空的情形
		assert currentAction != null;
		
		String key = trace.getClassName() + "." + trace.getMethodName();
		
		String[] actionPathArray = actionPathMap.get(key);
		if(actionPathArray == null) {
			actionPathArray = initActionPath(key, currentAction);
		}
		
		if(ArrayUtils.isEmpty(actionPathArray)) {
			pathVariableList = Collections.emptyList();
			return;
		}
		
		if(requestPath.startsWith(REQUEST_PATH_SEPARATOR)) {
			requestPath = requestPath.substring(1);
		}
		
		int i = 0, l = actionPathArray.length;
		List<String> list = new ArrayList<String>();
		
		for(String pathValue: requestPath.split(REQUEST_PATH_SEPARATOR)) {
			if( i >= l ) {
				break;
			}
			if( "*".equals(actionPathArray[i]) ) {
				list.add(pathValue);
			}
			i++;
		}
		
		pathVariableList = ListUtils.unmodifiableList(list);
	}
	
	/**
	 * 对于一个@Action，此方法只会在全局中调用一次
	 */
	private String[] initActionPath(String key, Action action) {
		String[] actionPathArray = null;
		try {
			String actionPath = action != null? action.value(): "";
			if(actionPath.startsWith(REQUEST_PATH_SEPARATOR)) {
				actionPath = actionPath.substring(1);
			}
			actionPathArray = StringUtils.isNotBlank(actionPath)
					? actionPath.split(REQUEST_PATH_SEPARATOR)
					: ArrayUtils.EMPTY_STRING_ARRAY;
			actionPathMap.putIfAbsent(key, actionPathArray);
			return actionPathArray;
		} catch(Exception e) {
			e.printStackTrace();
			actionPathMap.putIfAbsent(key, (actionPathArray = ArrayUtils.EMPTY_STRING_ARRAY));
			return actionPathArray;
		}
	}
	
}
