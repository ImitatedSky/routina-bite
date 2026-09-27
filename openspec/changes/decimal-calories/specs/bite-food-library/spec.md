# bite-food-library

## ADDED Requirements

### Requirement: A food's calories are shown as stored
A single food's or entry's calories SHALL be shown as stored wherever they are displayed or put
back into an input field, to two decimal places, with trailing zeros and a trailing decimal point
removed. Totals over a day or a meal MAY be rounded to whole calories.

#### Scenario: A label with decimals
- **WHEN** a food stored as 168.32 kcal is listed or opened in the editor
- **THEN** it reads 168.32, not 168

#### Scenario: A whole number
- **WHEN** a food stored as 193 kcal is listed
- **THEN** it reads 193, not 193.00

#### Scenario: Opening and saving without changes
- **WHEN** a food stored as 337.89 kcal is opened in the editor and saved untouched
- **THEN** it is still 337.89

#### Scenario: Day total
- **WHEN** the day's entries add up to 1832.47 kcal
- **THEN** the Today screen's consumed figure reads 1832
