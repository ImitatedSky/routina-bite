# bite-diary

## ADDED Requirements

### Requirement: Calories are derived from macros unless specified
The entry form SHALL fill the calorie field from the macros (protein and carbohydrates at 4 kcal/g,
fat at 9 kcal/g) whenever the user has not specified a calorie value.

#### Scenario: Typing macros
- **WHEN** the user enters 20 g protein and 10 g fat on an empty form
- **THEN** the calorie field shows 170

#### Scenario: User specifies calories
- **WHEN** the user types 500 into the calorie field and then edits carbohydrates
- **THEN** the calorie field still shows 500

#### Scenario: Handing it back to the calculation
- **WHEN** the user clears the calorie field
- **THEN** the calorie field is filled from the macros again

#### Scenario: A food's label value is not overwritten
- **WHEN** a food is picked from the library and its labelled calories do not equal the 4/9/4 sum
- **THEN** the calorie field keeps the food's labelled value
