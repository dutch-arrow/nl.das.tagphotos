/*
 * Copyright © 2020 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 03 July 2020.
 */

package nl.das.tagphotos;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;
import static com.mongodb.client.model.Updates.set;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.meta.iptc.IPTC;
import com.icafe4j.image.meta.iptc.IPTCApplicationTag;
import com.icafe4j.image.meta.iptc.IPTCDataSet;
import com.icafe4j.image.meta.iptc.IPTCTag;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import lombok.extern.slf4j.Slf4j;
import nl.das.tagphotos.model.Photo;
import nl.das.tagphotos.model.TagIndex;

/**
 * 
 */
@Slf4j
public class TagPhotos {

	private static MongoClient mongoClient;
	private static MongoDatabase database;
	private static Properties conf = new Properties();
	private String photosPath;
	private File selectedFolder;
	private List<Path> photoFiles;
	private List<Photo> photos;
	private int curIndex;
	private String curTags;
	private String action;
	private String year;

	// Swing components
	private JFrame frmTagAPhoto;
	private ImagePanel pnlImage;
	private JTextField txtYear;
	private JTextField txtTags;
	private JButton btnPrev;
	private JButton btnNext;
	private JButton btnSave;
	private JButton btnImport;
	private JPanel pnlSouth;
	private JComboBox<String> cmbYear;
	private JLabel lblChosenFolder;
	private JLabel lblFile;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			conf.load(new FileInputStream("/usr/local/etc/tagphotos.properties"));
		} catch (FileNotFoundException e) {
			log.error("'tagphotos.properties' file was not found in folder '/usr/local/etc'.");
			System.exit(1);
		} catch (IOException e) {
			log.error("'tagphotos.properties' file could not be read: " + e.getMessage());
			System.exit(1);
		}
		// Open the database
		PojoCodecProvider provider = PojoCodecProvider.builder().register("nl.das.tagphotos.model").build();
		CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), fromProviders(provider));
		ServerAddress sa = new ServerAddress("localhost", 27017);
		mongoClient = new MongoClient(sa, MongoClientOptions.builder().codecRegistry(pojoCodecRegistry).build());

		EventQueue.invokeLater(() -> {
			try {
				TagPhotos window = new TagPhotos();
				window.frmTagAPhoto.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * Create the application.
	 */
	public TagPhotos() {
		database = mongoClient.getDatabase("album");
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 * 
	 * @wbp.parser.entryPoint
	 */
	private void initialize() {
		this.frmTagAPhoto = new JFrame();
		this.frmTagAPhoto.setTitle("Tag a photo");
		this.frmTagAPhoto.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.frmTagAPhoto.setSize(1200, 800);
		this.frmTagAPhoto.setLocationRelativeTo(null);

		JPanel pnlNorth = new JPanel();
		this.frmTagAPhoto.getContentPane().add(pnlNorth, BorderLayout.NORTH);
		pnlNorth.setLayout(new GridLayout(0, 1, 0, 0));

		this.pnlImage = new ImagePanel();

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		pnlNorth.add(tabbedPane);

		JPanel pnlTabImport = new JPanel();
		tabbedPane.addTab("Import Photos", null, pnlTabImport, null);
		pnlTabImport.setLayout(new BoxLayout(pnlTabImport, BoxLayout.Y_AXIS));

		JPanel pnlFolderSelection = new JPanel();
		pnlFolderSelection.setBorder(new EmptyBorder(10, 0, 5, 0));
		pnlTabImport.add(pnlFolderSelection);
		pnlFolderSelection.setLayout(new BorderLayout(10, 10));

		JButton btnSelectFolder = new JButton("Select folder");
		btnSelectFolder.setMargin(new Insets(5, 14, 5, 14));
		btnSelectFolder.setHorizontalTextPosition(SwingConstants.LEFT);
		btnSelectFolder.setHorizontalAlignment(SwingConstants.LEFT);
		pnlFolderSelection.add(btnSelectFolder, BorderLayout.WEST);

		this.lblChosenFolder = new JLabel("No folder selected");
		this.lblChosenFolder.setSize(new Dimension(1000, 15));
		this.lblChosenFolder.setHorizontalAlignment(SwingConstants.LEFT);
		pnlFolderSelection.add(this.lblChosenFolder, BorderLayout.CENTER);

		this.lblFile = new JLabel("");

		this.btnImport = new JButton("Import folder");
		this.btnImport.setEnabled(false);
		this.btnImport.addActionListener(e -> {
			ImageImporter imp = new ImageImporter(conf, database);
			if (this.photoFiles != null) {
				for (Path path : this.photoFiles) {
					imp.importImage(path.toString());
				}
			}
		});
		this.btnImport.setHorizontalAlignment(SwingConstants.RIGHT);
		pnlFolderSelection.add(this.btnImport, BorderLayout.EAST);

		btnSelectFolder.addActionListener(event -> {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fileChooser.setCurrentDirectory(this.selectedFolder);
			int option = fileChooser.showOpenDialog(TagPhotos.this.frmTagAPhoto);
			if (option == JFileChooser.APPROVE_OPTION) {
				this.action = "import";
				TagPhotos.this.selectedFolder = fileChooser.getSelectedFile();
				this.lblChosenFolder.setText("Folder selected: " + this.selectedFolder.getAbsolutePath());
				try {
					this.photoFiles = Files.list(Paths.get(this.selectedFolder.getAbsolutePath()))
							.filter(p -> (p.toString().endsWith(".jpg") || p.toString().endsWith(".JPG")))
							.collect(Collectors.toList());
					if ((this.photoFiles != null) && (this.photoFiles.size() > 0)) {
						this.pnlImage.setImage(this.photoFiles.get(0).toString());
						this.pnlImage.repaint();
						this.curIndex = 0;
						if (this.photoFiles.size() == 1) {
							this.btnPrev.setVisible(false);
							this.btnNext.setVisible(false);
							this.btnSave.setVisible(true);
						} else {
							this.btnPrev.setVisible(true);
							this.btnNext.setVisible(true);
							this.btnPrev.setEnabled(false);
							this.btnNext.setEnabled(true);
							this.btnSave.setVisible(false);
						}
						this.btnImport.setEnabled(true);
						this.lblFile.setText(this.photoFiles.get(this.curIndex).getFileName().toString());
						this.curTags = getTags((IPTC) Metadata.readMetadata(this.photoFiles.get(0).toString()).get(MetadataType.IPTC));
						this.txtYear.setText(this.curTags.substring(0, this.curTags.indexOf(";")));
						this.txtTags.setText(this.curTags.substring(this.curTags.indexOf(";") + 1));

					} else {
						JOptionPane.showMessageDialog(this.frmTagAPhoto, "No photos found in this folder.",
								"Dialog", JOptionPane.ERROR_MESSAGE);
						this.pnlImage.clearImage();
						this.pnlImage.repaint();
						this.curIndex = 0;
						this.btnPrev.setEnabled(false);
						this.btnNext.setEnabled(false);
						this.btnSave.setVisible(false);
						this.btnImport.setEnabled(false);
						this.lblFile.setText("");
						this.curTags = null;
						this.txtYear.setText("");
						this.txtTags.setText("");
					}
				} catch (IOException e) {
					JOptionPane.showMessageDialog(this.frmTagAPhoto, Utils.stacktraceAsString(e),
							"Error Dialog", JOptionPane.ERROR_MESSAGE);
				}
			} else {
				if (this.selectedFolder == null) {
					this.lblChosenFolder.setText("No folder selected");
					this.btnImport.setEnabled(false);
				}
			}
		});

		JPanel pnlTabManage = new JPanel();
		pnlTabManage.setBorder(new EmptyBorder(10, 0, 5, 0));

		tabbedPane.addTab("Manage Photos", null, pnlTabManage, null);

		JLabel lblNewLabel = new JLabel("Choose year");
		pnlTabManage.add(lblNewLabel);

		this.cmbYear = new JComboBox<>();
		this.cmbYear.setMaximumRowCount(20);
		fillCmbYear();
		this.photosPath = conf.getProperty("path.photos");
		if (this.photosPath.lastIndexOf('/') != (this.photosPath.length() - 1)) {
			this.photosPath += "/";
		}
		this.cmbYear.addActionListener(event -> {
			this.year = (String) this.cmbYear.getSelectedItem();
			this.photos = getAllPhotosOfYear(this.year);
			this.action = "manage";
			this.pnlImage.setImage(this.photosPath + this.year + "/photos/" + this.photos.get(0).getId() + ".jpg");
			this.pnlImage.repaint();
			if (this.photos.size() == 1) {
				this.btnPrev.setVisible(false);
				this.btnNext.setVisible(false);
				this.btnSave.setVisible(true);
			} else {
				this.btnPrev.setVisible(true);
				this.btnNext.setVisible(true);
				this.btnPrev.setEnabled(false);
				this.btnNext.setEnabled(true);
				this.btnSave.setVisible(false);
			}
			this.curTags = this.photos.get(0).getTags();
			this.txtYear.setText(this.curTags.substring(0, this.curTags.indexOf(";")));
			this.txtTags.setText(this.curTags.substring(this.curTags.indexOf(";") + 1));
			this.curIndex = 0;
			log.debug("Show photo '" + this.photosPath + this.year + "/photos/" + this.photos.get(0).getId() + ".jpg'");
		});
		pnlTabManage.add(this.cmbYear);

		JPanel pnlNextPrev = new JPanel();
		pnlNextPrev.setBorder(new EmptyBorder(5, 0, 0, 0));
		pnlNorth.add(pnlNextPrev);
		FlowLayout flowLayout = (FlowLayout) pnlNextPrev.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		this.btnPrev = new JButton("<");
		this.btnNext = new JButton(">");

		this.btnPrev.setEnabled(false);
		this.btnPrev.addActionListener(event -> {
			// has the tags changed?
			String newTags = this.txtYear.getText() + ";" + this.txtTags.getText().toLowerCase();
			this.txtTags.setText(this.txtTags.getText().toLowerCase());
			try {
				if (tagsUpdated()) {
					// then update the metadata in the current photo
					JOptionPane.showMessageDialog(this.frmTagAPhoto,
							"Tags: '" + this.curTags + "' changed in '" + newTags + "'",
							"Information Dialog", JOptionPane.INFORMATION_MESSAGE);
					if (this.action.equalsIgnoreCase("import")) {
						changeKeywords(this.photoFiles.get(this.curIndex).toString(), newTags);
					} else {
						this.photos.get(this.curIndex).setTags(newTags);
						update(this.photos.get(this.curIndex));
					}
				}
				this.curIndex--;
				if (this.action.equalsIgnoreCase("import")) {
					this.lblFile.setText(this.photoFiles.get(this.curIndex).getFileName().toString());
					this.pnlImage.setImage(this.photoFiles.get(this.curIndex).toString());
					this.pnlImage.repaint();
					this.btnPrev.setEnabled((this.curIndex - 1) >= 0);
					this.btnNext.setEnabled((this.curIndex + 1) < this.photoFiles.size());
					this.curTags = getTags((IPTC) Metadata.readMetadata(this.photoFiles.get(this.curIndex).toString()).get(MetadataType.IPTC));
					this.txtYear.setText(this.curTags.substring(0, this.curTags.indexOf(";")));
					this.txtTags.setText(this.curTags.substring(this.curTags.indexOf(";") + 1));
					log.debug("Show photo '" + this.photoFiles.get(this.curIndex).toString() + "'");
				} else {
					this.lblFile.setText(this.photos.get(this.curIndex).getOrigFilename());
					this.pnlImage.setImage(this.photosPath + this.year + "/photos/" + this.photos.get(this.curIndex).getId() + ".jpg");
					this.pnlImage.repaint();
					this.btnPrev.setEnabled((this.curIndex - 1) >= 0);
					this.btnNext.setEnabled((this.curIndex + 1) < this.photos.size());
					this.curTags = this.photos.get(this.curIndex).getTags();
					this.txtYear.setText(this.curTags.substring(0, this.curTags.indexOf(";")));
					this.txtTags.setText(this.curTags.substring(this.curTags.indexOf(";") + 1));
					log.debug("Show photo '" + this.photosPath + this.year + "/photos/" + this.photos.get(this.curIndex).getId() + ".jpg'");
				}
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this.frmTagAPhoto, Utils.stacktraceAsString(e),
						"Error Dialog", JOptionPane.ERROR_MESSAGE);
			}
		});
		pnlNextPrev.add(this.btnPrev);

		this.btnNext.setEnabled(false);
		this.btnNext.addActionListener(event -> {
			// has the tags changed?
			String newTags = this.txtYear.getText() + ";" + this.txtTags.getText().toLowerCase();
			this.txtTags.setText(this.txtTags.getText().toLowerCase());
			try {
				if (tagsUpdated()) {
					// then update the metadata in the current photo
					JOptionPane.showMessageDialog(this.frmTagAPhoto,
							"Tags: '" + this.curTags + "' changed in '" + newTags + "'",
							"Information Dialog", JOptionPane.INFORMATION_MESSAGE);
					if (this.action.equalsIgnoreCase("import")) {
						changeKeywords(this.photoFiles.get(this.curIndex).toString(), newTags);
					} else {
						this.photos.get(this.curIndex).setTags(newTags);
						update(this.photos.get(this.curIndex));
					}
				}
				this.curIndex++;
				if (this.action.equalsIgnoreCase("import")) {
					this.lblFile.setText(this.photoFiles.get(this.curIndex).getFileName().toString());
					this.pnlImage.setImage(this.photoFiles.get(this.curIndex).toString());
					this.pnlImage.repaint();
					this.btnPrev.setEnabled((this.curIndex - 1) >= 0);
					this.btnNext.setEnabled((this.curIndex + 1) < this.photoFiles.size());
					this.curTags = getTags((IPTC) Metadata.readMetadata(this.photoFiles.get(this.curIndex).toString()).get(MetadataType.IPTC));
					if ((this.curTags != null) && (this.curTags.length() > 0)) {
						this.txtYear.setText(this.curTags.substring(0, this.curTags.indexOf(";")));
						this.txtTags.setText(this.curTags.substring(this.curTags.indexOf(";") + 1));
					} else {
						this.txtYear.setText("");
						this.txtTags.setText("");
					}
				} else {
					this.lblFile.setText(this.photos.get(this.curIndex).getOrigFilename());
					this.pnlImage.setImage(this.photosPath + this.year + "/photos/" + this.photos.get(this.curIndex).getId() + ".jpg");
					this.pnlImage.repaint();
					this.btnPrev.setEnabled((this.curIndex - 1) >= 0);
					this.btnNext.setEnabled((this.curIndex + 1) < this.photos.size());
					this.curTags = this.photos.get(this.curIndex).getTags();
					this.txtYear.setText(this.curTags.substring(0, this.curTags.indexOf(";")));
					this.txtTags.setText(this.curTags.substring(this.curTags.indexOf(";") + 1));
					log.debug("Show photo '" + this.photosPath + this.year + "/photos/" + this.photos.get(this.curIndex).getId() + ".jpg'");
				}
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this.frmTagAPhoto, Utils.stacktraceAsString(e),
						"Error Dialog", JOptionPane.ERROR_MESSAGE);
			}
		});
		pnlNextPrev.add(this.btnNext);
		pnlNextPrev.add(this.lblFile);

		this.btnSave = new JButton("Save");
		this.btnSave.setVisible(false);
		this.btnSave.addActionListener(event -> {
			// has the tags changed?
			String newTags = this.txtYear.getText() + ";" + this.txtTags.getText();
			if (tagsUpdated()) {
				// then update the metadata in the current photo
				JOptionPane.showMessageDialog(this.frmTagAPhoto,
						"Tags: '" + this.curTags + "' changed in '" + newTags + "'",
						"Information Dialog", JOptionPane.INFORMATION_MESSAGE);
				this.photos.get(this.curIndex).setTags(newTags);
				update(this.photos.get(this.curIndex));
				this.curTags = newTags;
			}
		});
		pnlNextPrev.add(this.btnSave);

		this.curTags = null;

		tabbedPane.addChangeListener(event -> {
			int index = tabbedPane.getSelectedIndex();
			if (index == 0) { // Import tab
				this.lblChosenFolder.setText("no folder chosen");
				this.selectedFolder = null;
				this.btnImport.setEnabled(false);
			} else if (index == 1) { // Manage tab
				this.cmbYear.setSelectedIndex(0);
			}
			this.pnlImage.clearImage();
			this.pnlImage.repaint();
			this.btnPrev.setVisible(true);
			this.btnNext.setVisible(true);
			this.btnPrev.setEnabled(false);
			this.btnNext.setEnabled(false);
			this.btnSave.setVisible(false);
			this.lblFile.setText("");
		});

		this.frmTagAPhoto.getContentPane().add(this.pnlImage, BorderLayout.CENTER);

		this.pnlSouth = new JPanel();
		this.pnlSouth.setBorder(new EmptyBorder(10, 0, 0, 0));
		FlowLayout fl_pnlSouth = (FlowLayout) this.pnlSouth.getLayout();
		fl_pnlSouth.setAlignment(FlowLayout.LEFT);
		this.frmTagAPhoto.getContentPane().add(this.pnlSouth, BorderLayout.SOUTH);

		JLabel lblYear = new JLabel("Year");
		this.pnlSouth.add(lblYear);

		this.txtYear = new JTextField();
		this.pnlSouth.add(this.txtYear);
		this.txtYear.setColumns(4);

		JLabel lblTags = new JLabel("Other tags");
		this.pnlSouth.add(lblTags);

		this.txtTags = new JTextField();
		this.txtTags.setToolTipText("Each tag may contain spaces and must be separated by semicolon");
		this.pnlSouth.add(this.txtTags);
		this.txtTags.setColumns(90);
	}

	public void close() {
		mongoClient.close();
	}

	private boolean tagsUpdated() {
		return !(this.txtYear.getText() + ";" + this.txtTags.getText()).equalsIgnoreCase(this.curTags);
	}

	/**
	 * Support function: get the value of the given directory and tag from the given
	 * Metadata object.
	 * 
	 * @param dirName
	 * @return the value as a string or NULL if not found
	 */
	private String getTags(IPTC iptc) {
		String tags = "";
		Map<IPTCTag, List<IPTCDataSet>> dataSets = iptc.getDataSets();
		for (IPTCTag ds : dataSets.keySet()) {
			if (ds == IPTCApplicationTag.KEY_WORDS) {
				for (IPTCDataSet d : dataSets.get(ds)) {
					tags += d.getDataAsString().toLowerCase() + ";";
				}
			}
		}
		return tags.isEmpty() ? "" : tags.substring(0, tags.length() - 1);
	}

	private void changeKeywords(String jpegFile, String keywords) throws IOException {
		String dst1 = Files.createTempFile("", "jpg").toString();
		// get metadata
		Map<MetadataType, Metadata> metadataMap = Metadata.readMetadata(jpegFile);
		IPTC iptc = (IPTC) metadataMap.get(MetadataType.IPTC);
		// replace all keywords
		List<IPTCDataSet> dss = new ArrayList<>();
		boolean found = false;
		for (IPTCTag tag : iptc.getDataSets().keySet()) {
			for (IPTCDataSet ds : iptc.getDataSets().get(tag)) {
				if (tag == IPTCApplicationTag.KEY_WORDS) {
					found = true;
					for (String kw : keywords.split(";")) {
						dss.add(new IPTCDataSet(IPTCApplicationTag.KEY_WORDS, kw.toLowerCase()));
					}
				} else {
					dss.add(ds);
				}
			}
			if (!found) {
				for (String kw : keywords.split(";")) {
					dss.add(new IPTCDataSet(IPTCApplicationTag.KEY_WORDS, kw.toLowerCase()));
				}
			}
		}
		// save metadata again
		Metadata.insertIPTC(new FileInputStream(jpegFile), new FileOutputStream(dst1), dss, false);
		Files.move(Paths.get(dst1), Paths.get(jpegFile), StandardCopyOption.REPLACE_EXISTING);
		Files.deleteIfExists(Paths.get(dst1));
	}

	private void fillCmbYear() {
		MongoCollection<TagIndex> colTags = database.getCollection("tags", TagIndex.class);
		MongoCursor<TagIndex> cursor = colTags.find(regex("tag", "[0-9]{4}")).cursor();
		while (cursor.hasNext()) {
			this.cmbYear.addItem(cursor.next().getTag());
		}
		cursor.close();
	}

	private List<Photo> getAllPhotosOfYear(String year) {
		List<Photo> photos = new ArrayList<>();
		MongoCollection<TagIndex> colTags = database.getCollection("tags", TagIndex.class);
		MongoCollection<Photo> colPhotos = database.getCollection("photos", Photo.class);
		MongoCursor<TagIndex> cursor = colTags.find(eq("tag", year)).cursor();
		if (cursor.hasNext()) {
			for (String id : cursor.next().getIds()) {
				photos.add(colPhotos.find(eq("_id", id)).first());
			}
		}
		cursor.close();
		return photos;
	}

	/**
	 * Update the image in the database.
	 * 
	 * @param img
	 */
	private void update(Photo photo) {
		log.debug("[update()] Photo " + photo.getId());
		MongoCollection<Photo> pcol = database.getCollection("photos", Photo.class);
		String oldtags = pcol.find(eq("_id", photo.getId())).first().getTags();
		log.debug("[update()] Old tags: '" + oldtags + "', new tags: '" + photo.getTags());
		// Update photo
		pcol.findOneAndReplace(eq("_id", photo.getId()), photo);
		// Also update the TagIndex
		MongoCollection<TagIndex> tcol = database.getCollection("tags", TagIndex.class);
		for (String tag : oldtags.split(";")) {
			// is this tag not in the new tags? Remove the id from the TagIndex record
			if (!photo.getTags().contains(tag)) {
				MongoCursor<TagIndex> ticur = tcol.find(eq("tag", tag)).cursor();
				if (ticur.hasNext()) {
					TagIndex ti = ticur.next();
					Set<String> ids = ti.getIds();
					ids.remove(photo.getId());
					// if there are no ids anymore, remove the TagIndexrecord
					if (ids.isEmpty()) {
						tcol.deleteOne(eq("tag", tag));
					} else {
						// update the existing TagIndex record
						tcol.updateOne(eq("tag", tag), set("ids", ids));
					}
				}
				ticur.close();
			}
		}
		for (String tag : photo.getTags().split(";")) {
			// is this tag not in the old tags? Add the id to the TagIndex record
			if (!oldtags.contains(tag)) {
				MongoCursor<TagIndex> ticur = tcol.find(eq("tag", tag)).cursor();
				if (ticur.hasNext()) {
					// Update TagIndexrecord and add id
					TagIndex ti = ticur.next();
					Set<String> ids = ti.getIds();
					ids.add(photo.getId());
					tcol.updateOne(eq("tag", tag), set("ids", ids));
				} else {
					// Create a new TagIndex record
					TagIndex ti = new TagIndex();
					ti.setTag(tag.toLowerCase());
					Set<String> ids = new HashSet<>();
					ids.add(photo.getId());
					ti.setIds(ids);
					tcol.insertOne(ti);
				}
				ticur.close();
			}
		}
	}

	class ImagePanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private ImageIcon imageIcon;

		public void setImage(String path) {
			this.imageIcon = new ImageIcon(path);
		}

		public void clearImage() {
			this.imageIcon = null;
		}

		@Override
		public void paintComponent(Graphics g) {
			if (this.imageIcon != null) {
				double factor = getScaleFactorToFit(
						new Dimension(this.imageIcon.getIconWidth(), this.imageIcon.getIconHeight()), getSize());
				int scaledWidth = (int) (this.imageIcon.getIconWidth() * factor);
				int scaledHeight = (int) (this.imageIcon.getIconHeight() * factor);
				int posx = (getWidth() - scaledWidth) / 2; // Center horizontally
				g.clearRect(0, 0, getWidth(), getHeight());
				g.drawImage(this.imageIcon.getImage(), posx, 0, scaledWidth, scaledHeight, this);
			} else {
				g.clearRect(0, 0, getWidth(), getHeight());
			}
		}

		private double getScaleFactor(int iMasterSize, int iTargetSize) {
			double dScale = 1;
			if (iMasterSize > iTargetSize) {
				dScale = (double) iTargetSize / (double) iMasterSize;
			} else {
				dScale = (double) iTargetSize / (double) iMasterSize;
			}
			return dScale;
		}

		private double getScaleFactorToFit(Dimension original, Dimension toFit) {
			double dScale = 1d;
			if ((original != null) && (toFit != null)) {
				double dScaleWidth = getScaleFactor(original.width, toFit.width);
				double dScaleHeight = getScaleFactor(original.height, toFit.height);
				dScale = Math.min(dScaleHeight, dScaleWidth);
			}
			return dScale;
		}
	}

	@FunctionalInterface
	public interface SimpleDocumentListener extends DocumentListener {
		void update(DocumentEvent e);

		@Override
		default void insertUpdate(DocumentEvent e) {
			update(e);
		}

		@Override
		default void removeUpdate(DocumentEvent e) {
			update(e);
		}

		@Override
		default void changedUpdate(DocumentEvent e) {
			update(e);
		}
	}
}
