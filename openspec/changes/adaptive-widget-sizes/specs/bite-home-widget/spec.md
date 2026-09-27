# bite-home-widget

## MODIFIED Requirements

### Requirement: Widget shows today's calories and water
The widget SHALL show, for the current calendar day, the remaining calories (target minus consumed,
which MAY be negative), and SHALL show the water total against its target and both progress bars
whenever its size leaves room for them. The remaining calories SHALL be shown at every size.

#### Scenario: Nothing logged yet
- **WHEN** the day has no entries and no water
- **THEN** remaining shows the full calorie target and both progress bars are empty

#### Scenario: Over the calorie target
- **WHEN** consumed calories exceed the target
- **THEN** remaining shows a negative number and the calorie bar is full

#### Scenario: Shrunk to a single cell
- **WHEN** the widget is one cell wide and one cell high
- **THEN** it shows the remaining calories and nothing else

### Requirement: Open the app from the widget
The widget SHALL offer, at its full size, a button that opens the add-entry screen for today and a
button that adds water, and tapping anywhere else on the widget SHALL open the Today screen. At
sizes too short for a button, the whole widget SHALL open the Today screen.

#### Scenario: App not running
- **WHEN** the app's process is not running and the add button is tapped
- **THEN** the app starts and lands on the add-entry screen

#### Scenario: One cell high
- **WHEN** the widget is one cell high and is tapped anywhere
- **THEN** the Today screen opens and nothing is logged

## ADDED Requirements

### Requirement: Widgets adapt their layout to the size the user gave them
Each Bite widget SHALL choose its layout from the width and height the home screen reports, redraw
that widget when the reported size changes, and SHALL be resizable down to one cell in each
direction while still being placed at its designed size.

#### Scenario: Resized down
- **WHEN** the user drags a four-by-two widget down to one cell
- **THEN** the widget redraws with the single-cell layout without being removed and re-added

#### Scenario: Resized back up
- **WHEN** the user drags it back to four by two
- **THEN** the full layout comes back with the same numbers

#### Scenario: Two copies at different sizes
- **WHEN** the same widget is placed twice, one small and one full size
- **THEN** each keeps its own layout when the data changes

#### Scenario: The home screen reports no size
- **WHEN** the home screen reports no size for a widget
- **THEN** the widget draws its full layout

### Requirement: Log a glass of water from a one-cell widget
A separate widget SHALL, at one cell in size, act as a single button that adds 250 ml to today's
water without opening any screen, confirm with a short message, and show today's running total.

#### Scenario: Tap
- **WHEN** the widget is tapped
- **THEN** 250 ml is added to today's water, a short message confirms it, and the widget's total
  changes to match

#### Scenario: No second target
- **WHEN** any part of the widget is tapped
- **THEN** water is logged; the widget has no other tap target

#### Scenario: Made bigger
- **WHEN** the widget is enlarged past one cell
- **THEN** the same content is shown in larger type, with the unit spelled out

### Requirement: Widgets are told apart in the picker
Each widget SHALL carry its own name and its own description, so the home screen's widget picker
lists them distinctly.

#### Scenario: Picking a widget
- **WHEN** the user opens the widget picker under Routina Bite
- **THEN** three entries are listed under distinct names, not three times the app's name
