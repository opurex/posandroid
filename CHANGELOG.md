# Project Changelog - Feature Documentation

All notable changes to this project are documented in this file. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

**Version Format**: Level.Version (Level = compatibility between software, Version = release number)

---

## [8.2] - June 15, 2020

### ✨ New Features
- **Ticket Reprinting**: Added ability to reprint previous Z tickets for audit purposes
- **Enhanced Printer Status**: Documents are now queued when printer is unavailable with status messages
- **Auto-Reconnection**: Automatic Bluetooth reconnection when connection is lost

### 🐛 Bug Fixes
- Orders now properly labeled as "Ticket" when printed
- Cash register name included in ticket numbers for better tracking
- Improved printer availability handling and status display

---

## [8.1] - May 27, 2020

### ✨ New Features
- **Multi-Printer Support**: Support for 1 main printer + 2 auxiliary printers
- **Flexible Printing**: Configurable buttons to print orders to any of the 3 printers
- **Cash Management**:
    - Cash counting when opening/closing sessions
    - Perpetual cash session tracking
- **Enhanced Login**: Cash register (machine name) display on login form
- **Driver Support**: New Epson ePOS driver integration

### 🔒 Security Improvements
- Password field masking in configuration screen

### 🐛 Bug Fixes
- Better error handling when error messages are null
- Fixed ticket cutting on compatible hardware

---

## [8.0 alpha6] - December 8, 2018

### ✨ New Features
- **Print Control**: New payment screen button to choose whether to print ticket
- **Default Print Setting**: Configuration option to set default ticket printing behavior

### 📊 Improvements
- Enhanced ticket list showing: date, time, customer, user, amount, and payment mode

### 🐛 Bug Fixes
- Fixed crash when assigning customers without tariff areas to tickets

---

## [8.0 alpha5] - March 6, 2018

### ✨ New Features
- **Cash Drawer Control**: Restored manual cash drawer open button

### 🔄 Behavior Changes
- Automatic cash drawer opening when registering payments

---

## [8.0 alpha4] - February 13, 2018

### 🐛 Critical Bug Fixes
- Fixed crash when sending tickets with arbitrary products
- Corrected tax amount calculation in Z tickets
- Resolved empty printer crash issues

---

## [8.0 alpha3] - January 30, 2018

### ✨ New Features
- **Product Compositions**: Restored composition functionality
- **Enhanced Connectivity**:
    - Device connection service for faster, more reliable connections
    - Lowercase Bluetooth address support for LKP and Woosim printers

### 🔄 Improvements
- **Order Management**:
    - Negative quantities allowed when editing lines
    - Mutable order lines restored
    - Better ticket splitting with cancel protection
- **Session Management**:
    - Rejected tickets automatically discarded
    - Removed always-empty inventory display on session close
    - No default server (prevents invalid connections)

### 🐛 Bug Fixes
- Fixed crashes with payment excess (change) tickets
- Improved printer resource saving
- Better connection detection and error handling
- Enhanced LKP and Woosim printer connection stability

---

## [8.0 alpha1]

### ✨ New Features
- **Advanced Reporting**: Tax breakdown by category on Z tickets
- **Session Management**: Period selection for session closing
- **Product Interaction**: Long-click products to open edit popup

### 🔄 Major Changes
- **API Upgrade**: Now connecting to Opurex-API v8
- **Improved UX**:
    - On-screen warnings for pending tickets (replaces popups)
    - Detailed archive information on debug screen
    - API response display in error popups for debugging
- **Immutable Orders**: Order lines are now immutable for data integrity

### ❌ Removed Features
- Auto-sync mode discontinued
- Local data deletion button removed from debug screen

### 🐛 Bug Fixes
- Correct error messages for invalid user credentials
- Enhanced cash session data handling with conflict prevention

---

## [7.0] - February 14, 2017

### ✨ New Features
- **Order Splitting**: Full order splitting functionality
- **Partial Payments**: Storage of partial payments with orders
- **Enhanced Restaurant Mode**: Map view for place selection (replaces list view)
- **Development Tools**: GitLab pipeline integration and linting

### 🔄 Major Changes
- **API Upgrade**: Connecting to Opurex-API v7
- **UI Improvements**:
    - Case-insensitive customer search
    - Updated popup styling
    - Standard ticket mode as default
- **Configuration**: Default host changed to my.opurex.org/7

### ❌ Removed Features
- Simple ticket mode (keeping standard and restaurant modes only)
- Demo mode discontinued
- SDK manager plugin removed

### 🐛 Bug Fixes
- Fixed crashes with empty data files and Android 4.0.4 payments
- Better out-of-memory error handling
- Resolved product image display issues during scrolling
- Added missing French translations

---

## [6.7.0] - April 15, 2016

### ✨ New Features
- **Crash Reporting**: Email crash logs to fixed support address
- **Smart Payment Flow**: First available payment mode auto-selected after validation
- **Customer Integration**: Ticket labels use customer names when assigned

### 🐛 Bug Fixes
- Improved printer connection detection
- Better customer selection popup behavior
- Enhanced keyboard handling for customer popups

---

## Earlier Versions (6.6.6 - 1.0.0)

### Notable Historical Features

#### Restaurant Mode (v1.1.0 - April 3, 2013)
- Introduction of restaurant-specific functionality

#### Payment Enhancements (v1.0.1 - September 20, 2012)
- Portrait mode payment screen
- Improved soft keyboard behavior

#### Core Foundation (v1.0.0 - August 24, 2012)
- Initial stable release with core POS functionality

---

## Feature Categories Summary

### 🖨️ **Printing & Hardware**
- Multi-printer support (main + 2 auxiliary)
- Epson ePOS driver
- Bluetooth connectivity with auto-reconnection
- Cash drawer integration
- Ticket reprinting capabilities

### 💰 **Payment & Cash Management**
- Partial payment storage
- Cash session tracking
- Automatic cash drawer control
- Payment mode selection optimization

### 📊 **Reporting & Analytics**
- Z ticket tax breakdowns
- Enhanced ticket information display
- Session period management
- Archive detailed reporting

### 🍽️ **Restaurant Operations**
- Order splitting functionality
- Table/place management with map view
- Product compositions
- Customer assignment to tickets

### 🔧 **Technical Improvements**
- API version upgrades (v7, v8)
- Enhanced error handling
- Connection stability improvements
- Data integrity with immutable orders

### 👤 **User Experience**
- Case-insensitive search
- Improved popup interfaces
- Better keyboard handling
- Crash reporting system