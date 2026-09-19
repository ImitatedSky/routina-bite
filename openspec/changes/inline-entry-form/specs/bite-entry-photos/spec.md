# bite-entry-photos

## MODIFIED Requirements

### Requirement: Photo files follow the entry
The system SHALL delete an entry's photo file when the entry is deleted, SHALL delete the replaced file when
the user picks a different photo for an entry, and SHALL delete any file it wrote during a form session that
the user then abandoned. A form session is abandoned when the user cancels the edit dialog, clears the
add-entry form, refills it by picking a food, or leaves the add-entry screen without adding the entry.
Once an entry has been added or saved, its photo belongs to that entry and SHALL NOT be deleted by any of
those paths. The repository SHALL expose a single delete-photo operation used by all of them.

#### Scenario: Deleting an entry
- **WHEN** the user deletes an entry that has a photo
- **THEN** the photo file is removed from `filesDir/photos/` as well

#### Scenario: Replacing a photo
- **WHEN** the user picks a different photo for an entry and saves
- **THEN** only the new file remains and the previous one is gone

#### Scenario: Cancelling the edit dialog
- **WHEN** the user picks a photo and then cancels the edit dialog
- **THEN** the file written during that session is deleted and the entry keeps whatever photo it had

#### Scenario: Leaving the add-entry screen
- **WHEN** the user attaches a photo on the add-entry screen and then goes back without tapping 加入
- **THEN** the file written during that session is deleted, so no file is left that no entry refers to

#### Scenario: Picking a food after attaching a photo
- **WHEN** the user attaches a photo and then taps a food, which refills the whole form
- **THEN** the file written for that photo is deleted along with the rest of the discarded input

#### Scenario: The added entry keeps its photo
- **WHEN** the user attaches a photo and taps 加入
- **THEN** the entry is listed with its thumbnail and the file stays in `filesDir/photos/`, even though the
  add-entry screen is closed straight afterwards
