# bite-entry-form

## ADDED Requirements

### Requirement: The entry form takes the whole nutrition label
The entry form SHALL offer every nutrient the food library stores — calories, protein, fat,
saturated fat, trans fat, carbohydrates, sugar, sodium and cholesterol — keeping the four
commonly used ones always visible and the rest behind a disclosure that opens by default when
any of them already has a value.

#### Scenario: Filling a secondary nutrient
- **WHEN** the user opens the disclosure and enters a sodium figure
- **THEN** it is stored with the entry and scales with the servings like every other nutrient

#### Scenario: Opening an entry that has them
- **WHEN** an entry that records sodium is opened for editing
- **THEN** the secondary nutrients are already showing

#### Scenario: Secondary nutrients do not change the calories
- **WHEN** saturated fat, trans fat, sugar, sodium or cholesterol is entered
- **THEN** the automatically calculated calories do not change

### Requirement: Picking a food brings its note along
Choosing a food from the library SHALL fill the entry's note with that food's note, which the
user MAY then change or clear for this entry alone.

#### Scenario: Food with a note
- **WHEN** a food whose note reads "no mayo" is picked
- **THEN** the entry's note field reads "no mayo" and the food is left unchanged

#### Scenario: Editing the note afterwards
- **WHEN** the user changes the note and saves the entry
- **THEN** only the entry carries the new text

### Requirement: A quick entry can be saved to the library at the same time
The add-entry screen SHALL offer to store what the user just typed as a library food, revealing
the fields only a library food needs when the offer is accepted, and SHALL require a name before
the entry can be added that way.

#### Scenario: Accepting the offer
- **WHEN** the user fills the form, ticks the offer and adds the entry
- **THEN** a food is created from the per-serving values and the entry is linked to it, so it
  appears under the recently used foods

#### Scenario: Two servings
- **WHEN** the entry records two servings of something worth 80 calories each
- **THEN** the food stores 80 calories, not 160

#### Scenario: No name
- **WHEN** the offer is ticked and the name is blank
- **THEN** the entry cannot be added until a name is given

#### Scenario: The food came from the library
- **WHEN** the user picked an existing food
- **THEN** the offer is not shown

## MODIFIED Requirements

### Requirement: Calories are calculated from the macronutrients
The form SHALL calculate calories from protein, fat and carbohydrates whenever the user has not
given a figure of their own, SHALL stop doing so as soon as they type one, and SHALL resume when
they clear the field. The food editor and the entry form SHALL use one and the same rule.

#### Scenario: Macros entered
- **WHEN** protein and fat are entered and calories were left alone
- **THEN** calories show the calculated figure

#### Scenario: Calories typed by hand
- **WHEN** the user types a calorie figure and then changes a macronutrient
- **THEN** the typed figure stays

#### Scenario: Calories cleared
- **WHEN** the user clears the calorie field
- **THEN** the calculated figure comes back

#### Scenario: A food's labelled calories
- **WHEN** a food carrying labelled calories is picked
- **THEN** those calories are kept rather than replaced by the calculation
