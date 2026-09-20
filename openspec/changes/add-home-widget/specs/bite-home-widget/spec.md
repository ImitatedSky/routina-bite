# bite-home-widget

## ADDED Requirements

### Requirement: Widget shows today's calories and water
The widget SHALL show, for the current calendar day, the remaining calories (target minus consumed,
which MAY be negative) with a progress bar, and the water total against its target with a progress bar.

#### Scenario: Nothing logged yet
- **WHEN** the day has no entries and no water
- **THEN** remaining shows the full calorie target and both progress bars are empty

#### Scenario: Over the calorie target
- **WHEN** consumed calories exceed the target
- **THEN** remaining shows a negative number and the calorie bar is full

### Requirement: Widget always reflects the current day
The widget SHALL read the current calendar day, never the date the app happens to be showing.

#### Scenario: App is browsing an old date
- **WHEN** the app's Today screen is showing 2025-10-13 and the widget updates
- **THEN** the widget still shows today's numbers

#### Scenario: Midnight
- **WHEN** the device broadcasts that the date changed
- **THEN** the widget redraws with the new day's numbers

### Requirement: Log a glass of water from the widget
The widget SHALL offer a button that adds 250 ml to today's water without opening any screen,
confirming with a short message, and the widget's own numbers SHALL update.

#### Scenario: Two taps
- **WHEN** the button is tapped twice
- **THEN** today's water increases by 500 ml in total and the widget shows the new total

### Requirement: Open the app from the widget
The widget SHALL offer a button that opens the add-entry screen for today, and tapping elsewhere
on the widget SHALL open the Today screen.

#### Scenario: App not running
- **WHEN** the app's process is not running and the add button is tapped
- **THEN** the app starts and lands on the add-entry screen

### Requirement: Widget updates when the data changes
The widget SHALL redraw when diary entries, water or targets change, without waiting for the
system's periodic update.

#### Scenario: Entry added in the app
- **WHEN** an entry is added on the Today screen
- **THEN** the widget's remaining calories change to match

### Requirement: Widget needs no permissions
Adding the widget SHALL NOT require any runtime or install-time permission.

#### Scenario: Install
- **WHEN** the app is installed
- **THEN** the system still reports that the app requests no permissions
