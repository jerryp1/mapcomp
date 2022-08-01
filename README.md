Multi-Party Computation for Java (`mpc4j`) is an efficient and easy-to-use multi-party computation (MPC) library mainly written in Java. 

# News

# Introduction 

The target of `mpc4j` is to provide an *academic library* for researchers to learn and develop MPC and related protocols in a unified manner. `mpc4j` provides Java APIs for basic operations used in MPC, such as communication, data-type conversions, commonly used cryptographic tools, so that researchers can use them to develop MPC protocols. `mpc4j` also tries to provide state-of-the-art MPC implementations. If researchers propose new MPC protocols, they can directly have an efficiency / communication comparisons with existing ones and obtain experimental reports.

## Features

`mpc4j` has the following features compared with existing open-source libraries:

- **Aarch64 support**: We develop `mpc4j` so that it can be run on x86_64 Ubuntu, x86_64 CentOS, and aarch64. Therefore, researchers can develop and test on M1-chip Macbook and then run experiments on Linux OS. Note that currently `mpc4j` does not support Windows. However, since all native libraries that `mpc4j` rely on support Windows, we believe `mpc4j` can support Windows with minor modification. We welcome Windows developers to make `mpc4j` successfully running on Windows.
- **SM series support**: Due to some reasons, developers would want to use SM series algorithms (SM2 for public-key operations, SM3 for hashing, and SM4 for block cipher operations) instead of regular algorithms (like secp256k1 for public key operations, SHA256 for hashing, and AES for block cipher operations). Also, the SM series algorithms are accepted by ISO/IES so that it may be necessary to support SM series algorithm under MPC settings. `mpc4j` leverages [Bouncy Castle](https://www.bouncycastle.org/java.html)

## Why Java?