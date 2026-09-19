# bite-diary

## ADDED Requirements

### Requirement: Clearing the inline entry form
The add-entry screen SHALL offer a 清空 control next to 加入 that resets the form to an empty entry —
empty name, one serving, no grams, empty nutrient fields, no photo and no note — while keeping the meal
the user has selected. Clearing SHALL also delete any photo file written during this form session,
since no entry refers to it any more.

#### Scenario: Wrong food picked
- **WHEN** the user picks the wrong food and taps 清空
- **THEN** every field goes back to its empty state and the selected meal stays as it was

#### Scenario: Clearing drops the pending photo
- **WHEN** the user attaches a photo and then taps 清空
- **THEN** the photo row returns to 加照片 and the file written for that photo is deleted

## MODIFIED Requirements

### Requirement: One entry form for every entry
The system SHALL use one set of entry form fields for both adding and editing a diary entry.
When adding, the fields SHALL be embedded in the add-entry screen and ready for input as soon as that screen
opens; when editing an existing entry from the today screen, the same fields SHALL be shown in a dialog.
The fields SHALL be the same in both cases: name, servings, grams (only when a grams-per-serving basis exists),
kcal, protein, fat, carbohydrates, meal and a note. The confirm control SHALL read 加入 on the add-entry screen
and 儲存 in the edit dialog.

#### Scenario: Adding starts on the form
- **WHEN** the user taps ＋ on the today screen
- **THEN** the add-entry screen opens with an empty form already on screen, and the name field can be typed
  into without any further tap

#### Scenario: Logging from the food library
- **WHEN** the user taps a food on the add-entry screen
- **THEN** the form above is refilled with that food's name, its grams per serving, one serving and its kcal,
  protein, fat and carbohydrates, all of them editable, the meal the user selected is kept, and the screen
  scrolls back to the top so the filled-in values are visible

#### Scenario: No dialog when picking a food
- **WHEN** the user taps a food in 常吃, 最近, the search results or a category group
- **THEN** no dialog opens; the entry is added by the 加入 control after the form has been filled

#### Scenario: Quick add starts blank
- **WHEN** the add-entry screen opens
- **THEN** the form shows an empty name, one serving, empty nutrient fields and no grams field

#### Scenario: Editing an existing entry
- **WHEN** the user opens an entry that was logged from a food
- **THEN** a dialog shows that entry's name, servings, grams, kcal, protein, fat, carbohydrates,
  meal and note, and every one of them can be changed

#### Scenario: Editing a quick-add entry
- **WHEN** the user opens an entry that has no food behind it
- **THEN** the dialog lets the user change its name and nutrients, so correcting it does not require
  deleting and re-entering it

#### Scenario: Grams field only when there is a basis
- **WHEN** the entry has no grams per serving
- **THEN** the form shows the servings field without a grams field

### Requirement: Quick add without a food
The system SHALL let the user add an entry without creating a food, using the form that the add-entry screen
already shows. The add-entry screen SHALL NOT offer a separate 快速輸入 button: its form starts blank and that
is the quick-add path. The name field SHALL start empty and show 快速輸入 as a placeholder; an entry saved with
a blank name SHALL be stored as 快速輸入. Such entries have `foodId = null`.

#### Scenario: Quick add kcal only
- **WHEN** the user opens the add-entry screen, types 450 into the kcal field and taps 加入
- **THEN** an entry named 快速輸入 with 450 kcal and zero macros is added to the selected meal

#### Scenario: No quick-add button
- **WHEN** the user looks at the add-entry screen
- **THEN** there is no 快速輸入 button, because the form it used to open is already on the screen

#### Scenario: Name field is ready to type in
- **WHEN** the user opens the add-entry screen
- **THEN** the name field is empty with 快速輸入 shown as a hint, so typing needs no deletion first

#### Scenario: Quick add by servings
- **WHEN** the user quick-adds 300 kcal with the servings set to 2
- **THEN** the entry totals 300 kcal and is stored as 2 servings of 150 kcal each

### Requirement: Add screen ordering
The add-entry screen SHALL be laid out, top to bottom, as: the meal chips, the entry form, a divider, then the
food picker — the 新建食物 and 食物庫 entry points, 常吃 (top 12 food ids by entry count in the last 90 days),
最近 (12 most recently logged distinct food ids, both excluding quick-add entries and deleted foods),
the search field and the foods grouped by category. The whole screen SHALL scroll as one list, and the 加入
and 清空 controls SHALL sit in a bottom bar that stays visible however far the list is scrolled.
加入 SHALL be disabled while the servings amount is not a number greater than zero.

#### Scenario: Frequently eaten food
- **WHEN** a food was logged 13 times in the last 90 days and others fewer
- **THEN** it appears first in 常吃

#### Scenario: The add button is always in reach
- **WHEN** the user scrolls down to the category groups
- **THEN** 加入 and 清空 are still shown at the bottom of the screen

#### Scenario: Nothing to add yet
- **WHEN** the servings field is empty or zero
- **THEN** 加入 is disabled

#### Scenario: Form first, foods below
- **WHEN** the add-entry screen opens
- **THEN** the meal chips and the form fields are visible without scrolling, and the food picker starts below
  the divider
