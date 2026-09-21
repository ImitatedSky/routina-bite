# bite-diary

## ADDED Requirements

### Requirement: The name field filters the food library
On the add-entry screen the name field SHALL also filter the food list below it, matching name or
note case-insensitively. There SHALL NOT be a second search field on that screen.

#### Scenario: Typing a name
- **WHEN** the user types "beef" into the name field
- **THEN** the list below shows only foods whose name or note contains "beef", under a heading that
  names what is being matched

#### Scenario: Name is empty
- **WHEN** the name field is empty
- **THEN** the frequent and recent shortcuts are shown above the food library grouped by category

#### Scenario: Name has text
- **WHEN** the name field has any text
- **THEN** the frequent and recent shortcuts are hidden

#### Scenario: Nothing matches
- **WHEN** the typed name matches no food
- **THEN** the list says so, and the entry can still be added as a one-off
