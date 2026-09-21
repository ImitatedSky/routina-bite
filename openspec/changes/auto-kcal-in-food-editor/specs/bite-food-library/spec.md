# bite-food-library

## ADDED Requirements

### Requirement: The food editor derives calories from macros
The food editor SHALL fill the calorie field from the macros (protein and carbohydrates at 4 kcal/g,
fat at 9 kcal/g) whenever the user has not specified a calorie value.

#### Scenario: Creating a food from a label
- **WHEN** the user creates a food and enters 20 g protein and 10 g fat
- **THEN** the calorie field shows 170

#### Scenario: User specifies calories
- **WHEN** the user types a calorie value and then edits a macro
- **THEN** the calorie field keeps the typed value

#### Scenario: Handing it back
- **WHEN** the user clears the calorie field
- **THEN** it is filled from the macros again

#### Scenario: Editing an existing food
- **WHEN** a food with labelled calories is opened for editing and a macro is changed
- **THEN** the labelled calorie value is not overwritten
