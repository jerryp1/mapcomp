# About MapComp.

MapComp is a  secure collaborative analytics framework towards join group aggregation. MapComp is developed upon mpc4j (https://github.com/alibaba-edu/mpc4j). The protocols and building blocks of MapComp are itegregted as sub-modules in current project, which can be mainly found in mpc4j-s2pc-pjc, mpc4j-s2pc-groupagg, mpc4j-s2pc-opf, mpc4j-s2pc-aby, mpc4j-common-circuit.


# Requirements

The requirements for building and running project are the same with mpc4j, please refer https://github.com/alibaba-edu/mpc4j for further details.

# Running the protocols

MapComp contains mainly two sub-process: mapping and group-aggregation.

## Unit test

For unit test, you can use an IDE (e.g., IntelliJ IDEA) to import the source code and run unit tests. The unit tests of MapComp's main protocols can be found in:

- mpc4j-s2pc-groupagg/src/test/java/edu/alibaba/mpc4j/s2pc/groupagg/pto
- mpc4j-s2pc-pjc/src/test/java/edu/alibaba/mpc4j/s2pc/pjc

Unit tests of building blocks that designed in our protocol can also be performed at will.

## Benchmarks


For benchmark, please follow the instructions below.

### Mapping protocols:

-  Package this project with maven to generate runnable jar file.
- ``cd mpc4j-s2pc-pjc``
- Execute the following command in two terminals respectively:``java -Djava.library.path=/YOUR_MPC4J_ABSOLUTE_PATH/mpc4j-native-tool/cmake-build-release:/YOUR_MPC4J_ABSOLUTE_PATH/mpc4j-native-fhe/cmake-build-release -jar mpc4j-s2pc-pjc-1.1.0-SNAPSHOT-jar-with-dependencies.jar fileDir role``

    - `fileDir` is the directory of configure files. An example configure files can be seen in `./mpc4j-s2pc-pjc/src/test/resources/pmap`.

    - `role` needs to be replaced with `server` or `client`

### Group-aggregation protocols:

- Package this project with maven.

- ``cd mpc4j-s2pc-groupagg``

- ``mkdir result``

- Execute the following command in two terminals respectively:``java -Djava.library.path=/YOUR_MPC4J_ABSOLUTE_PATH/mpc4j-native-tool/cmake-build-release:/YOUR_MPC4J_ABSOLUTE_PATH/mpc4j-native-fhe/cmake-build-release -jar mpc4j-s2pc-pjc-1.1.0-SNAPSHOT-jar-with-dependencies.jar -s/-m PATH``

    - ``-s``  means run single test with a spcified configure file, which located in PATH

    - ``-m``  means run multiple tests with spcified configure files located in a directory PATH.

    - An example of configure is located in ```./mpc4j-s2pc-groupagg/src/test/resources/pmap``
