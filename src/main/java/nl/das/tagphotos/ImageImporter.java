/*
 * Copyright © 2020 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 01 July 2020.
 */

package nl.das.tagphotos;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.meta.exif.Exif;
import com.icafe4j.image.meta.exif.ExifTag;
import com.icafe4j.image.meta.iptc.IPTC;
import com.icafe4j.image.meta.iptc.IPTCApplicationTag;
import com.icafe4j.image.meta.iptc.IPTCDataSet;
import com.icafe4j.image.meta.iptc.IPTCTag;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import ij.IJ;
import ij.ImagePlus;
import lombok.extern.slf4j.Slf4j;
import nl.das.tagphotos.model.Photo;
import nl.das.tagphotos.model.TagIndex;

/**
 * Importing a photo.
 */
@Slf4j
public class ImageImporter {

	private String photosPath;
	private String panoramasPath;
	private String videosPath;
	private MongoDatabase database;

	/**
	 * Constructor
	 *
	 * @param conf     configuration properties
	 * @param database MongoDatabase object
	 */
	public ImageImporter(Properties conf, MongoDatabase database) {
		this.database = database;
		this.photosPath = conf.getProperty("path.photos");
		if (this.photosPath.lastIndexOf('/') != (this.photosPath.length() - 1)) {
			this.photosPath += "/";
		}
		this.panoramasPath = conf.getProperty("path.panoramas");
		if (this.panoramasPath.lastIndexOf('/') != (this.panoramasPath.length() - 1)) {
			this.panoramasPath += "/";
		}
		this.videosPath = conf.getProperty("path.videos");
		if (this.videosPath.lastIndexOf('/') != (this.videosPath.length() - 1)) {
			this.videosPath += "/";
		}
	}

	/**
	 * Import an image located in the given path.
	 * 
	 * @param path
	 * @return the GUID of the image
	 */
	public String importImage(String path) {
		try {
			String name = Paths.get(path).getFileName().toString();
			String id = UUID.randomUUID().toString();
			IPTC iptc = (IPTC) Metadata.readMetadata(path).get(MetadataType.IPTC);
			Exif exif = (Exif) Metadata.readMetadata(path).get(MetadataType.EXIF);
			String tags = getKeywords(iptc);
			log.debug("Importing photo '" + name + "' with id " + id + " and tags '" + tags + "'");
			String year = tags.split(";")[0];
			ImagePlus org = IJ.openImage(path);
			// Check if all folders are there. If not create them
			Files.createDirectories(Paths.get(this.photosPath + year + "/originals/"));
			Files.createDirectories(Paths.get(this.photosPath + year + "/photos/"));
			Files.createDirectories(Paths.get(this.photosPath + year + "/thumbs/"));
			// Check if photo was previously imported. If so, remove it first
			String oldid = findAndRemove(name);
			if (oldid != null) {
				Files.deleteIfExists(Paths.get(this.photosPath + year + "/originals/" + oldid + ".jpg"));
				Files.deleteIfExists(Paths.get(this.photosPath + year + "/photos/" + oldid + ".jpg"));
				Files.deleteIfExists(Paths.get(this.photosPath + year + "/thumbs/" + oldid + ".jpg"));
			}
			// Now save it
			IJ.save(org, this.photosPath + year + "/originals/" + id + ".jpg");
			ImagePlus photo = resize(768, 1024, org);
			IJ.save(photo, this.photosPath + year + "/photos/" + id + ".jpg");
			ImagePlus thumb = resize(150, 150, org);
			IJ.save(thumb, this.photosPath + year + "/thumbs/" + id + ".jpg");
			Photo p = new Photo();
			p.setId(id);
			p.setTags(tags);
			p.setPath(this.photosPath + year);
			p.setOrigFilename(name);
			p.setOrigWidth((int) getWidthAndHeight(exif).getWidth());
			p.setOrigHeight((int) getWidthAndHeight(exif).getHeight());
			p.setPhotoHeight(photo.getHeight());
			p.setPhotoWidth(photo.getWidth());
			p.setThumbHeight(thumb.getHeight());
			p.setThumbWidth(thumb.getWidth());
			p.setCreationDate(getCreationDate(iptc));
			MongoCollection<Photo> pcol = this.database.getCollection("photos", Photo.class);
			save(p, pcol);
			log.debug("Imported photo '" + name + "' with id " + id);
			return id;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Retrieve the list of keywords from an IPTC metadata block.
	 * 
	 * @param meta the IPTC metadata block
	 * @return
	 */
	protected String getKeywords(IPTC meta) {
		String tags = "";
		Map<IPTCTag, List<IPTCDataSet>> dataSets = meta.getDataSets();
		for (IPTCTag ds : dataSets.keySet()) {
			if (ds == IPTCApplicationTag.KEY_WORDS) {
				for (IPTCDataSet d : dataSets.get(ds)) {
					tags += d.getDataAsString().toLowerCase() + ";";
				}
			}
		}
		return tags.isEmpty() ? "" : tags.substring(0, tags.length() - 1);
	}

	/**
	 * Retrieve the width and height from the EXIF metadata block.
	 * 
	 * @param meta the EXIF metadata block
	 * @return
	 */
	protected Dimension getWidthAndHeight(Exif meta) {
		String w = meta.getAsString(ExifTag.EXIF_IMAGE_WIDTH).replace("[", "").replace("]", "");
		String h = meta.getAsString(ExifTag.EXIF_IMAGE_HEIGHT).replace("[", "").replace("]", "");
		return new Dimension(Integer.parseInt(w), Integer.parseInt(h));
	}

	/**
	 * Retrieve the Creation Date of a photo from the given IPTC metadata block
	 * 
	 * @param meta the IPTC metadata block
	 * @return
	 */
	protected long getCreationDate(IPTC meta) {
		Map<IPTCTag, List<IPTCDataSet>> dataSets = meta.getDataSets();
		String dt = null;
		String tm = null;
		for (IPTCTag ds : dataSets.keySet()) {
			if (ds == IPTCApplicationTag.DATE_CREATED) {
				dt = dataSets.get(ds).get(0).getDataAsString();
			} else if (ds == IPTCApplicationTag.TIME_CREATED) {
				tm = dataSets.get(ds).get(0).getDataAsString();
			}
		}
		if ((dt != null) && (tm != null)) {
			SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd HHmmss");
			try {
				return fmt.parse(dt + " " + tm.split("[+]")[0]).getTime();
			} catch (ParseException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		return 0;
	}

	/**
	 * Create a new resized image.
	 * 
	 * @param maxHeight the maximun height of the new image
	 * @param maxWidth  the maximun width of the new image
	 * @param img       the original image
	 * @return the new resized image
	 */
	protected ImagePlus resize(int maxHeight, int maxWidth, ImagePlus img) {
		ImagePlus newimg = img;
		int srcWidth = img.getWidth();
		int srcHeight = img.getHeight();
		if ((srcWidth > maxWidth) || (srcHeight > maxHeight)) {
			double ratio = ((double) srcHeight / (double) srcWidth);

			int dstWidth = maxWidth;
			int dstHeight = (int) Math.round(dstWidth * ratio);
			if (dstHeight > maxHeight) {
				dstHeight = maxHeight;
				dstWidth = (int) Math.round(dstHeight / ratio);
			}
			String options = "none";
			newimg = img.resize(dstWidth, dstHeight, options);
		}
		return newimg;

	}

	/**
	 * Insert the image in the database.
	 * 
	 * @param img
	 */
	public void save(Photo photo, MongoCollection<Photo> pcol) {
		if (photo != null) {
			pcol.insertOne(photo);
			String tags = photo.getTags();
			if ((tags != null) && (tags.length() > 0)) {
				MongoCollection<TagIndex> tcol = this.database.getCollection("tags", TagIndex.class);
				for (String tag : tags.split(";")) {
					TagIndex i = findTag(tag, tcol);
					if (i != null) {
						i.addId(photo.getId());
						tcol.updateOne(eq("tag", tag.toLowerCase()), set("ids", i.getIds()));
					} else {
						TagIndex ti = new TagIndex();
						ti.setTag(tag.toLowerCase());
						ti.addId(photo.getId());
						tcol.insertOne(ti);
					}
				}
			}
		}
	}

	/**
	 * Find a tag in the database.
	 * 
	 * @param tag  the tag to find
	 * @param tcol the MongoCollection<TagIndex> to be searched.
	 * @return
	 */
	public TagIndex findTag(String tag, MongoCollection<TagIndex> tcol) {
		MongoCursor<TagIndex> cursor = tcol.find(eq("tag", tag)).cursor();
		if (cursor.hasNext()) {
			return cursor.next();
		} else {
			return null;
		}
	}

	/**
	 * Find id of image with given name and remove it from the database.
	 * 
	 * @param name the original filename of the photo
	 * @return
	 */
	public String findAndRemove(String name) {
		log.debug("[findAndRemove()] " + name);
		MongoCollection<Photo> collection = this.database.getCollection("photos", Photo.class);
		MongoCursor<Photo> pcur = collection.find(eq("origFilename", name)).cursor();
		if (pcur.hasNext()) {
			// Remove it from database
			Photo p = pcur.next();
			String id = p.getId();
			pcur.close();
			collection.findOneAndDelete(eq("_id", id));
			// Also remove id from TagIndex
			MongoCollection<TagIndex> tcol = this.database.getCollection("tags", TagIndex.class);
			for (String tag : p.getTags().split(";")) {
				MongoCursor<TagIndex> ticur = tcol.find(eq("tag", tag)).cursor();
				if (ticur.hasNext()) {
					TagIndex ti = ticur.next();
					Set<String> ids = ti.getIds();
					log.debug("'" + id + (ids.remove(id) ? "' found" : "' not found") + " in tag '" + tag + "'");
					if (ids.isEmpty()) {
						tcol.deleteOne(eq("tag", tag));
					} else {
						tcol.updateOne(eq("tag", tag), set("ids", ids));
					}
				}
				ticur.close();
			}
			log.debug("[findAndRemove()] Found and removed: " + id);
			return id;
		} else {
			pcur.close();
			log.debug("[findAndRemove()] Not found");
			return null;
		}
	}
}
