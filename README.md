# About MapComp.


MapComp is a secure view-based collaborative analytics framework for join group aggregation. MapComp is developed upon mpc4j (https://github.com/alibaba-edu/mpc4j). The protocols and building blocks of MapComp are integrated as sub-modules in the current project, which can be mainly found in mpc4j-s2pc-pjc, mpc4j-s2pc-groupagg, mpc4j-s2pc-opf, mpc4j-s2pc-aby, mpc4j-common-circuit.

# Requirements

The requirements for building and running project are the same with mpc4j, please refer https://github.com/alibaba-edu/mpc4j for further details.

Note: it is required to build native-tools (in the module named mpc4j-native-tool) first to run any unit tests or benchmarks. The build process can be found at https://github.com/alibaba-edu/mpc4j/tree/main/mpc4j-native-tool/README.md.

# How to run our protocols

MapComp contains mainly two sub-process: view operations and group-aggregation.

## Unit tests

For unit tests, you can use an IDE (e.g., IntelliJ IDEA) to import the source code and run unit tests. The unit tests of MapComp's main protocols can be found in: 

- mpc4j-s2pc-groupagg/src/test/java/edu/alibaba/mpc4j/s2pc/groupagg/pto
- mpc4j-s2pc-pjc/src/test/java/edu/alibaba/mpc4j/s2pc/pjc

Unit tests of building blocks that are designed in our protocol can also be performed at will.

Note: The path of native-tool MUST be set as running parameters. An example is:
``-ea -Djava.library.path=/YOUR_MPC4J_ABSOLUTE_PATH/mpc4j-native-tool/cmake-build-release:/YOUR_MPC4J_ABSOLUTE_PATH/mpc4j-native-fhe/cmake-build-release``

## Benchmarks

For benchmarks, please follow the instructions below.

### View operation protocols:

-  Package this project with maven to generate a runnable jar file.
- ``cd mpc4j-s2pc-pjc``
- Execute the following command in two terminals respectively:``java -Djava.library.path=/YOUR_MPC4J_ABSOLUTE_PATH/mpc4j-native-tool/cmake-build-release:/YOUR_MPC4J_ABSOLUTE_PATH/mpc4j-native-fhe/cmake-build-release -jar mpc4j-s2pc-pjc-1.1.0-SNAPSHOT-jar-with-dependencies.jar fileDir role``

  - `fileDir` is the directory of configure files. An example of configure files can be seen in `./mpc4j-s2pc-pjc/src/test/resources/pmap`.

  - `role` needs to be replaced with `server` or `client`

  - An example of configure is located in ```./mpc4j-s2pc-groupagg/src/test/resources/view``

### Group-aggregation protocols:

- Package this project with maven.

- ``cd mpc4j-s2pc-groupagg``

- ``mkdir result``

- Execute the following command in two terminals respectively:``java -Djava.library.path=/YOUR_MPC4J_ABSOLUTE_PATH/mpc4j-native-tool/cmake-build-release:/YOUR_MPC4J_ABSOLUTE_PATH/mpc4j-native-fhe/cmake-build-release -jar mpc4j-s2pc-pjc-1.1.0-SNAPSHOT-jar-with-dependencies.jar -s/-m PATH``

  - ``-s``  means run single test with a spcified configure file, which located in PATH

  - ``-m``  means run multiple tests with spcified configure files located in a directory PATH.

  - An example of configure is located in ```./mpc4j-s2pc-groupagg/src/test/resources/view``
