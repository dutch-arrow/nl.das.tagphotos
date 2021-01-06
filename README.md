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

### Data structure

##### Index: album

| Property     | type   | format                                                |
| ------------ | ------ | ----------------------------------------------------- |
| _id          | string | string representation of a GUID                       |
| creationDate | long   | seconds since 1-1-1970                                |
| origFilename | string | name of the original file                             |
| origHeight   | int    |                                                       |
| origWidth    | int    |                                                       |
| path         | string | path to base folder                                   |
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









