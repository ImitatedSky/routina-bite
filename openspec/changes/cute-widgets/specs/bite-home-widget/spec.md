# bite-home-widget

## MODIFIED Requirements

### Requirement: Widget shows today's calories and water
The widget SHALL show, for the current calendar day, how many calories remain against the target as
a round progress ring with a large figure beside or inside it, and SHALL show water against its
target as a second ring whenever its size leaves room for it. When consumed calories exceed the
target, the widget SHALL say by how much it is over rather than showing a negative number.

#### Scenario: Nothing logged yet
- **WHEN** the day has no entries and no water
- **THEN** the figure shows the full calorie target and both rings are empty

#### Scenario: Partway through the day
- **WHEN** 620 of 2000 calories have been eaten
- **THEN** the calorie ring is about a third full and the figure reads 1380 under a "left to eat" label

#### Scenario: Over the calorie target
- **WHEN** consumed calories exceed the target by 120
- **THEN** the figure reads 120 under an "over" label and the ring is full in a deeper shade

#### Scenario: Water target reached
- **WHEN** the day's water reaches its target
- **THEN** the water label says the target is met

#### Scenario: Shrunk to a single cell
- **WHEN** the widget is one cell wide and one cell high
- **THEN** it shows the calorie ring with the remaining figure inside it and nothing else

### Requirement: Log a glass of water from a one-cell widget
A separate widget SHALL act as a single button that adds 250 ml to today's water without opening any
screen, confirm with a short message, and show today's progress as a water ring; at one cell the
button label sits inside the ring, and when enlarged the widget SHALL also say how much is left.

#### Scenario: Tap
- **WHEN** the widget is tapped
- **THEN** 250 ml is added to today's water, a short message confirms it, and the ring grows to match

#### Scenario: No second target
- **WHEN** any part of the widget is tapped
- **THEN** water is logged; the widget has no other tap target

#### Scenario: Made bigger
- **WHEN** the widget is enlarged past one cell and is wide enough
- **THEN** it shows today's total, the target and how much is still to drink
