# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

The semantic of version number is 'Level.Version'. Level is for compatibility between sofwares and Version is the release number.

## [8.2] - 2020-06-15

### Added
- Reprint previous Z tickets

### Fixed
- Order are not labelled "Ticket" when printed.
- Add the name of the cash register in ticket number.
- Catch bluetooth disconnection and try to reconnect.
- Documents are queued when the printer is not available, display printing status messages.


## [8.1] - 2020-05-27

### Added
- Changelog
- Epson ePOS driver
- Support 1 main printer and 2 auxiliaries.
- Buttons to print orders to one of the 3 configurable printers.
- Count cash when opening and closing the cash session.
- Perpetual CS in cash session.
- Cash register (machine name) on the login from.

### Changed
- The Password field is now masked in the configuration screen.

### Fixed
- Show error when the error message is null.
- Fix ticket cutting that wasn't available on compatible hardware before.


## [8.0 alpha6] - 2018-12-08

### Added
- New button on payment screen to print the ticket or not.
- New setting in configuration to select if the ticket should be printed by default or not.

### Changed
- The ticket list shows date, time, customer, user, amount and payment mode.

### Fixed
- Assigning a customer to a ticket does not crash when the customer has no tariff area.


## [8.0 alpha5] - 2018-03-06

### Added
- Restore the button to open the cash drawer.

### Changed
- The cash drawer opens itself automatically when registering a payment.


## [8.0 alpha4] - 2018-02-13

### Fixed
- Sending a ticket with an arbitrary product does not crash anymore.
- The tax amount is correctly computed in Z tickets instead of full taxed price.
- Empty printer does not crash.


## [8.0 alpha3] - 2018-01-30

### Added
- Compositions are back.

### Changed
- Negative quantity can be set when editing a line.
- Rejected tickets are discarded when sent to the server.
- The inventory is not shown when closing the session anymore. It was always empty anyway.
- There is no default server anymore, instead of an invalid one.
- Allow to enter lowercase bluetooth address for LKP and Woosim printers.
- Device connection is now handled by a service accross activities (faster and more reliable connection).
- Order lines are mutable again.

### Fixed
- Ticket with payment exceedent (give back) does not crash when sent to the server.
- Discarding rejected tickets prevents blocking sending any further data.
- Fix not saving printer resources locally (except the last one).
- Better printer connection detection.
- Connection error with LKP and Woosim printers are now correctly handled.
- Cancelling splitting a ticket does not mess with the order.


## [8.0 alpha2]
- Lost release. Merged with alpha3


## [8.0 alpha1]

### Added
- Tax by category on Z ticket.
- Ask for the period to close.
- Long clicking a product opens the edit popup.

### Changed
- Connecting to Opurex-API v8.
- Cash session sequence is displayed on debug screen.
- Load images on a dedicated sync phase again.
- API response is shown in the error popup to help debugging.
- Show the warning about pending tickets on screen instead of in a popup when closing the session.
- Do not show the warning before entering the debug screen (the user can not delete data anymore).
- Show details about the archives on debug screen.
- Order lines are immutable.

### Removed
- Auto-sync mode.
- The button to delete local data on the debug screen was removed.

### Fixed
- Display the correct message when user credentials are incorrect.
- Better handling of incoming cash session data. Prevent conflicts when possible and warn the user otherwise.


## [7.0] - 2017-02-14

### Added
- Gitlab pipeline.
- Lint.
- License.
- Order splitting.
- Partial payment are stored along the order.
- Note in readme about signing the release apk.

### Changed
- Connecting to Opurex-API v7.
- Customer search is case insensitive.
- Popup style.
- Standard ticket mode is now the default one.
- Restaurant mode now shows the map instead of just the list of places.
- URL to get the source code.
- Default host is now my.opurex.org/7.

### Removed
- SDK manager plugin.
- Simple ticket mode (keeping standard and restaurant mode only).
- Demo mode, the demo account does not exist anymore.

### Fixed
- Fix crashing when reading an empty data file.
- Dependencies with gradle.
- Does not crash on payment with Android 4.0.4.
- Show an error when out of memory instead of crashing.
- Product with no image are not shown the image of an other product when scrolling a lot.
- Missing French translations.


## [6.7.0] - 2016-04-15

### Added
- Option to send crash log by mail to a fixed address.

### Changed
- The first available payment mode is selected after validating a payment.
- The label of the ticket is the customer's name if one is assigned.

### Fixed
- Connection detection with printers.
- Touching outside the customer selection popup closes it.
- Ticket without customer does not crash.
- Do not show the virtual keyboard on non-editable customer popup.


## [6.6.6] - 2016-04-05




## [6.6.5]


## [6.0.0]


## [5.0.1]


## [4.0.0]


## [1.4 alpha1]


## [1.3 alpha1]


## [1.2.1]


## [1.2.0]

## [1.2 alpha1]



## [1.1.1]


## [1.1.0] - 2013-04-03

### Added
- Restaurant mode.


## [1.0.5] - 2013-04-30

### Fixed
- Prevent garbage collecting session data.
- Save session data synchronously and after selecting the user (and creating the first empty ticket).
- Project url in about screen.


## [1.0.4] - 2013-03-12

### Fixed
- Escape host name on sync. It could prevent synchronizing with special characters.


## [1.0.3] - 2013-02-22

### Fixed
- Fix a crash when entering the payment screen on some devices.


## [1.0.2] - 2012-09-28

### Fixed
- Tax rate is correctly read from server.


## [1.0.1] - 2012-09-20

### Added
- Payment screen in portrait mode.

### Changed
- Soft keyboard does not automatically shows itself when entering the payment screen.


## [1.0.0] - 2012-08-24

