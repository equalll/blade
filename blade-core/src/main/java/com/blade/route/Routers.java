/**
 * Copyright (c) 2015, biezhi 王爵 (biezhi.me@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blade.route;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import blade.kit.ReflectKit;
import blade.kit.log.Logger;

import com.blade.http.HttpMethod;
import com.blade.http.HttpStatus;
import com.blade.http.Request;
import com.blade.http.Response;
import com.blade.ioc.Container;
import com.blade.ioc.Scope;

/**
 * 
 * <p>
 * 注册、管理路由
 * </p>
 *
 * @author	<a href="mailto:biezhi.me@gmail.com" target="_blank">biezhi</a>
 * @since	1.0
 */
public class Routers {
	
	private Logger LOGGER = Logger.getLogger(Routers.class);
	
	private Container container = null;
	
	private List<Route> routes = new CopyOnWriteArrayList<Route>();
	
	private List<Route> interceptors = new CopyOnWriteArrayList<Route>();
	
	private static final String METHOD_NAME = "handle";
	
	public Routers(Container container) {
		this.container = container;
	}
	
	public void handle(Request request, Response response, Route route) throws Exception {
		request.setRoute(route);
		response.status(HttpStatus.NOT_FOUND);
		
		Object controller = route.getTarget();
		Method method = route.getAction();
		try {
			method.invoke(controller, request, response);
		} catch (InvocationTargetException e) {
			throw (Exception) e.getCause();
		}
	}
	
	public List<Route> getRoutes() {
		return routes;
	}

	public void setRoutes(List<Route> routes) {
		this.routes = routes;
	}
	
	public void addRoute(Route route) {
		this.routes.add(route);
	}
	
	public void addRoutes(List<Route> routes) {
		for(Route route : routes){
			if(this.routes.contains(route)){
				LOGGER.warn("\tRoute "+ route +" has exist");
				continue;
			}
			this.routes.add(route);
			LOGGER.debug("Add Route：" + route);
		}
	}
	
	public List<Route> getInterceptors() {
		return interceptors;
	}
	
	public void addInterceptors(List<Route> interceptors) {
		this.interceptors.addAll(interceptors);
	}

	public void addRoute(HttpMethod httpMethod, String path, Object controller, String methodName) throws NoSuchMethodException {
		Method method = controller.getClass().getMethod(methodName, Request.class, Response.class);
		addRoute(httpMethod, path, controller, method);
	}
	
	public void addRoute(HttpMethod httpMethod, String path, Object controller, Method method) {
//		method.setAccessible(true);
		Route route = new Route(httpMethod, path, controller, method);
		if(this.routes.contains(route)){
			LOGGER.warn("\tRoute "+ route +" has exist");
		}
		
		if(httpMethod == HttpMethod.BEFORE || httpMethod == HttpMethod.AFTER){
			interceptors.add(route);
			LOGGER.debug("Add Interceptor：" + route);
		} else {
			routes.add(route);
			LOGGER.debug("Add Route：" + route);
		}
		
	}
	
	public void route(String path, RouteHandler handler, HttpMethod httpMethod) {
		try {
			addRoute(httpMethod, path, handler, METHOD_NAME);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void route(String[] paths, RouteHandler handler, HttpMethod httpMethod) {
		for(String path : paths){
			route(path, handler, httpMethod);
		}
	}
	
	public void route(String path, Object target, String methodName) {
		try {
			Method method = target.getClass().getMethod(methodName, Request.class, Response.class);
			addRoute(HttpMethod.ALL, path, target, method);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public void route(String path, Class<?> clazz, String methodName) {
		try {
			HttpMethod httpMethod = HttpMethod.ALL;
			if(methodName.indexOf(":") != -1){
    			String[] methodArr = methodName.split(":");
    			httpMethod = HttpMethod.valueOf(methodArr[0].toUpperCase());
    			methodName = methodArr[1];
    		}
			Object controller = container.getBean(clazz, Scope.SINGLE);
			if(null == controller){
				controller = ReflectKit.newInstance(clazz);
				container.registBean(controller);
			}
			
			Method method = clazz.getMethod(methodName, Request.class, Response.class);
			
			addRoute(httpMethod, path, controller, method);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}
	
	public void route(String path, Class<?> clazz, String methodName, HttpMethod httpMethod) {
		try {
			Object controller = container.getBean(clazz, Scope.SINGLE);
			if(null == controller){
				controller = ReflectKit.newInstance(clazz);
				container.registBean(controller);
			}
			Method method = clazz.getMethod(methodName, Request.class, Response.class);
			addRoute(httpMethod, path, controller, method);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}
	
	public void route(String path, Class<?> clazz, Method method, HttpMethod httpMethod) {
		try {
			Object controller = container.getBean(clazz, Scope.SINGLE);
			if(null == controller){
				controller = ReflectKit.newInstance(clazz);
				container.registBean(controller);
			}
			addRoute(httpMethod, path, controller, method);
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}
	
	public void route(String path, Object target, String methodName, HttpMethod httpMethod) {
		try {
			Class<?> clazz = target.getClass();
			container.registBean(target);
			Method method = clazz.getMethod(methodName, Request.class, Response.class);
			addRoute(httpMethod, path, target, method);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}
}
