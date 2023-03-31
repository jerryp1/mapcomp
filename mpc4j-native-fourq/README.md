<!--
 * @Description: 
 * @Author: Qixian Zhou
 * @Date: 2023-03-30 22:20:29
-->
### Complie FourQlib

Here we provide the compilation method for FourQlib. Note that in order to be able to compile on different platforms, we provide CMakeLists.txt, and modify `#include<malloc.h>` in the `FourQ_64bit_and_portable/schnorrq.c` file to `#include<stdlib.h>`.

Please compile according to the following method. After verification, the following compilation method can be used for both MacOS (x64 and Arm) and Linux:

```
cd FourQ_64bit_and_portable
mkdir build
cd build
cmake .. 
make 
sudo make install
```