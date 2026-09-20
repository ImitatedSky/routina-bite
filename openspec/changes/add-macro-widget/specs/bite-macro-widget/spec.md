# bite-macro-widget

## ADDED Requirements

### Requirement: Widget shows today's calories and macros
The widget SHALL show, for the current calendar day, calories consumed against the target with a
progress bar, and the consumed grams of protein, fat and carbohydrates.

#### Scenario: Nothing logged
- **WHEN** the day has no entries
- **THEN** calories show 0 against the target and all three macros show 0.0 g

#### Scenario: Macros use their own colours
- **WHEN** the widget is drawn
- **THEN** each macro value uses the same colour the app uses for it, and a lighter variant in dark mode

### Requirement: Macro widget follows the current day
The widget SHALL read the current calendar day, never the date the app is showing, and SHALL redraw
when the date changes.

#### Scenario: Midnight
- **WHEN** the device broadcasts that the date changed
- **THEN** the widget shows the new day's numbers

### Requirement: Macro widget updates with the data
The widget SHALL redraw when diary entries or targets change.

#### Scenario: Entry added
- **WHEN** an entry is added anywhere in the app
- **THEN** the widget's calories and macros change to match

### Requirement: Macro widget opens the app
Tapping the widget SHALL open the Today screen.

#### Scenario: Tap
- **WHEN** the widget is tapped
- **THEN** the app opens on today
