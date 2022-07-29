# JNA GMP with Arm64 Support

A minimal JNA wrapper around the [GNU Multiple Precision Arithmetic Library](http://gmplib.org/) ("libgmp") supporting the Arm64 operating system. 

The original JNA-GMP is provided by [JNA GMP project](https://github.com/square/jna-gmp). 
We modify the code for supporting the Arm64 operating system. The details are as follows:

- We separate `darwin-x86_64` and `darwin-aarch64` by renaming the dictionary name `darwin` in `resources` as `darwin-x86_64` and providing a pre-compiled libgmp for Arm64 operating system in `darwin-aarch64`.
- We found that all libgmp provided for other operating systems are in version 6.1.1. However, we cannot successfully compile libgmp 6.1.1 on the Arm64 operating system with the error `Oops, mp_limb_t is 64 bits, but the assembler code`. We tried and successfully compiled libgmp 6.2.1. Therefore, the libgmp in `darwin-aarch64` is in version 6.2.1, while others are in version 6.1.1. See `LibGmpTest.testVersion()` for more details.

## Features

### modPow

`modPow` is the critical operation in many public-key cryptography primitives. Libgmp's native implementation is *significantly* faster and less CPU intensive than Java's implementation.  Typical performance improvement would be on the order of 5x faster than Java.

- Use `modPowSecure` for crypto. It's slower, but resistant to timing attacks.
- Use `modPowInsecure` for non-crypto, it's faster.

### modInverse

A faster version of BigInteger.modInverse().

### kronecker (jacobi, legendre)

The GMP kronecker implementation generalizes jacobi and legendre symbols.

### exactDivide

A *very* fast way to perform `dividend / divisor` *if and only if* `dividend % divisor == 0`. That is, if the `dividend` is a whole-number/integer multiple of `divisor`, then exactDivide is a much faster way to perform division. If the division operation does not follow the `dividend % divisor == 0` requirement then you will get the wrong answer and no error. This is a 
limitation of the GMP function (this method is based on: [`mpz_divexact`](https://gmplib.org/manual/Integer-Division.html#index-mpz_005fdivexact)). 

### gcd

Finds the greatest common divisor of the two values that are input. It is exactly analogous to `BigInteger.gcd()` but *much* faster. When benchmarked with two 6148-bit values (sharing a 3074-bit factor) the GMP implementation was about 20x faster. 

## Notes

The maven artifact/jar embeds a precompiled libgmp for some platforms.  LibGmp will try to load the native library from the Java classpath first. If that fails, it falls back to trying a system-installed libgmp. We are missing binaries for many platforms.