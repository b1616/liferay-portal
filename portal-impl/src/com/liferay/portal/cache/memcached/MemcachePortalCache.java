/**
 * Copyright (c) 2000-2010 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.cache.memcached;

import com.liferay.portal.cache.AbstractPortalCache;
import com.liferay.portal.kernel.cache.PortalCache;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.io.Serializable;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.MemcachedClientIF;

/**
 * <a href="MemcachePortalCache.java.html"><b><i>View Source</i></b></a>
 *
 * @author Michael C. Han
 */
public class MemcachePortalCache extends AbstractPortalCache
	implements PortalCache {

	public MemcachePortalCache(
		MemcachedClientIF memcachedClient, int timeout,
		TimeUnit timeoutTimeUnit) {

		_memcachedClient = memcachedClient;
		_timeout = timeout;
		_timeoutTimeUnit = timeoutTimeUnit;
	}

	public void destroy() {
		_memcachedClient.shutdown();
	}

	public Object get(String key) {

		String processedKey = processKey(key);
		
		Object cachedObject = null;

		Future<Object> future = null;

		try {
			future = _memcachedClient.asyncGet(processedKey);
		}
		catch (IllegalArgumentException iae) {
			if (_log.isWarnEnabled()) {
				_log.warn("Error retrieving with key " + key, iae);
			}

			return null;
		}

		try {
			cachedObject = future.get(_timeout, _timeoutTimeUnit);
		}
		catch (Throwable t) {
			if (_log.isWarnEnabled()) {
				_log.warn("Memcache operation error", t);
			}

			future.cancel(true);
		}

		return cachedObject;
	}

	public void put(String key, Object obj) {
		put(key, obj, _timeToLive);
	}

	public void put(String key, Object obj, int timeToLive) {

		String processedKey = processKey(key);

		try {
			_memcachedClient.set(processedKey, timeToLive, obj);
		}
		catch (IllegalArgumentException iae) {
			if (_log.isWarnEnabled()) {
				_log.warn("Error storing value with key " + key, iae);
			}
		}
	}

	public void put(String key, Serializable obj) {
		put(key, obj, _timeToLive);
	}

	public void put(String key, Serializable obj, int timeToLive) {

		String processedKey = processKey(key);
		
		try {
			_memcachedClient.set(processedKey, timeToLive, obj);
		}
		catch (IllegalArgumentException iae) {
			if (_log.isWarnEnabled()) {
				_log.warn("Error storing value with key " + key, iae);
			}
		}
	}

	public void remove(String key) {

		String processedKey = processKey(key);

		try {
			_memcachedClient.delete(processedKey);
		}
		catch (IllegalArgumentException iae) {
			if (_log.isWarnEnabled()) {
				_log.warn("Error removing value with key " + key, iae);
			}
		}
	}

	public void removeAll() {
		_memcachedClient.flush();
	}

	public void setTimeToLive(int timeToLive) {
		_timeToLive = timeToLive;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		MemcachePortalCache.class);

	private MemcachedClientIF _memcachedClient;
	private int _timeout;
	private TimeUnit _timeoutTimeUnit;
	private int _timeToLive;

}