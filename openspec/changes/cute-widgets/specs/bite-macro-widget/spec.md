# bite-macro-widget

## MODIFIED Requirements

### Requirement: Widget shows today's calories and macros
The widget SHALL show, for the current calendar day, the calories consumed inside a ring whose
segments give each macronutrient's share of those calories (protein and carbohydrates at 4 kcal per
gram, fat at 9), and SHALL show the target and the grams of protein, fat and carbohydrates whenever
its size leaves room for them. The consumed calories SHALL be shown at every size.

#### Scenario: Nothing logged
- **WHEN** the day has no entries
- **THEN** the ring is an empty track, calories show 0 and all three macros show 0.0 g

#### Scenario: Mixed day
- **WHEN** 30 g protein, 20 g fat and 80 g carbohydrates have been eaten
- **THEN** the ring shows three segments of roughly 19%, 29% and 52% and the centre reads 620

#### Scenario: Macros use their own colours
- **WHEN** the widget is drawn
- **THEN** each segment, dot and figure for a macro uses the colour the app uses for it

#### Scenario: Shrunk to a single cell
- **WHEN** the widget is one cell wide and one cell high
- **THEN** it shows the ring with the calories consumed inside it and nothing else

#### Scenario: One cell high and four wide
- **WHEN** the widget is one cell high and at least four cells wide
- **THEN** it shows the ring, the calories consumed and each macro's grams next to a dot in its colour

#### Scenario: One cell high and two wide
- **WHEN** the widget is one cell high and too narrow for the macro figures
- **THEN** it shows the ring and the calories consumed alone
