# bite-macro-widget

## MODIFIED Requirements

### Requirement: Widget shows today's calories and macros
The widget SHALL show, for the current calendar day, the calories consumed inside a ring whose
segments give each macronutrient's share of those calories, and SHALL add the target and the grams
of protein, fat and carbohydrates in the longest form that fits the space it is given. The consumed
calories SHALL be shown at every size.

#### Scenario: Nothing logged
- **WHEN** the day has no entries
- **THEN** the ring is an empty track, calories show 0 and the macros show 0

#### Scenario: Three cells wide, one cell high
- **WHEN** the widget is one cell high and about three cells wide
- **THEN** it shows the calories and each macro's grams next to a dot in its colour, dropping the
  calorie unit if that is what it takes to fit

#### Scenario: Wide enough for names
- **WHEN** the widget is wide enough for short names
- **THEN** each macro reads like "蛋白 30" next to its dot

#### Scenario: Two cells wide, one cell high
- **WHEN** the macro figures do not fit
- **THEN** the ring and the calories are shown alone, centred

#### Scenario: Shrunk to a single cell
- **WHEN** the widget is one cell wide and one cell high
- **THEN** it shows the ring with the calories consumed inside it and nothing else
