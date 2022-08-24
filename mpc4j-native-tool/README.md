# `mpc4j-native-tool`安装与配置

`mpc4j`使用`JNI`技术提高多项式、大整数、椭圆曲线等代数运算的性能。本地库所使用的C/C++代码已组织在`mpc4j-native-tool`本地库中。为尽可能支持跨平台运行，`mpc4j`也为用户提供了`Java`的替代版本，并完全打通数据格式。然而，安装底层代码的过程非常繁琐。本文档描述了整个安装过程，安装过程在MAC、Ubuntu、CentOS下验证成功。

`mpc4j-native-tool`依赖的底层C/C++库有：

- [GMP](https://gmplib.org/)：高性能C/C++大整数运算库。`mpc4j`中的大整数模幂运算（`modPow`）、模拟运算（`modInverse`）通过应用[jna-gmp](https://github.com/square/jna-gmp)实现运算加速。其他依赖的底层C/C++库也基本都会用到`GMP`。
- [GF2X](https://gitlab.inria.fr/gf2x/gf2x)：高性能伽罗华域（Galois Field，GF）计算库，提供了更高效的多项式运算实现。
- [NTL](https://libntl.org/)：著名密码学家[Victor Shoup](https://shoup.net/)实现的高性能数论库，几乎所有密码学方案的C/C++实现都会用到NTL。NTL支持使用GF2X替代其本身自带的多项式运算库。`mpc4j-native-tool`只调用NTL的接口，未使用GF2X的接口，因此即使未能成功构建GF2X库，只要能成功构建NTL库，则也可以享受`mpc4j-native-tool`的C/C++加速功能。
- [MCL](https://github.com/herumi/mcl)：高性能椭圆曲线运算库，在不同平台上应用不同的指令集实现运算加速。经过测试，MCL的性能比著名椭圆曲线库[Relic](https://github.com/relic-toolkit/relic)的性能还要好。

## GMP

### X86 MAC

MAC下可以直接使用`homebrew`安装GMP。

```shell
brew install gmp
```

也可以通过源代码安装，安装方法和Ubuntu一致。

### Ubuntu & CentOS

Ubuntu和CentOS可以通过源代码安装。如果可以打开浏览器，则可以直接访问[GMP官方网站](https://gmplib.org/)下载得到源代码。如果只能使用命令行，可以使用下述命令下载源代码：

```shell
wget https://gmplib.org/download/gmp/gmp-x.x.x.tar.xz
```

下载得到的源代码为`gmp-x.x.x.tar.xz`格式，其中`gmp-x.x.x`为压缩包名称，需要依次执行下述命令完成解压，并切换到目录下。

```shell
xz -d gmp-x.x.x.tar.xz
tar -xvf gmp-x.x.x.tar
cd gmp-x.x.x/
```

执行下述命令完成安装。

```shell
./configure
make
make check
sudo make install
```

- 如果提示`gcc`命令不存在，则需要安装`gcc`。
  - Ubuntu安装方法：`sudo apt install gcc`。
  - CentOS安装方法：`sudo yum install gcc`。
- 如果提示`m4`命令不存在，则需要安装`m4`。
  - Ubuntu安装方法：`sudo apt-get install m4`。
  - CentOS安装方法：`sudo yum install m4`。
- 如果提示`make`命令不存在，则需要安装`make`。
  - Ubuntu安装方法：`sudo apt install make`。
  - CentOS安装方法：`sudo yum install make`。

安装完毕后，回退到上层目录：

```shell
cd ..
```

### M1 MAC

M1 Mac环境不能直接使用`brew install gmp`，也不能安装6.2.0及以前版本的GMP，这是因为6.2.0版本GMP使用的是macOS-x86-64，但后面的MCL需要arm64的GMP。因此，需要源代码层面安装6.2.1版本的GMP。另外，后续安装MCL时默认会使用GMP的C++版本，但NTL会使用GMP的C版本。因此，需要同时安装C和C++版本的GMP。C版本GMP的安装方法为：

```shell
./configure
make
make check
sudo make install
```

C++版本GMP的安装方法为：

```shell
./configure --enable-cxx
make
make check
sudo make install
```

安装后，`/usr/local/lib`下面会同时存在`libgmp.dylib`和`libgmpxx.dylib`。

### 卸载GMP

安装完毕后，请不要删除GMP源码包。这是因为如果NTL安装时提示GMP版本不正确，则需要卸载当前的GMP并安装正确的版本。在GMP源码包上卸载GMP的命令为：

```shell
sudo make uninstall
```

经过测试，Ubuntu下的NTL只支持[GMP-6.2.0](https://gmplib.org/download/gmp/gmp-6.2.0.tar.xz)，CentOS下的NTL只支持[GMP-6.0.0](https://gmplib.org/download/gmp/gmp-6.0.0.tar.xz)。卸载已有GMP后，按照链接下载旧版本的GMP并安装。

## GF2X

只能通过源代码方式安装GF2X，X86 MAC、M1 MAC、Ubuntu、CentOS下的安装方法一致。首先，执行下述命令下载GF2X源代码。

```shell
git clone https://gitlab.inria.fr/gf2x/gf2x.git
```

- 如果提示`git`命令不存在，则需要安装`git`。
  - MAC自带`git`指令，测试结果看不需要单独安装。
  - Ubuntu安装方法：`sudo apt install git`。
  - CentOS安装方法：`sudo yum install git`。

### 官方方法错误

[GF2X官方文档](https://gitlab.inria.fr/gf2x/gf2x)的**Instruction to install the package**给出的是标准UNIX安装方式，即：

```shell
./configure && make
make install
```

然而，使用这一方法是无法成功安装GF2X的。GF2X的正确安装方法是使用`automake`工具。

### MAC安装`automake`

由于GF2X代码已经比较陈旧，MAC环境`brew`安装的`automake`已经不支持GF2X中古老的语法，需要使用旧版本的`automake`工具。

执行下述命令，下载、解压[autoconf-2.69](https://mirrors.nju.edu.cn/gnu/autoconf/autoconf-2.69.tar.gz)，并切换到目录。

```shell
wget https://mirrors.nju.edu.cn/gnu/autoconf/autoconf-2.69.tar.gz
tar -xvzf autoconf-2.69.tar.gz
cd autoconf-2.69/
```

执行下述命令安装`autoconf`：

```shell
./configure
make
make install
```

执行完毕后，退回到上层目录：

```shell
cd ..
```

执行下述命令，下载、解压[automake-1.15](https://mirrors.aliyun.com/gnu/automake/automake-1.15.tar.gz)，并切换到目录。

```shell
wget https://mirrors.aliyun.com/gnu/automake/automake-1.15.tar.gz
tar -xvzf automake-1.15.tar.gz
cd automake-1.15/
```

执行下述命令安装`automake`：

```shell

./configure
make
make install
```

执行完毕后，退回到上层目录：

```shell
cd ..
```

执行下述命令，安装`libtool`。

```shell
brew install libtool
```

### Ubuntu安装`automake`

执行下述命令，安装`autoconf`、`automake`和`libtool`。

```shell
sudo apt-get install autoconf
sudo apt-get install automake
sudo apt-get install libtool
```

### CentOS安装`automake`

执行下述命令，安装`autoconf`、`automake`和`libtool`。

```shell
sudo yum install autoconf
sudo yum install automake
sudo yum install libtool
```

### 生成配置项

`automake`安装完毕后，切换到`gf2x`目录下，执行下述命令（参考链接：[《Install》](https://blog.csdn.net/LEE_FIGHTING_JINGYU/article/details/105754580)）。

```shell
aclocal
```

如果是X86 MAC、M1 MAC，执行下述命令（参考链接：[《Installed libtool but libtoolize not found》](https://stackoverflow.com/questions/15448582/installed-libtool-but-libtoolize-not-found)）。

```shell
glibtoolize --force
```

如果是Ubuntu、CentOS，执行下述命令。

```shell
libtoolize --force
```

### 编译和安装

任何平台均执行下述命令，生成生成头文件和`configure`文件。

```shell
autoheader
automake --add-missing
autoconf
./configure --disable-dependency-tracking
```

随后，执行下述命令，安装GF2X：

```shell
make
make tune-lowlevel
make tune-toom
make tune-fft # 这个命令可能会报错，不影响最后结果
make check
make install
```

## NTL

### 安装NTL

从[NTL官方网站的下载页面](https://libntl.org/download.html)下载最新的NTL源代码。如果只能使用命令行，可以使用下述命令下载源代码：

```shell
wget https://libntl.org/ntl-11.5.1.tar.gz
```

从而得到格式为`ntl-XX.X.X.tar.gz`的压缩包。执行

```shell
tar -xvzf ntl-XX.X.X.tar.gz
```

解压源代码，并进入到`ntl-XX.X.X`目录。执行下述命令安装NTL。

```shell
cd src
./configure NTL_GF2X_LIB=on SHARED=on CXXFLAGS=-O3 # 需把NTL编译成共享库，因为原因是Ubuntu下使用JNI必须使用共享库。
make
make check
sudo make install
```

如果GF2X没有安装成功，也可以配置NTL不关联GF2X，即施行下述命令安装NTL。

```shell
cd src
./configure SHARED=on CXXFLAGS=-O3 # 需把NTL编译成共享库，因为原因是Ubuntu下使用JNI必须使用共享库。
make
make check
sudo make install
```

Ubuntu下，如果`make check`失败，原因是NTL找不到GMP，需要将`/usr/local/lib`添加到目录中，具体方法为：

- 执行命令sudo vim /etc/ld.so.conf，打开`ldconfig`配置文件。 
- 在文件后面添加一行`/usr/local/lib`。 
- 执行`/sbin/ldconfig`。

重新执行`make check`，应该可以完成正确性检查。

如果`make`执行失败，需要安装`g++`。
- MAC下默认安装`g++`，一般不需要重新安装。
- Ubuntu安装方法：`sudo apt-get install g++`。
- CentOS安装方法：`sudo yum install gcc-c++`。

### 卸载

如果想重新安装NTL，需要卸载之前的NTL，执行下述指令即可。

```shell
cd src
sudo make -n install
```

## MCL

### 下载源代码

我们使用的MCL版本为[v1.61](https://github.com/herumi/mcl/releases/tag/v1.61)。如果发现未来新版本无法使用，还请联系我们。执行下述命令，下载MCL源代码。

```shell
git clone -b v1.61 https://github.com/herumi/mcl.git
```

### Ubuntu & CentOS & X86 MAC安装

执行下述命令安装MCL。

```shell
cd mcl
mkdir build
cd build
cmake ..
make
make install
```
如果`cmake`命令执行失败，则需要安装`cmake`。

Ubuntu安装方法比较简单，直接执行`sudo apt install cmake`。

CentOS安装比较特殊。如果使用`sudo yum install cmake`安装，得到的是2.8版本的`cmake`，无法用于编译`mcl`。需要通过源代码安装新版本。具体方法如下。

```shell
# 如果已安装低版本，则此命令可以卸载当前版本。
sudo yum remove cmake -y 
# 下载最新版本（Latest Release）的cmake
wget https://github.com/Kitware/CMake/releases/download/vX.XX.X/cmake-X.XX.X.tar.gz
tar -zxvf cmake-X.XX.X.tar.gz
cd cmake-X.XX.X
./bootstrap
make
make install
\cp -f ./bin/cmake ./bin/cpack ./bin/ctest /bin
```
如果`./bootstrap`执行失败，则需要安装`openssl`开发套件，即执行`sudo yum install openssl-devel`。安装完毕后重新执行`./bootstrap`。

如果`cmake`提示找不到GMP，则需要安装GMP的开发套件。
- Ubuntu安装方法：`sudo apt install libgmp-dev`。
- CentOS安装方法：`sudo yum install gmp-devel`。

### M1 MAC安装

目前MCL还没有针对Arm64 MacOS进行汇编优化，因此编译时需要移除汇编支持，即在mcl目录下执行下述命令：

```shell
mkdir build
cd build
cmake .. -DMCL_USE_ASM=OFF
make
make install
```

### 其他说明

如果在安装过程中发现路径找不到等问题，可在`build`目录下执行指令：

```shell
cmake .. -LA . # 注意最后方有个点
```

查看是否都能找到相应的动态库（参考MCL文档[readme.md](https://github.com/herumi/mcl)中的options部分）。

## 安装`Java`并设置`JAVA_HOME`

安装之前需要在环境变量里面设置`JAVA_HOME`。

### CentOS安装方法

如果尚未安装`Java`，则执行下述命令：

```shell 
yum search java
```

我们一般要安装`JDK`，执行下述指令（参考文章[《CentOS使用yum安装jdk》](https://segmentfault.com/a/1190000015389941)）：

```shell 
sudo yum install java-1.8.0-openjdk-devel.x86_64
```

安装好`JDK`后，查看`JDK`所在的位置，执行下述指令：

```shell 
which java                      # 结果显示为/usr/bin/java
ls -lrt /usr/bin/java           # 结果显示为/usr/bin/java -> /etc/alternatives/java
ls -lrt /etc/alternatives/java  # 结果显示为/etc/alternatives/java -> /usr/lib/jvm/java-1.8.0-openjdk-1.8.0.302.b08-0.el7_9.x86_64/jre/bin/java
```

这意味着`java`指令位于`/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.302.b08-0.el7_9.x86_64/jre/bin/java`。执行下述指令，打开`~/.bash_profile`：

```shell
vim ~/.bash_profile
```

在里面添加下述命令：

```shell
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.302.b08-0.el7_9.x86_64
```

保存退出后，执行命令（参考文章：[《Centos7配置JAVA_HOME》](https://blog.csdn.net/zzpzheng/article/details/73613838)）：

```shell
source ~/.bash_profile
```

## 编译`mpc4j-native-tool`

准备完毕后，即可编译`mpc4j-native-tool`。

### MAC安装

打开命令行进入到`mpc4j-native-tool`目录，执行下述步骤，即可在`cmake-build-release`目录下生成`libnct`函数库。

```shell
mkdir cmake-build-release
cd cmake-build-release
cmake ..
make
```

### CentOS安装

安装`openssl`，并安装开发套件：

```shell
sudo yum install openssl
sudo yum install openssl-devel
```

调用AES指令集需要使用CentOS的指令集算法包，安装方法为：

```shell 
sudo yum install centos-release-scl
sudo yum install devtoolset-8
```

> 参考文章：[《Cannot compile from source on Centos7》](https://discuss.zerotier.com/t/cannot-compile-from-source-on-centos7/842)

上述工具均包安装成功后，即可通过下述指令生成`libent`函数库。

```shell
mkdir cmake-build-release
cd cmake-build-release
cmake ..
make
```

### Ubuntu安装

安装前需要在cmake前指定`gcc`和`g++`，方法为：

```shell
export CC=/usr/bin/gcc
export CXX=/usr/bin/g++
```

指定好`gcc`和`g++`后，即可通过下述指令生成`libent`函数库。

```shell
mkdir cmake-build-release
cd cmake-build-release
cmake ..
make
```

## 在Intelli IDEA下配置测试环境

生成`libnct`函数库后，还需要配置Intellij IDEA的开发环境，以方便测试。否则，Intellij IDEA会提示找不到`libent`库。

1. 打开`Run->Edit Configurations...`。
2. 在`VMOptions`后面增加`-Djava.library.path=XXX`，其中`XXX`为`libent`所在目录。

如果想在main函数下引入`libent`，需要将`java.library.path`添加到执行目录中，即在jar -jar后方增加`-Djava.library.path=xxx`，其中`xxx`为`libent`的**绝对路径**。
