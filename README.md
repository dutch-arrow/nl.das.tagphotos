# Photo Tagger program

## Functionality

This program opens photos, extracts all its metadata and creates an index for each photo in a Mongo NoSQL database.  
The photo will then be converted into 2 other sizes (web display size: 800x600 and thumb size: ) and all three copied to a special folder and 
given a GUID as the name and the format as an extension.  
The index will contain the creation date of the original photo, the height and with of the three sizes and its tags.  
Also a second index will be created that contains all photos that belong to a single tag.

## Technical details

### Configuration data

The configuration data is stored in the tagphotos.properties file located in folder _/usr/lib/etc_.  
it contains a property which contains as value a base path for each album content type:
* path.photos
* path.panoramas
* path.videos

For example:
```
    path.photos=/home/dutch/Workspaces/java/nl.das.tagphotos/src/test/resources/photos
    path.panoramas=/home/dutch/Workspaces/java/nl.das.tagphotos/src/test/resources/panoramas
    path.videos=/home/dutch/Workspaces/java/nl.das.tagphotos/src/test/resources/videos
```
This base path is the root folder where the indexed photos/panoramas/videos are copied into the subfolder with the name of the year of creation.  
Photos have an extra subfolder :
* the originals into subfolder _originals_
* the web format into subfolder _photos_
* the thumbnails into subfolder _thumbs_


### Data structure

##### Index: album

| Property     | type   | format                                                |
| ------------ | ------ | ----------------------------------------------------- |
| _id          | string | string representation of a GUID                       |
| creationDate | long   | seconds since 1-1-1970                                |
| origFilename | string | name of the original file                             |
| origHeight   | int    |                                                       |
| origWidth    | int    |                                                       |
| path         | string | path to folder where the files are stored             |
| photoHeight  | int    |                                                       |
| photoWidth   | int    |                                                       |
| tags         | string | semi-colon separated list of words or short sentences |
| thumbHeight  | int    |                                                       |
| thumbWidth   | int    |                                                       |

##### Index: tags

| Property | type             | format                                   |
| -------- | ---------------- | ---------------------------------------- |
| _id      | UUID             | unique object id                         |
| tag      | string           | a single tag                             |
| ids      | array of strings | all ids of the photos that have this tag |


### Program structure

The main program is a Java Swing application that renders the following pages using tabs:
```
+----------------+---------------+------------------+------------------+---------------+---------------+
|  Import Photos | Manage Photos | Import Panoramas | Manage Panoramas | Import Videos | Manage Videos |
+----------------+---------------+------------------+------------------+---------------+---------------+
```
```
  +----------------+
  |  Import Photos |
  +----------------+---------------+------------------+------------------+---------------+---------------+
  |+---------------+                                                                    +---------------+|
1 || Select folder | Path-of-selected-folder                                            | Import folder ||
  |+---------------+                                                                    +---------------+|
  +------------------------------------------------------------------------------------------------------+
  |+---+ +---+                                                                                           |
2 || < | | > |                                                                                           |
  |+---+ +---+                                                                                           |
  |                                                                                                      |
  |                                                                                                      |
3 |                                              Photo                                                   |
  |                                                                                                      |
  |                                                                                                      |
  |                                                                                                      |
  |      +-------+             +------------------------------------------------------------------------+|
4 | Year |       |  Other tags |                                                                        ||
  |      +-------+             +------------------------------------------------------------------------+|
  +------------------------------------------------------------------------------------------------------+

```
#### Line 1
**Select folder** is button that when pressed shows a folder selection dialog. The path of the selected folder is displayed after it.  
**Import folder** is a button that can be pressed when the modification of the fields on line 4 are done for all photos in the selected folder
are done. The whole folder is then imported in the application. If a photo with the same filename is already in the database it will be removed
first and then again inserted.

#### Line 2
Contains a previous and next button to browse through all photos in the selected folder. The filename of the selected photo is shown after the buttons.

#### Line 3
The place where the photo image is displayed.

#### Line 4
**Year** is a field that holds the year of creation of the photo. Initially it is extracted from the metadata, but can be modified.  
**Other Tags** is a field where other tags can be entered, separated by a semi-colon (;). Initially they are extracted from the metadata,
but can be modified. Remember that the tags will be used in searches.

When you browse to another photo the content of the **Year** and **Other tags** field will be saved automatically.  
If both browse buttons are disabled (in case there is only one photo) and a change is made they will be replaced by a single **Save** button.


```
                   +---------------+
                   | Manage Photos |
  +----------------+---------------+------------------+------------------+---------------+---------------+
  |                                                 +--------+---+                                       |
1 |                                     Choose year |        | v |                                       |
  |                                                 +--------+---+                                       |
  +------------------------------------------------------------------------------------------------------+
  |+---+ +---+                                                                                           |
2 || < | | > |                                                                                           |
  |+---+ +---+                                                                                           |
  |                                                                                                      |
  |                                                                                                      |
3 |                                              Photo                                                   |
  |                                                                                                      |
  |                                                                                                      |
  |                                                                                                      |
  |      +-------+             +------------------------------------------------------------------------+|
4 | Year |       |  Other tags |                                                                        ||
  |      +-------+             +------------------------------------------------------------------------+|
  +------------------------------------------------------------------------------------------------------+
```

#### Line 1
**Choose year** is a dropdown with all the years that contain photos. When a year is selected you can browse through all photos.

#### Line 2
Contains a previous and next button to browse through all photos in the selected year.

#### Line 3
The place where the photo image is displayed.

#### Line 4
**Year** is a field that holds the year of creation of the photo.  
**Other Tags** is a field where other tags can be entered, separated by a semi-colon (;). Remember that the tags will be used in searches.

When you browse to another photo the content of the **Year** and **Other tags** field will be saved automatically.  
If both browse buttons are disabled (in case there is only one photo) and a change is made they will be replaced by a single **Save** button.











