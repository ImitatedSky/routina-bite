# bite-food-library

## MODIFIED Requirements

### Requirement: Editing a food
The food editor SHALL present the same nutrient fields, in the same order and under the same
labels, as the entry form, and SHALL state that its figures are per serving. Name and calories
SHALL remain required.

#### Scenario: Same fields as the entry form
- **WHEN** the food editor and the entry form are compared
- **THEN** both show the same four nutrients first and the same five behind the same disclosure

#### Scenario: Per serving
- **WHEN** the food editor is open
- **THEN** its hint says the figures are for one serving, unlike the entry form's total

#### Scenario: Missing name or calories
- **WHEN** save is pressed with either left blank
- **THEN** the food is not saved and the offending field is marked
