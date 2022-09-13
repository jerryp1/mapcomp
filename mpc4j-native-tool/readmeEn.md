# `mpc4j-native-tool`

`mpc4j`uses`JNI`programming interface to improve the performance of polynomial,big integer,elliptic curve and other algebraic operations.The c/c++ code used by `mpc4j` has been organized in the `mpc4j-native-tool`.In order to support cross platform operation as much as possible, `mpc4j` also provides users with alternative versions of Java and supports arbitrary data formats. However, the process of installing the infrastructure code is very cumbersome. This document describes the whole installation process, which is successfully verified on Mac, Ubuntu and CentOS.

The native C/C++ libraries that `mpc4j-native-tool` depends on are:

- [GMP](https://gmplib.org/)：The high-performance C/C++ big integer operation library.`Modpow` and `modinverse` in `mpc4j` are all accelerated by [jna-gmp](https://github.com/square/jna-gmp). Meanwhile other dependent native C/C++ libraries also depends on `GMP`
- [GF2X](https://gitlab.inria.fr/gf2x/gf2x)：The high-performance Galois field (GF) calculation library provides a more efficient implementation of polynomial operations.
- [NTL](https://libntl.org/)：The high-performance number theory library implemented by cryptologists famous cryptographer [Victor Shoup](https://shoup.net/)，Almost all C/C++ implementations of cryptographic schemes use NTL.Moreover, NTL supports the use of gf2x to replace its own polynomial operation library.`mpc4j-native-tool` only calls the interface of NTL and does not call the interface of gf2x. Therefore, even if the gf2x library cannot be successfully built, as long as the NTL library built successfully, users can also use the C/C++ acceleration function in `mpc4j-native-tool`.
- [MCL](https://github.com/herumi/mcl)：The high-performance elliptic curve operation library, which applies different instruction sets on different platforms to realize operation acceleration.Through our experimental test,the performance of MCL is better than the performance of famous elliptic curve library [Relic](https://github.com/relic-toolkit/relic).

## GMP

### X86 MAC

You can directly use `homebrew` to install GMP on Mac.

```shell
brew install gmp
```

You can also install through the source code. The installation method is the same as that on Ubuntu.

### Ubuntu & CentOS

Ubuntu and CentOS can be installed through source code. If you can open the browser, you can directly visit  [GMP official website](https://gmplib.org/) to download the source code. If you can only use the terminal, you can use the following commands to download the source code:


```shell
wget https://gmplib.org/download/gmp/gmp-x.x.x.tar.xz
```

The downloaded source code is in the format of `gmp-x.x.x.tar.xz`，where `gmp-x.x.x` is the name of the compressed package. You can execute the following commands to complete the decompression and enter the directory.

```shell
xz -d gmp-x.x.x.tar.xz
tar -xvf gmp-x.x.x.tar
cd gmp-x.x.x/
```

You can execute the following commands to complete the installation。

```shell
./configure
make
make check
sudo make install
```

- If you get the `gcc:command not found` error after installation，you need to install `gcc`.
  - Ubuntu installation method：`sudo apt install gcc`.
  - CentOS installation method：`sudo yum install gcc`.
- If you get the `m4:command not found` error after installation，you need to install `m4`.
  - Ubuntu installation method：`sudo apt-get install m4`.
  - CentOS installation method：`sudo yum install m4`.
- If you get the `make:command not found` error after installation，you need to install `make`.
  - Ubuntu installation method：`sudo apt install make`.
  - CentOS installation method：`sudo yum install make`.

After installation, you can go back to the initial directory:

```shell
cd ..
```

### M1 MAC

On M1 MAC environment, you cannot directly use `brew install GMP` or install GMP of version 6.2.0 or earlier. This is because until version 6.2.0, GMP used macos-x86-64, but the later installation of MCL requires GMP of arm64. Therefore, you need to install GMP version 6.2.1 or later.
In addition, the C++ version of GMP will be used by default when MCL is subsequently installed, but NTL will use the C version of GMP. Therefore, both C and C++ versions of GMP need to be installed at the same time. The installation method of the C version of GMP is:

```shell
./configure
make
make check
sudo make install
```

The installation method of the C++ version of GMP is:

```shell
./configure --enable-cxx
make
make check
sudo make install
```

After installation, both `libgmp.dylib` and `libgmpxx.dylib` will exist in the directory `/usr/local/lib`.

You can go back to the initial directory:

```shell
cd ..
```

### uninstall GMP

After installation, please do not delete the GMP package. This is because if terminal prompts that the GMP version is incorrect during the NTL installation later, you need to uninstall the current GMP and install the correct version. The command to uninstall GMP on the GMP package is:

```shell
sudo make uninstall
```

After our testing, NTL under Ubuntu only supports[GMP-6.2.0](https://gmplib.org/download/gmp/gmp-6.2.0.tar.xz)， NTL under CentOS only supports [GMP-6.0.0](https://gmplib.org/download/gmp/gmp-6.0.0.tar.xz).After uninstalling the wrong version of GMP, you can follow the link to download and install the old version of GMP.

## GF2X

Gf2x can only be installed through source code. The installation methods on x86 Mac, M1 Mac, Ubuntu and CentOS are the same. First, execute the following command to download the gf2x source code.

```shell
git clone https://gitlab.inria.fr/gf2x/gf2x.git
```

- If you get the `git:command not found` error after installation，you need to install `git`.
  - MAC support `git` instruction. According to our test results, there is no need to install it.
  - Ubuntu installation method：`sudo apt install git`。
  - CentOS installation method：`sudo yum install git`。

### Official document error

The **Instruction to install the package** in [GF2X Official document](https://gitlab.inria.fr/gf2x/gf2x) is the standard UNIX installation method, namely:：

```shell
./configure && make
make install
```

However, gf2x cannot be successfully installed with this method. The correct way to install gf2x is to use the `automake` tool.

### Mac installation scheme of `automake`

Because the gf2x code is really early, the 'automake' installed in the MAC environment by 'brew' does not support the old syntax in gf2x, and you need to use the old version of the 'automake' tool.

Execute the following commands to download and decompress [autoconf-2.69](https://mirrors.nju.edu.cn/gnu/autoconf/autoconf-2.69.tar.gz)，and switch to the directory.

```shell
wget https://mirrors.nju.edu.cn/gnu/autoconf/autoconf-2.69.tar.gz
tar -xvzf autoconf-2.69.tar.gz
cd autoconf-2.69/
```

Execute the following command to install `autoconf`：

```shell
./configure
make
make install
```

After installation, return to the upper directory：

```shell
cd ..
```

Execute the following commands to download and decompress [automake-1.15](https://mirrors.aliyun.com/gnu/automake/automake-1.15.tar.gz)，and switch to the directory.

```shell
wget https://mirrors.aliyun.com/gnu/automake/automake-1.15.tar.gz
tar -xvzf automake-1.15.tar.gz
cd automake-1.15/
```

Execute the following command to install `automake`：

```shell

./configure
make
make install
```

After installation, return to the upper directory：

```shell
cd ..
```

Execute the following command to install `libtool`.

```shell
brew install libtool
```

### Ubuntu installation scheme of `automake`

Execute the following command to install `autoconf`、`automake` and `libtool`.

```shell
sudo apt-get install autoconf
sudo apt-get install automake
sudo apt-get install libtool
```

### CentOS installation scheme of `automake`

Execute the following command to install `autoconf`、`automake` and `libtool`.

```shell
sudo yum install autoconf
sudo yum install automake
sudo yum install libtool
```

### Generate configuration items

Afrer `automake` is installed,switch to the `gf2x` directory, and execute the following commands（reference links：[《Install》](https://blog.csdn.net/LEE_FIGHTING_JINGYU/article/details/105754580)）.

```shell
aclocal
```

If the execution environment is X86 MAC、M1 MAC，and execute the following commands（reference links：[《Installed libtool but libtoolize not found》](https://stackoverflow.com/questions/15448582/installed-libtool-but-libtoolize-not-found)）.

```shell
glibtoolize --force
```

If the execution environment is Ubuntu、CentOS，and execute the following commands.

```shell
libtoolize --force
```

### Compilation and installation

Execute the following commands on all platforms to generate header files and `configure` files.

```shell
autoheader
automake --add-missing
autoconf
./configure --disable-dependency-tracking
```

Then, execute the following command to install GF2X：

```shell
make
make tune-lowlevel
make tune-toom
make tune-fft # 这个命令可能会报错，不影响最后结果
make check
make install
```

After installation, go back to the upper directory：

```shell
cd ..
```

## NTL

### The installation of NTL

From [Download page of NTL official website](https://libntl.org/download.html) to download the latest NTL source code. If you can only use the command line, you can use the following commands to download the NTL source code:：

```shell
wget https://libntl.org/ntl-11.5.1.tar.gz
```

Thus, a compressed package in the format `ntl-XX.X.X.tar.gz`is obtained, you can implement through the following commands:

```shell
tar -xvzf ntl-XX.X.X.tar.gz
```

Decompress the compressed package，and enter to the `ntl-XX.X.X` directory. You can execute the following command to install NTL.

```shell
cd src
./configure NTL_GF2X_LIB=on SHARED=on CXXFLAGS=-O3 # NTL needs to be compiled into a shared library, because it is necessary to use a shared library when using JNI under Ubuntu.
make
make check
sudo make install
```

If GF2X is not installed successfully，you can configure NTL is not linker to GF2X，then you can execute the following command to install NTL.

```shell
cd src
./configure SHARED=on CXXFLAGS=-O3 # NTL needs to be compiled into a shared library, because it is necessary to use a shared library when using JNI under Ubuntu.
make
make check
sudo make install
```

On Ubuntu, if 'make check' fails ,that is because NTL cannot find GMP, you need to add `/usr/local/lib` to the directory. The specific method is:

- execute the following command  `sudo vim /etc/ld.so.conf`，open `ldconfig` configuration file.
- Add a line to the last side of the file `/usr/local/lib`. 
- execute `/sbin/ldconfig`.

Re execute `make check`，you would be able to complete the correctness check.

If `make` execution failed，you need to install `g++`。
- `g++` On is installed by default on the MAC. Generally, it does not need to be reinstalled.
- On Ubuntu, the installation command is ：`sudo apt-get install g++`。
- On CentOS, the installation command is：`sudo yum install gcc-c++`。

After installation, go back to the upper directory：

```shell
cd ../..
```

### Uninstall NTL

If you want to reinstall NTL, you need to uninstall the previous NTL and execute the following instructions.

```shell
cd src
sudo make -n install
```

## MCL

### Download MCL source code

The MCL version we use is [v1.61](https://github.com/herumi/mcl/releases/tag/v1.61). If you find that the new version in the future cannot be used, please contact us. Execute the following command to download the MCL source code.

```shell
git clone -b v1.61 https://github.com/herumi/mcl.git
```

### Ubuntu & CentOS & X86 MAC installation method

You can install MCL through the following commands.

```shell
cd mcl
mkdir build
cd build
cmake ..
make
make install
```
If `cmake` execution failed，then you need to install `cmake`。

Installation method on Ubuntu is relatively simple, you can execute directly through `sudo apt install cmake`.

CentOS installation method is special. If you use `sudo yum install cmake` to install，you would get the 2.8 version of `cmake`，which can not be used in `mcl`. The new version of mcl needs to be installed through source code. The specific installation methods are as follows.

```shell
# If a lower version is installed, this command can uninstall the current version of cmake.
sudo yum remove cmake -y 
# Get the latest release of cmake
wget https://github.com/Kitware/CMake/releases/download/vX.XX.X/cmake-X.XX.X.tar.gz
tar -zxvf cmake-X.XX.X.tar.gz
cd cmake-X.XX.X
./bootstrap
make
make install
\cp -f ./bin/cmake ./bin/cpack ./bin/ctest /bin
```
If you failed at the `./bootstrap` step，then you need to install the `openssl` development kit ，then execute the `sudo yum install openssl-devel`.After installation,re execute `./bootstrap`.

If `cmake` return can not find GMP ，then you need to reinstall GMP development kit.
- The installation method on Ubuntu：`sudo apt install libgmp-dev`.
- The installation method on CentOS：`sudo yum install gmp-devel`.

After installation, go back to the upper directory：

```shell
cd ~
```

### M1 MAC installation method

Until now, MCL has not optimized assembly for arm64 MacOS, so you need to remove assembly during compilation, you can execute the following command:

```shell
cd mcl
mkdir build
cd build
cmake .. -DMCL_USE_ASM=OFF
make
make install
```

If `cmake` execute failed，then you need to install `cmake`,the installation method is similar to CentOS scheme.

After installation, go back to the upper directory：

```shell
cd ~
```

### Other instructions

If you find that the path cannot be found during installation, you can execute the instructions in `build` directory:

```shell
cmake .. -LA . # Notice that there is a point at the end of the command
```

Check whether the corresponding dynamic library can be found（refer to MCL document [readme.md](https://github.com/herumi/mcl) Options section）.

## Install`Java` and set `JAVA_HOME`

You need to set `JAVA_HOME` variable before installation.

### Installation method on CentOS

If you have not installed `Java`, execute the following command:


```shell 
yum search java
```

We usually install `JDK` and execute the following instructions（reference document [《how to yum install jdk on CentOS》](https://segmentfault.com/a/1190000015389941)）：

```shell 
sudo yum install java-1.8.0-openjdk-devel.x86_64
```

After installation of `JDK`, check the location of `JDK` and execute the following instructions:

```shell 
which java                      # It returns /usr/bin/java
ls -lrt /usr/bin/java           # It returns /usr/bin/java -> /etc/alternatives/java
ls -lrt /etc/alternatives/java  # It returns /etc/alternatives/java -> /usr/lib/jvm/java-1.8.0-openjdk-1.8.0.302.b08-0.el7_9.x86_64/jre/bin/java
```

Which means `java` is located in `/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.302.b08-0.el7_9.x86_64/jre/bin/java`. execute the following commands，to open `~/.bash_profile`：

```shell
vim ~/.bash_profile
```

Add the following commands：

```shell
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.302.b08-0.el7_9.x86_64
```

After saving and exiting, execute the following command（reference paper：[《configuring JAVA_HOME on CentOS》](https://blog.csdn.net/zzpzheng/article/details/73613838)）：

```shell
source ~/.bash_profile
```

## compile `encap-native-tool`

After all this preparations above, you can compile`encap-native-tool`。

### Installation method on MAC

Open the terminal and enter the `encap-native-tool` directory，to execute the following commands，you can generate `libnct` library on `cmake-build-release` directory.

```shell
mkdir cmake-build-release
cd cmake-build-release
cmake ..
make
```

### Installation method on CentOS

Install `openssl`，and install the development kit：

```shell
sudo yum install openssl
sudo yum install openssl-devel
```

Calling AES instruction set requires using CentOS instruction set algorithm package. The installation method is:
```shell 
sudo yum install centos-release-scl
sudo yum install devtoolset-8
```

> references paper ：[《Cannot compile from source on Centos7》](https://discuss.zerotier.com/t/cannot-compile-from-source-on-centos7/842)

After the installation of above tools ，you can generate `libent` library through the following instructions.

```shell
mkdir cmake-build-release
cd cmake-build-release
cmake ..
make
```

### Installation method on Ubuntu

before installation, you need to set `gcc` and `g++` before cmake, the instruction  are as follows:

```shell
export CC=/usr/bin/gcc
export CXX=/usr/bin/g++
```

After setting `gcc` and `g++`，we can generate `libent`library through the following commands:

```shell
mkdir cmake-build-release
cd cmake-build-release
cmake ..
make
```

## Configuring the test environment on Intelli IDEA

After generate `libnct` library，we need to configure development environment on Intellij IDEA to facilitate testing. Otherwise, Intellij IDEA would return can not find `libent`.

1. Open `Run->Edit Configurations...`.
2. Add `-Djava.library.path=XXX` after `VMOptions`，and `XXX` means the directory of `libent`.

If you want to introduce `libent` under the main function，you need to add `java.library.path` to the execution directory，then add `-Djava.library.path=xxx` jar -jar，and `XXX` means the **absolute path to the directory of `libent`**. 
