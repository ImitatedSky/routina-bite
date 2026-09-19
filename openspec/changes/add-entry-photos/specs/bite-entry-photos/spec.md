# bite-entry-photos

## ADDED Requirements

### Requirement: One photo per diary entry
The system SHALL let the user attach at most one photo to each diary entry. The entry SHALL store only
the photo's file name, and the file SHALL live in the app's private `filesDir/photos/` directory.
An entry with an empty photo name has no photo. The backup `schemaVersion` SHALL stay 1.

#### Scenario: Entry carries its photo
- **WHEN** the user saves an entry after picking a photo
- **THEN** the entry holds a file name such as `9f2c….jpg` and the matching JPEG exists under `filesDir/photos/`

#### Scenario: Reading a diary saved before entry photos existed
- **WHEN** the app loads a `diary.json` or a backup written by a version without the photo field
- **THEN** every entry loads successfully with no photo, and the backup `schemaVersion` is still 1

#### Scenario: Copying yesterday leaves photos behind
- **WHEN** the user taps 複製昨天 for a day whose entries have photos
- **THEN** the copied entries are created without photos, so no two entries share one photo file

### Requirement: Photos come from the system photo picker with no permissions
The system SHALL obtain photos through the system photo picker restricted to images, and the app manifest
SHALL declare no `uses-permission`. The system SHALL copy the picked image into its own storage while the
picker's grant is still valid, and SHALL NOT keep the picker's URI.

#### Scenario: Picking a photo asks for no permission
- **WHEN** the user taps 加照片 on the entry form
- **THEN** the system photo picker opens and no permission dialog is shown, on this release or any other

#### Scenario: Picker dismissed
- **WHEN** the user backs out of the picker without choosing anything
- **THEN** the form is unchanged and no file is written

### Requirement: Stored photos are downscaled
The system SHALL downscale every picked image so its longest edge is at most 1600 pixels and SHALL store it
as JPEG at quality 85, applying the source image's EXIF orientation. Images already within the limit
SHALL NOT be enlarged. Decoding and writing SHALL happen off the main thread.

#### Scenario: A large camera photo
- **WHEN** the user picks a 4000x3000 photo
- **THEN** the stored file is a JPEG whose longest edge is 1600 pixels, typically a few hundred kilobytes

#### Scenario: A small image
- **WHEN** the user picks a 300x300 image
- **THEN** the stored file stays 300x300

#### Scenario: A rotated photo
- **WHEN** the picked photo carries an EXIF orientation of 90 degrees
- **THEN** the stored photo is upright everywhere it is shown

### Requirement: Photos are shown as thumbnails and can be opened full screen
The entry form SHALL show an 加照片 button when the entry has no photo, and a thumbnail with a remove
button when it has one. Tapping that thumbnail SHALL replace the photo; long-pressing it SHALL open the
photo full screen on a black background, dismissed by a single tap. The today screen SHALL show a small
thumbnail at the start of an entry row that has a photo, and SHALL leave rows without a photo unchanged.
Thumbnails SHALL be decoded off the main thread at the size they are displayed, and kept in an in-memory cache.

#### Scenario: Form with no photo
- **WHEN** the user opens the entry form for an entry without a photo
- **THEN** the photo row shows a single 加照片 button above the note field

#### Scenario: Form with a photo
- **WHEN** the entry has a photo
- **THEN** the form shows its thumbnail with a remove (✕) button, and removing it returns the row to 加照片

#### Scenario: Viewing the photo full screen
- **WHEN** the user long-presses the thumbnail on the form
- **THEN** the whole photo is shown centred on a black full-screen background, and one tap closes it

#### Scenario: Today screen row
- **WHEN** an entry with a photo is listed on the today screen
- **THEN** its row starts with a small rounded thumbnail, and rows without a photo keep their current layout

### Requirement: Photo files follow the entry
The system SHALL delete an entry's photo file when the entry is deleted, SHALL delete the replaced file when
the user picks a different photo for an entry, and SHALL delete any file it wrote during a form session that
the user then cancelled. The repository SHALL expose a single delete-photo operation used by all three.

#### Scenario: Deleting an entry
- **WHEN** the user deletes an entry that has a photo
- **THEN** the photo file is removed from `filesDir/photos/` as well

#### Scenario: Replacing a photo
- **WHEN** the user picks a different photo for an entry and saves
- **THEN** only the new file remains and the previous one is gone

#### Scenario: Cancelling the form
- **WHEN** the user picks a photo and then cancels the form
- **THEN** the file written during that session is deleted and the entry keeps whatever photo it had

### Requirement: A missing photo file means no photo
The system SHALL treat an entry whose photo file cannot be found as an entry without a photo. It SHALL NOT
crash, SHALL NOT show a broken-image placeholder, and SHALL NOT clear the stored file name.

#### Scenario: Importing a JSON-only backup
- **WHEN** the user imports a backup that carries photo file names but no photo files
- **THEN** those entries are listed without a thumbnail, the app does not crash, and their rows take no extra space

#### Scenario: The photos come back later
- **WHEN** the user afterwards imports a zip backup containing those photo files
- **THEN** the same entries show their photos again without any further editing
