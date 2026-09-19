# bite-settings-backup

## MODIFIED Requirements

### Requirement: Export backup
The system SHALL export foods, diary entries, weights, day notes and targets via the system file picker.
When no entry has a photo the export SHALL be a single JSON file named `bite-backup-yyyyMMdd.json`
with MIME type `application/json`, exactly as before. When at least one entry has a photo the export
SHALL instead be a ZIP named `bite-backup-yyyyMMdd.zip` with MIME type `application/zip`, containing
`backup.json` in that same JSON format plus one `photos/<file name>.jpg` entry per stored photo.

#### Scenario: Export without photos
- **WHEN** the user taps 匯出備份 and no entry has a photo
- **THEN** a `bite-backup-yyyyMMdd.json` file with `app = "bite"`, `schemaVersion = 1` and all four
  collections plus targets is written

#### Scenario: Export with photos
- **WHEN** the user taps 匯出備份 and some entries have photos
- **THEN** a `bite-backup-yyyyMMdd.zip` is written holding `backup.json` with the same contents as the
  JSON export, and one `photos/…jpg` entry for each photo those entries reference

#### Scenario: A referenced photo file is gone
- **WHEN** an entry names a photo whose file no longer exists
- **THEN** the zip is still written, simply without that photo, and the export reports success

### Requirement: Import merges by id and never deletes
The system SHALL import a backup chosen with the system file picker, accepting both the JSON file and the ZIP
produced by the export. The system SHALL decide which it is from the file's first two bytes (`PK` means ZIP)
and not from its name or MIME type. For a ZIP the system SHALL first extract every `photos/` entry into
`filesDir/photos/`, overwriting a file of the same name, and then merge the contained `backup.json`.
Merging SHALL be unchanged: foods, diary entries and weights by id (imported wins), day notes by date, and
targets applied when present. Existing data not in the file SHALL be kept.

#### Scenario: Import the converted spreadsheet
- **WHEN** the user imports `bite-import.json` into a fresh install
- **THEN** the library still has 78 foods (same ids as seed), the diary has 184 entries across 31 dates,
  weights has 7 entries and 2025-09-15 carries the note 感冒日

#### Scenario: Import twice
- **WHEN** the same file is imported again
- **THEN** counts do not change

#### Scenario: Import a zip backup
- **WHEN** the user imports a `bite-backup-yyyyMMdd.zip` into a fresh install
- **THEN** the diary entries come back with their photos and the thumbnails are shown again

#### Scenario: A zip without a backup.json
- **WHEN** the user picks a ZIP that carries no `backup.json`
- **THEN** nothing changes and the message says the file cannot be read

#### Scenario: Import a plain JSON after photos exist
- **WHEN** the user imports a JSON-only backup while photo files are already stored
- **THEN** the merge runs as usual and no photo file is deleted
