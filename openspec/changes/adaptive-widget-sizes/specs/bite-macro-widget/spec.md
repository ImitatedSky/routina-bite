# bite-macro-widget

## MODIFIED Requirements

### Requirement: Widget shows today's calories and macros
The widget SHALL show, for the current calendar day, the calories consumed, and SHALL also show the
target, the progress bar and the grams of protein, fat and carbohydrates whenever its size leaves
room for them. The consumed calories SHALL be shown at every size.

#### Scenario: Nothing logged
- **WHEN** the day has no entries
- **THEN** calories show 0 against the target and all three macros show 0.0 g

#### Scenario: Macros use their own colours
- **WHEN** the widget is drawn
- **THEN** each macro value uses the same colour the app uses for it, and a lighter variant in dark mode

#### Scenario: Shrunk to a single cell
- **WHEN** the widget is one cell wide and one cell high
- **THEN** it shows the calories consumed and nothing else

#### Scenario: One cell high and four wide
- **WHEN** the widget is one cell high and at least four cells wide
- **THEN** it shows the calories consumed and the three macro figures in their colours, with no
  progress bar

#### Scenario: One cell high and two wide
- **WHEN** the widget is one cell high and too narrow for the macro figures
- **THEN** it shows the calories consumed alone rather than cutting the macro figures off

## ADDED Requirements

### Requirement: Macro widget is resizable down to one cell
The widget SHALL be resizable down to one cell in each direction, redrawing itself with the layout
that fits the size the home screen reports.

#### Scenario: Resized down and back
- **WHEN** the user drags the widget to one cell and then back to four by two
- **THEN** each size draws its own layout with the same numbers, without re-adding the widget
