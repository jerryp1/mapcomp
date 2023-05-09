# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## \[1.0.8\]

### Added

- `mpc4j-common-circuit`
  - We abstract `MpcZlVector` and `MpcZlParty`.
- `mpc4j-s2pc-pcg`
  - We implement coin-tossing protocols with semi-honest and malicious security.
- `mpc4j-s2pc-opf`
  - We implement Naor-Ringold OPRF, and OPRP-based OPRF.
- `mpc4j-s2pc-pso`
  - We formalize and implement some PSI cardinality protocols, including EC-DH-based, DH-OPRF-based, and circuit-PSI-based.

### Fixed

- `mpc4j-common-tool`
  - We fixed a bug in CommitFactory. We need to create a commitment scheme with SHA256 hash for STANDARD and with SM3 hash for INLAND.
- `mpc4j-crypto-matrix`
  - We fixed a bug for `toString()` in vectors and databases. We need to correctly display the string even if the vector (the database) is empty (with num = 0).

## \[1.0.7\]

### Added

- `mpc4j-common-circuit`
  - We add a new module `mpc4j-common-circuit` to write all circuits in a unified manner.
  - We add some basic integer circuits: add, sub, increase one, equality (eq), less than or equal to (leq).
- `mpc4j-crypto-matrix`
  - We add a new module `mpc4j-crypto-matrix` to put functionalities related to cryptographic matrix operations.
  - We add some database / vector implementations.
- `mpc4j-common-rpc`
  - We add `receiveAny()` in Rpc.
  - We update the way of generating taskId. Now all sub-protocols have the same taskId with the root protocol. We distinguish sub-protocols using encodeTaskId. See `AbstractMultiPartyPto` for more details.
- `mpc4j-common-tool`
  - We add algebra operation interfaces in `galoisfiled`, including zl (Z mod (1 << l)), zl64 (Z mod (1 << l) where l < 64), zn (Z mod n), zn64 (Z mod n where n < (1 << 64)), zp (Z mod p where p is a prime), zp64 (Z mod p where p is a prime and p < (1 << 64)). 
  - We introduce FourQ ECC.
- `mpc4j-dp-service`
  - Now main supports more configurations: (1) Allow running without plain case; (2) Allow no/empty settings for α, ε_w, fo_types, hg_types.
  - Add necessary test cases for HhLdpMain.
- `mpc4j-s2pc-pcg`
  - We add HE-based and OT-based multiplication triple generation protocols introduced in the DSZ15 paper.
  - We add FHE-based multiplication triple generation protocol introduced in the RSS19 paper.
  - We implement pre-computed 1-out-of-n OTs based on the silent OT.
- `mpc4j-s2pc-aby`
  - We refine many implementations for Boolean circuits.
  - We implement mux operations introduced in RRK+20 and RRG+21 papers.
  - We implement Boolean circuit based PEQT protocol and the optimized PEQT protocol introduced in the CGS22 paper.
- `mpc4j-s2pc-pir`
  - We implement vector PIR introduced in the MR23 paper.
- `mpc4j-s2pc-opf`
  - We create a new module `mpc4j-s2pc-opf` for oblivious pseudo-random functions.
  - We implement programmable OPRFs based on OKVS introduced in the PSTY19 paper.
  - We implement related-batch programmable OPRFs introduced in the CGS22 paper.
  - We implement single-query OPRF introduced in the RA17 paper.
- `mpc4j-s2pc-pso`
  - We implement two circuit PSI protocols (without associated payload) introduced in the PRTY19 and CGS22 paper.

### Fixed

- `mpc4j-common-tool`
  - Fix a bug when switching the elliptic curve. In [Missing docs for c++ interface? #72](https://github.com/herumi/mcl/issues/72), the MCL author said "The current version does not support multi parameters. At first, I had developed the features, but I gave up it because a class dependency was very complicated." It brings some problems when we want to switch from an elliptic curve to another one that both use MCL. Now, we only allow users to use SEC_P256_K1 with MCL. 
- `mpc4j-dp-service`
  - Fix a bug for AppleHcmsFoLdp, we note that in Java, a % b (for b > 0) can have negative value. Therefore, we need to write Math.abs(a % b) instead of directly a % b to ensure a % b must be in \[0, b). Thank Xiaochen Li for the report.
  - Fix a bug for OLH and FLH, we note that $g$ in OLH and FLH must be an integer. Therefore, we cannot directly use the optimized frequency estimation formula to estimate the count. Instead, we use the original formula.
- `mpc4j-s2pc-pcg`
  - We slightly reduce the communication cost for distributed oblivious puncturable OPRF.
- `mpc4j-s2pc-aby`
  - Now we allow large BitNums per operations in the Boolean circuit.

## \[1.0.6\]

### Added

- `mpc4j-common-sampler`
    - We implement many discrete Gaussian sampling techniques, including native sampling, Alias sampling, sigma-2 sampling, convolution techniques, and discrete gaussian sampling introduced in NIPS 2020.
- `mpc4j-common-tool`
    - We implement metrics used for HeavyHitter (in `metrics/HeavyHitterMetrics.java`), including NDCG (Normalized
      Discounted Cumulative Gain), precision, and relative error.
    - We introduce a new tool named `BitVector` for efficient bit operations.
    - We add `MathPreconditions` for math precondition checks.
    - We implement the non-cryptographic hash function [BobHash](http://burtleburtle.net/bob/hash/evahash.html) and introduce [xxHash](https://github.com/lz4/lz4-java) in pure-Java.
- `mpc4j-dp-service`
    - We create a new module `mpc4j-dp-service` for implementing specific differential private mechanisms, e.g., Frequency Oracles.
    - We implement state-of-the-art LDP-based frequency oracle mechanisms, including Hadamard-related mechanisms, Unary Encoding (UE)-related mechanisms, Direct Encoding (DE)-related mechanisms, Local Hash (LH)-based mechanisms.
- `mpc4j-s2pc-pir`
    - We implement SealPIR, OnionPIR and FastPIR.
- `mpc4j-s2pc-pjc`
    - We create a new module `mpc4j-s2pc-pjc` to manage "Private Join and Compute" protocols, such as PSI-CA, PID, PMID,
      PSI-CA-SUM, and others.

### Changed

- common
  - Previously, we place our own `log4j.properties` in `resources`. However, this may reject developers to use its
    own `log4j.properties`. We replace all `log4j.properties` from `main/resources` to `test/resources`.
  - We optimize `LongUtils.ceilLog2` and some implementations in `BigIntegerUtils` based on Guava.
- `mpc4j-common-tool`
    - We rename package `correlation` to `metrics` so that we can include other metrics in that package.
    - We replace `RankUtils.java` with package `util`.
    - We optimize implementations for the Hadamard matrix and the Hadamard coder.
- `mpc4j-s2pc-pso`
    - We move blackIP data from module `mpc4j-s2pc-pso` to the dictionary `data`.
    - We move PID and PMID from module `mpc4j-s2pc-pso` to module `mpc4j-s2pc-pjc`.

### Fixed

- `mpc4j-common-tool`
  - We fixed a bug in `RandomCoderUtils.java`, thanks Qixian Zhou for reporting.

## \[1.0.5\]

### Added

- `mpc4j-common-tool`
    - Polynomial: We add batched polynomial implementation algorithms (both for Java and C/C++) introduced in the CRYPTO
      2019
      paper [SpOT-Light: Lightweight Private Set Intersection from Sparse OT Extension](https://eprint.iacr.org/2019/634)
      .
    - Ecc: We now support pure-Java [Ristretto](https://ristretto.group/) curve. We also support pure-Java Elliagtor
      encoding/decoding introduced in the CCS 2021
      paper [Compact and Malicious Private Set Intersection for Small Sets](https://eprint.iacr.org/2021/1159).
- `mpc4j-common-rpc`
    - We add the interface `PtoFactory` and make protocol factory classes implement `PtoFactory`.
    - We add `setEnvType()` into the interface `SecurePtoConfig`. All protocol config can support `setEnvType()` so that
      we can switch `EnvType.STANDARD` to others in a unified way.
- `mpc4j-native-fhe`
    - We merged all native tools in one utils class for all protocols.
- `mpc4j-s2pc-pcg`
    - Multiplication Triple in Zp64: Introduce Multiplication Triple Generation (MTG) under Zp64 in `mpc4j-s2pc-pcg`.
- `mpc4j-s2pc-pir`
    - Index PIR: We implemented XPIR proposed in the PETS 2022
      paper [XPIR : Private Information Retrieval for Everyone](https://petsymposium.org/2016/files/papers/XPIR___Private_Information_Retrieval_for_Everyone.pdf)
      .
- `mpc4j-s2pc-pso`
    - `psu`
        - Now `Main` supports unbalanced PSU inputs.
        - Now `Main` supports BlackIP tests, recommended by anonymous USENIX Security 2023 reviewers.

### Changed

- Documentations
    - We update documentations for how to install and run `mpc4j`. Now, the documentation contains installing `mpc4j` in
      Ubuntu and CentOS Docker images both for `aarch64` and `x86_64`.
- `mpc4j-common-tool`
    - We revise the code for `SparseBitMatrix`. Now the code is easier to understand.

### Fixed

- `mpc4j-common-rpc`
    - Fix issue \#5.
- `mpc4j-native-tool`
    - We thank anonymous USENIX Security 2023 Artifact Evaluation (AE) reviewers for many suggestions
      for `mpc4j-native-tool`. These suggestions help us fix many memory leakage problems. Also, the comments help us
      remove many duplicate codes. Specifically, we replace constant-size heap allocations (
      e.g., `auto *p = new uint8_t[]`) with stack allocations (e.g., `uint8_t p[]`). We fixed many memory leakage bugs
      in our C/C++ implementations.
    - We update `CMakeList.txt` so that one can successfully compile `mpc4j-native-tool` in Ubuntu and CentOS Docker
      images both for `aarch64` and `x86_64`.

## \[1.0.4\]

### Added

- `mpc4j-common-tool`
    - ByteEcc: Add scalar validation for X25519. Add libsodium support for both X25519 and Ed25519.
    - Kyber: Add post-quantum secure public key encryption scheme Kyber. The implementation is modified
      from [KyberJCK](https://github.com/fisherstevenk/kyberJCE).
- `mpc4j-s2pc-pcg`
    - Multiplication Triple in Zl: Introduce Multiplication Triple Generation (MTG) under Zl in `mpc4j-s2pc-pcg`.
    - Kyber Base-OT: Introduce Kyber Base-OT schemes.
- `mpc4j-s2pc-pso`
    - mqRPMT: Introduce mqRPMT.
    - Facebook PID: Introduce the Facebook PID scheme based on X25519.
    - PSI: Introduce EC-DH-PSI and KKRT16-PSI.

### Changed

- `mpc4j-s2pc-pcg`
    - $2^l$-out-of-1 homomorphic oblivious transfer: We change $2^l$-out-of-1 homomorphic oblivious transfer to core
      $2^l$-out-of-1 oblivious transfer. In this way, $2^l$-out-of-1 oblivious transfer implementations have the same
      style with 2-out-of-1 oblivious transfer implementations.

### Remove

- `mpc4j-common-tool`
    - `byte[]` -> `int[]`: More tests show that the ByteBuffer conversion is as fast as unsafe conversion. We remove the
      unsafe conversion method. Now, developer can use `mpc4j` on any JDK with version 1.8 or later (instead of only
      1.8).
- `mpc4j-s2pc-pcg`
    - n-out-of-1 oblivious transfer: We remove n-out-of-1 oblivious transfer since it seems useless in the current
      framework.

## \[1.0.3\]

### Added

- CHANGELOG: We add CHANGELOG.md to write any changes during our development.
- UNSAFE: We find that `byte[]` to `int[]` conversion dominates the cost for Silent OT. We
  add `unsafeByteArrayToIntArray` in `IntUtils`, and introduce such a method in our Silent OT implementation.
- Ecc in OpenSSL: Ecc now supports OpenSSL. This means that we now have C/C++ SM2 implementation in `mpc4j`.
- ByteEcc: We add `ByteMulEcc` and `ByteFullEcc` interface and its Ed25519 and X25519 implementations. The performance
  report shows that Ed25519 and X25519 are more efficient than the standard Ecc implementations but with some
  limitations. For example, X25519 only supports multiplication with specific scalars.
- PropertiesUtils: We add `PropertiesUtils` in `mpc4j-common-tool` for ease of using `Properties`. In addition, we
  refine `main` in `mpc4j-s2pc-pso` and `mpc4j-sml-opboost`.

### Changed

- Fixed-Point Multiplication in ECC: In `mpc4j-common-tool`, we introduce the Window Method for ECC Fixed-Point
  Multiplication implemented in MCL into our pure-Java implementation, replacing the pre-computation techniques provided
  by Bouncy Castle. The efficiency results show that our new implementation is about 10x faster than the original one.
- Multiplication Triple: In `mpc4j-s2pc-pcg`, we merge Boolean Triple Generation (BTG) packages into Multiplication
  Triple Generation (MTG) packages and rename `booleanTriple` to `Z2Triple`, since BTG is a special case of MTG under
  the Z2 Field.
- Distributed Punctured PRF: In `mpc4j-s2pc-pcg`, we define a new protocol named Distributed Punctured PRF (DPPRF), and
  move all related implementations into DPRRF. This helps remove repeating codes when using DPPRF to implement subfield
  VOLE, including Silent OT and $GF(2^{\kappa})$-(sub)VOLE.
- PMID in `mpc4j-s2pc-pso` supports multiset inputs for both parties. We further refine implementations for PMID
  protocols.

### Removed

- Single Sparse-Point COT (`sspcot`): We remove `sspcot` in our Silent OT implementation since there is no usage
  in `mpc4j`. We recommend developers use `mspcot` instead.
- Z2-VOLE: We find that Z2-VOLE is not secure and has no usage. We remove it from `mpc4j`.

### Fixed

- Ecc multiple init: We find a bug that if we first init the first native Ecc, then init the second native Ecc, and
  finally use the first one, an error would arise. This is because we call `native.init` in the constructor, and the
  later constructor would overlap the previous status. We fix this bug by refining our ECC implementation.
- APSI: There would be some unknown error when using `try_clear_irrelevant_bits` (provided
  by [the original APSI implementation](https://github.com/microsoft/APSI/blob/main/sender/apsi/bin_bundle.cpp)) to
  reduce communication costs. The error occurs with relatively low probability, around 0.8% in total tries. We remove it
  from our APSI implementation to ensure 100% correctness.
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