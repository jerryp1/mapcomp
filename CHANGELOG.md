# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Reminder

### Guiding Principles

- Changelogs are for humans, not machines. 
- There should be an entry for every single version. 
- The same types of changes should be grouped. 
- Versions and sections should be linkable. 
- The latest version comes first. 
- The release date of each version is displayed. 
- Mention whether you follow [Semantic Versioning](https://semver.org/).

### Types of Changes

- `Added` for new features.
- `Changed` for changes in existing functionality.
- `Deprecated` for soon-to-be removed features.
- `Removed` for now removed features.
- `Fixed` for any bug fixes.
- `Security` in case of vulnerabilities.

## \[Unreleased\]

## \[0.0.1\] - 2022-07-31

### Added

- Add `README.md` for the dictionary `data` and for modules `mpc4j-common-jnagmp`, `mpc4j-crypto-phe`.
- Add `LISENSE.txt` for modules `mpc4j-common-jnagmp`, `mpc4j-crypto-phe`.

### Changed

- Remove waiting times when running performance tests in `PsuMain`, `PidMain` and `PmidMain`, since `synchronize` in `Rpc` can help synchronize status between different parties and waiting time for synchronization is now unnecessary.
- Scan source codes using IDEA plugin "Alibaba Java Coding Guidelines" and refine codes based on the scanning report.
- `Cm20MpOprf`: use UNSAFE to have more efficient int array to byte array conversion. The performance test shows about 50% improvement. See [Convert a byte array into an int array in java](https://stackoverflow.com/questions/43079234/convert-a-byte-array-into-an-int-array-in-java) for more details.