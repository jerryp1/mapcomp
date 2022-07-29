## 椭圆曲线工具类`Ecc`

`mpc4j-common-tool`封装了椭圆曲线的相关操作，只需要选择相应的椭圆曲线类型，即可切换不同的实现。`Ecc`是线程安全的，可以并发调用。

### 支持的椭圆曲线类型

- `MCL_SEC_P256_K1`：应用JNI技术调用C/C++层的MCL库实现`SecP256k1`椭圆曲线。
- `BC_SEC_P256_K1`：应用Bouncy Castle库实现的`SecP256k1`椭圆曲线。
- `BC_CURVE_25519`：应用Bouncy Castle库实现的`Curve25519`椭圆曲线。
- `BC_SM2_P256_V1`：应用Bouncy Castle库实现的`Sm2P256v1`椭圆曲线。

### 样例代码

`Ecc`封装了哈希到椭圆曲线点（HashToCurve）、乘法（multiply）、固定基预计算（Fixed-Base Precompute）等接口。 经过测试，`Java`层实现的加法、减法、逆运算效率已经足够高，使用JNI反而会降低效率。因此，`Ecc`未封装此类运算。

样例代码如下。

```
// 选择椭圆曲线类型
EccType eccType = EccType.MCL_SEC_P256_K1;
// 创建Ecc实例
Ecc ecc = EccFactory.createInstance(eccType);
// HashToCurve
int messageByteLength = 100;
byte[] message = new byte[messageByteLength];
ECPoint hash = ecc.hashToCurve(message);
// 生成随机幂指数
SecureRandom secureRandom = new SecureRandom();
BigInteger r = ecc.randomZn(secureRandom);
// 幂指数求逆，可以使用r.modInverse(ecc.getN())，但推荐使用下述方法，通过JNA-GMP加速运算
BigInteger rInv = BigIntegerUtils.modInverse(r, ecc.getN());
// 获得生成元
ECPoint g = ecc.getG();
// 乘法
ECPoint gr = ecc.multiply(g, r);
// 固定基（Fixed-Base）预计算
ecc.precompute(g);
// 如果调用预计算，再执行乘法时会使用预计算乘法
gr = ecc.multiply(g, r);
// 销毁预计算
ecc.destroyPrecompute(g);
```