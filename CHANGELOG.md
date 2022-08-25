# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## \[1.0.3\]

### Added

- CHANGELOG: We add CHANGELOG.md to write any changes during our development.
- UNSAFE: We find that `byte[]` to `int[]` conversion dominants the cost for Silent OT. We add `unsafeByteArrayToIntArray` in `IntUtils`, and introduce such method in our Silent OT implementation.
- Ecc in OpenSSL: Ecc now supports OpenSSL. This means that we now have C/C++ SM2 implementation in `mpc4j`.

### Changed

- Fixed-Point Multiplication in ECC: In `mpc4j-common-tool`, we introduce Window Method for ECC Fixed-Point Multiplication implemented in MCL into our pure-Java implementation, replacing the pre-computation techniques provided by Bouncy Castle. The efficiency results show that our new implementation is about 10x faster than the original one.
- Multiplication Triple: In `mpc4j-s2pc-pcg`, we merge Boolean Triple Generation (BTG) packages into Multiplication Triple Generation (MTG) packages and rename `booleanTriple` to `Z2Triple`, since BTG is a special case of MTG under the Z2 Field.
- Distributed Punctured PRF: In `mpc4j-s2pc-pcg`, we define a new protocol named Distributed Punctured PRF (DPPRF), and move all related implementations into DPRRF. This helps remove repeating codes when using DPPRF to implement subfield VOLE, including Silent OT and $GF(2^{\kappa})$-(sub)VOLE.

### Removed

- Single Sparse-Point COT (sspcot): We remove sspcot in our Silent OT implementation, since there is no usage in `mpc4j`. We recommend developers to use mspcot instead.
- Z2-VOLE: We find that Z2-VOLE is not secure and have no usage. We remove it from `mpc4j`.

### Fixed

- Ecc multiple init: We find a bug that if we first init the first native Ecc, then init the second native Ecc, and finally use the first one, an error would raise. This is because we call `native.init` in the constructor, and the later constructor would overlap the previous status. We fix this bug by refine our Ecc implementation.
- APSI: That there would be some unknown error when using `try_clear_irrelevant_bits` (provided by [the original APSI implementation](https://github.com/microsoft/APSI/blob/main/sender/apsi/bin_bundle.cpp)) to reduce communication costs. The error occurs with relative low probability, around 0.8% in total tries. We remove it from our APSI implementation to ensure 100% correctness.
- APSI: Add JNI memory release functions in `mpc4j-native-fhe/upsi/serialize.cpp`.

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