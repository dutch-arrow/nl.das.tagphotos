/*
 * Copyright © 2020 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 04 July 2020.
 */

package nl.das.tagphotos;

import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.junit.Test;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.meta.exif.Exif;
import com.icafe4j.image.meta.exif.ExifTag;
import com.icafe4j.image.meta.iptc.IPTC;
import com.icafe4j.image.meta.iptc.IPTCApplicationTag;
import com.icafe4j.image.meta.iptc.IPTCDataSet;
import com.icafe4j.image.meta.iptc.IPTCTag;
import com.icafe4j.image.tiff.TiffField;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import nl.das.tagphotos.model.Photo;

/**
 * https://github.com/dragon66/icafe/wiki/Metatadata-Manipulation-and-More-Cool-Things
 */
public class MetadataTest {

	@Test
	public void test() {
		PojoCodecProvider provider = PojoCodecProvider.builder().register("nl.das.tagphotos.model").build();
		CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), fromProviders(provider));
		ServerAddress sa = new ServerAddress("localhost", 27017);
		try (MongoClient mongoClient = new MongoClient(sa, MongoClientOptions.builder().codecRegistry(pojoCodecRegistry).build())) {
			MongoDatabase database = mongoClient.getDatabase("album");
			MongoCollection<Photo> pcol = database.getCollection("photos", Photo.class);
			MongoCursor<Photo> pcur = pcol.find(eq("origFilename", "54-19960054.JPG")).cursor();
			if (pcur.hasNext()) {
				Photo p = pcur.next();
				System.out.println(p.getId());
			} else {
				System.out.println("Not found");
			}

		}
	}

	@Test
	public void testImage() {
		try {
			String path = "/home/dutch/Workspaces/java/nl.das.tagphotos/src/test/resources/54-19960054.JPG";
			IPTC iptc = (IPTC) Metadata.readMetadata(path).get(MetadataType.IPTC);
			if (iptc != null) {
				String tags = "";
				Map<IPTCTag, List<IPTCDataSet>> dataSets = iptc.getDataSets();
				for (IPTCTag ds : dataSets.keySet()) {
					if (ds == IPTCApplicationTag.KEY_WORDS) {
						for (IPTCDataSet d : dataSets.get(ds)) {
							tags += d.getDataAsString().toLowerCase() + ";";
						}
					}
				}
				System.out.println(tags.substring(0, tags.length() - 1));
			} else {
				System.out.println("No IPTC data found.");
			}
			Exif exif = (Exif) Metadata.readMetadata(path).get(MetadataType.EXIF);
			if (exif != null) {
				for (TiffField<?> f : exif.getExifIFD().getFields()) {
					System.out.println(ExifTag.fromShort(f.getTag()).getName() + "=" + f.getDataAsString());
				}
			} else {
				System.out.println("No EXIF data found.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
