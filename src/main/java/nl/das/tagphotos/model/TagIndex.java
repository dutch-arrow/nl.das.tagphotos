/*
 * Copyright © 2020 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 08 July 2020.
 */

package nl.das.tagphotos.model;

import java.util.HashSet;
import java.util.Set;

/**
 * 
 */
public class TagIndex {
	private String tag;
	private Set<String> ids = new HashSet<>();

	public void addId(String id) {
		this.ids.add(id);
	}

	public void removeId(String id) {
		this.ids.remove(id);
	}

	/**
	 * @return the tag
	 */
	public String getTag() {
		return this.tag;
	}

	/**
	 * @param tag the tag to set
	 */
	public void setTag(String tag) {
		this.tag = tag;
	}

	/**
	 * @return the ids
	 */
	public Set<String> getIds() {
		return this.ids;
	}

	/**
	 * @param ids the ids to set
	 */
	public void setIds(Set<String> ids) {
		this.ids = ids;
	}

}
