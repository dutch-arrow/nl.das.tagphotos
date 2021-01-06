/*
 * Copyright © 2020 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 01 July 2020.
 */

package nl.das.tagphotos.model;

/**
 * 
 */
public class Photo {
	private String id;
	private String tags;
	private String origFilename;
	private int origWidth;
	private int origHeight;
	private int photoWidth;
	private int photoHeight;
	private int thumbWidth;
	private int thumbHeight;
	private long creationDate;
	private String path;

	public Photo() {
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the tags
	 */
	public String getTags() {
		return this.tags;
	}

	/**
	 * @param tags the tags to set
	 */
	public void setTags(String tags) {
		this.tags = tags;
	}

	/**
	 * @return the origFilename
	 */
	public String getOrigFilename() {
		return this.origFilename;
	}

	/**
	 * @param origFilename the origFilename to set
	 */
	public void setOrigFilename(String origFilename) {
		this.origFilename = origFilename;
	}

	/**
	 * @return the origWidth
	 */
	public int getOrigWidth() {
		return this.origWidth;
	}

	/**
	 * @param origWidth the origWidth to set
	 */
	public void setOrigWidth(int origWidth) {
		this.origWidth = origWidth;
	}

	/**
	 * @return the origHeight
	 */
	public int getOrigHeight() {
		return this.origHeight;
	}

	/**
	 * @param origHeight the origHeight to set
	 */
	public void setOrigHeight(int origHeight) {
		this.origHeight = origHeight;
	}

	/**
	 * @return the photoWidth
	 */
	public int getPhotoWidth() {
		return this.photoWidth;
	}

	/**
	 * @param photoWidth the photoWidth to set
	 */
	public void setPhotoWidth(int photoWidth) {
		this.photoWidth = photoWidth;
	}

	/**
	 * @return the photoHeight
	 */
	public int getPhotoHeight() {
		return this.photoHeight;
	}

	/**
	 * @param photoHeight the photoHeight to set
	 */
	public void setPhotoHeight(int photoHeight) {
		this.photoHeight = photoHeight;
	}

	/**
	 * @return the thumbWidth
	 */
	public int getThumbWidth() {
		return this.thumbWidth;
	}

	/**
	 * @param thumbWidth the thumbWidth to set
	 */
	public void setThumbWidth(int thumbWidth) {
		this.thumbWidth = thumbWidth;
	}

	/**
	 * @return the thumbHeight
	 */
	public int getThumbHeight() {
		return this.thumbHeight;
	}

	/**
	 * @param thumbHeight the thumbHeight to set
	 */
	public void setThumbHeight(int thumbHeight) {
		this.thumbHeight = thumbHeight;
	}

	/**
	 * @return the creationDate
	 */
	public long getCreationDate() {
		return this.creationDate;
	}

	/**
	 * @param creationDate the creationDate to set
	 */
	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

}
