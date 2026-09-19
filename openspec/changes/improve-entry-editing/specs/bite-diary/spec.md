# bite-diary

## ADDED Requirements

### Requirement: One entry form for every entry
The system SHALL use a single entry form for adding and editing a diary entry, reached from three places:
quick add on the add-entry screen, tapping a food on the add-entry screen, and opening an existing entry
from the today screen. The form SHALL offer the same fields in all three cases: name, servings,
grams (only when a grams-per-serving basis exists), kcal, protein, fat, carbohydrates, meal and a note.
The confirm button SHALL read 加入 when adding and 儲存 when editing.

#### Scenario: Logging from the food library
- **WHEN** the user taps a food on the add-entry screen
- **THEN** the entry form opens pre-filled with that food's name, its grams per serving, one serving
  and its kcal, protein, fat and carbohydrates, all of them editable

#### Scenario: Quick add starts blank
- **WHEN** the user taps 快速輸入
- **THEN** the same form opens with an empty name, one serving, empty nutrient fields and no grams field

#### Scenario: Editing an existing entry
- **WHEN** the user opens an entry that was logged from a food
- **THEN** the form shows that entry's name, servings, grams, kcal, protein, fat, carbohydrates,
  meal and note, and every one of them can be changed

#### Scenario: Editing a quick-add entry
- **WHEN** the user opens an entry that has no food behind it
- **THEN** the form lets the user change its name and nutrients, so correcting it does not require
  deleting and re-entering it

#### Scenario: Grams field only when there is a basis
- **WHEN** the entry has no grams per serving
- **THEN** the form shows the servings field without a grams field

### Requirement: Nutrient fields show the entry total and drive the stored per-serving values
The four nutrient fields SHALL show the total for this entry, that is the per-serving value multiplied by
the servings. Changing the servings or the grams SHALL recompute all four fields from the per-serving values.
Changing one nutrient field SHALL set that per-serving value to the entered amount divided by the servings,
leaving the other three untouched. The system SHALL store the resulting per-serving values on the entry.

#### Scenario: Servings drive the nutrients
- **WHEN** the user picks a 500 kcal food and changes the servings from 1 to 2
- **THEN** the kcal field reads 1000 and the protein, fat and carbohydrate fields scale the same way

#### Scenario: Typing a nutrient wins
- **WHEN** the user types 900 into the kcal field of a two-serving entry and saves
- **THEN** the entry's total is 900 kcal and the list row shows 900

#### Scenario: Nutrients not shown on the form are kept
- **WHEN** the user logs a food that has sodium and sugar and changes only its kcal
- **THEN** the saved entry still carries that food's sodium and sugar, scaled by the servings

#### Scenario: Servings not usable yet
- **WHEN** the servings field is empty or zero
- **THEN** the confirm button is disabled and the per-serving values are left unchanged

#### Scenario: Zero nutrients show as empty
- **WHEN** a nutrient's per-serving value is zero
- **THEN** its field is shown empty rather than as 0, and an empty field is saved as zero

### Requirement: Entry note
The system SHALL let the user attach one free-text note to each diary entry, editable in the entry form
and shown on the today screen under that entry's amount when it is not empty. This note is separate from
the per-date day note and neither replaces the other.

#### Scenario: Note saved and shown
- **WHEN** the user adds the note "extra sauce" to an entry
- **THEN** the entry row on the today screen shows that note beneath its servings line

#### Scenario: No note
- **WHEN** an entry has an empty note
- **THEN** its row shows no note line and no placeholder

#### Scenario: Reading a diary saved before entry notes existed
- **WHEN** the app loads a `diary.json` or a backup written by a version without the entry note field
- **THEN** every entry loads successfully with an empty note, and the backup `schemaVersion` is still 1

### Requirement: Number fields select their content on focus
Every numeric field in the app SHALL select its current content when it receives focus, so typing replaces it.
Free-text fields, including the entry name and the notes, SHALL NOT do this.

#### Scenario: Replacing a servings value
- **WHEN** the user taps the servings field showing 1 and types 3
- **THEN** the field reads 3 without the user deleting anything first

#### Scenario: Free text is not selected
- **WHEN** the user taps a note field that already has text
- **THEN** the caret is placed without selecting the text, so typing appends instead of replacing

## MODIFIED Requirements

### Requirement: Diary entry snapshot
The system SHALL store each diary entry with the local date, an optional meal, an optional food id,
the food name, servings, optional grams per serving, a copy of the per-serving nutrients taken at logging time
and an optional note. The food id SHALL be kept when the user edits an entry that came from a food,
even if its name or nutrients were changed.

#### Scenario: Food edited after logging
- **WHEN** the user changes a food's kcal after it was logged yesterday
- **THEN** yesterday's entry still shows the kcal it had when logged

#### Scenario: Edited entry keeps its food
- **WHEN** the user edits the kcal of an entry that was logged from a food
- **THEN** the entry still counts towards that food's 常吃 and 最近 ranking

### Requirement: Log by servings or grams
The system SHALL accept a positive servings value, or a positive grams value when the entry has grams per serving,
converting grams to servings as `grams / servingGrams`. The system SHALL store servings only.
When grams were the last amount the user typed, the stored servings SHALL be computed from the grams value itself,
not from the rounded servings shown on screen; when the user changed neither, the entry's existing servings
SHALL be kept exactly.

#### Scenario: Grams input
- **WHEN** the user logs 150 g of a food with 100 g per serving
- **THEN** the entry is stored with servings 1.5

#### Scenario: Grams survive the round trip
- **WHEN** the user enters 150 g for a food whose serving is 46 g and saves
- **THEN** the entry reads 150.0 g afterwards, not 150.6 g

#### Scenario: Amount untouched while editing
- **WHEN** the user opens an entry stored as 3.260869 servings, changes only its meal and saves
- **THEN** the entry still holds 3.260869 servings and still reads 150.0 g

#### Scenario: Grams unavailable
- **WHEN** the selected food has no grams per serving
- **THEN** the grams input is not offered

#### Scenario: Non-positive amount
- **WHEN** the user enters 0 or a negative amount for a new entry
- **THEN** the add button is disabled

### Requirement: Quick add without a food
The system SHALL let the user add an entry without creating a food, using the same form as every other entry.
The name field SHALL start empty and show 快速輸入 as a placeholder; an entry saved with a blank name
SHALL be stored as 快速輸入. Such entries have `foodId = null`.

#### Scenario: Quick add kcal only
- **WHEN** the user quick-adds 500 kcal without typing a name
- **THEN** an entry named 快速輸入 with 500 kcal and zero macros is added to the selected meal

#### Scenario: Name field is ready to type in
- **WHEN** the user opens the quick add form
- **THEN** the name field is empty with 快速輸入 shown as a hint, so typing needs no deletion first

#### Scenario: Quick add by servings
- **WHEN** the user quick-adds 300 kcal with the servings set to 2
- **THEN** the entry totals 300 kcal and is stored as 2 servings of 150 kcal each

### Requirement: Edit and delete entries
The system SHALL let the user change every field of an existing entry — name, servings, grams, kcal, protein,
fat, carbohydrates, meal and note — through the entry form, reachable both by tapping the entry row and from
an 編輯 item in the row's overflow (⋯) menu. The same menu SHALL offer deleting the entry with confirmation.

#### Scenario: Move entry to another meal
- **WHEN** the user changes an entry's meal from 點心 to 晚餐
- **THEN** it appears under 晚餐 and the section subtotals update

#### Scenario: Editing from the overflow menu
- **WHEN** the user opens an entry row's ⋯ menu
- **THEN** it offers 編輯 above 刪除, and 編輯 opens the same form as tapping the row

#### Scenario: Correcting a name and a number
- **WHEN** the user edits an entry's name and its kcal and saves
- **THEN** the today screen shows the new name and the new kcal for that entry, and the day total updates
