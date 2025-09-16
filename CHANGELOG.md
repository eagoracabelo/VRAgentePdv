# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial release of Salt-Minion Universal Installer
- Cross-platform support (Linux and Windows)
- Interactive installation mode
- Silent installation mode with command-line arguments
- Automatic platform detection
- Salt Master connectivity validation
- Service management (start, stop, status)
- Configuration backup functionality
- Uninstallation support
- Kotlin Native implementation for performance

### Features
- **Interactive Mode**: Guided installation with user prompts
- **Silent Mode**: Automated installation with predefined parameters
- **Platform Detection**: Automatic detection of OS and architecture
- **Network Validation**: Validates connectivity to Salt Master before installation
- **Service Management**: Creates and manages Salt Minion system service
- **Backup**: Creates backup of existing configuration before updates
- **Uninstall**: Clean removal of Salt Minion and configuration

### Supported Platforms
- Linux (Ubuntu, Debian, CentOS, RHEL, Fedora)
- Windows (Windows 10, Windows Server 2016+)

## [1.0.0] - TBD

### Added
- Initial stable release