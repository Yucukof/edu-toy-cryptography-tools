package org.dcu.student.sem1.ca642.asymmetric.encryption;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.dcu.student.sem1.ca642.modulus.exponentiation.Exponentiation;

import java.util.Random;

import static org.dcu.student.sem1.ca642.modulus.exponentiation.SquareAndMultiply.power;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ElGamal {

    @Value
    public static class Ciphertext {

        int c1;
        int c2;

        @Override
        public String toString() {
            return "(" + c1 + "||" + c2 + ")";
        }
    }

    @Builder
    @Getter
    @RequiredArgsConstructor
    public static class PrivateKey {

        /**
         * A prime number of length greater than 512 bits.
         */
        private final int prime;
        /**
         * A generator of multiples in $\mathbb{Z}_p^*$
         *
         * @see #prime
         */
        private final int generator;
        /**
         * The private key component.
         *
         * @see PublicKey#value
         */
        private final int exponent;

        public static PrivateKey from(final int p, final int g, final int x) {
            return builder()
                  .prime(p)
                  .generator(g)
                  .exponent(x)
                  .build();
        }

        /**
         * Decrypt a given ciphertext c back to its original plaintext m.
         *
         * @param c the ciphertext $(c1||c2)$ to decrypt.
         * @return an integer representing $m$.
         * @see PublicKey#encrypt(int, int)
         */
        public int decrypt(final Ciphertext c) {

            final int c1 = c.getC1();
            final int c2 = c.getC2();

            log.info("Decrypting ciphertext ({}||{})...", c1, c2);

            final int p = prime;
            final int x = exponent;

            log.info("Calculating exponent (p-1-x)...");
            final int exponent = p - 1 - x;
            log.debug("{} - 1 - {} = {}", p, x, exponent);

            log.info("Calculating c_1^(p-1-x) (mod p)...");
            // NB : c_1^(p-1-x)\\equiv c_1^{-x}
            final int c1_ = Exponentiation.compute(c1, exponent, p);
            log.debug("c^{-x} = {}^{} (mod {}) = {}", c1, exponent, p, c1_);

            log.info("Calculating m = c1^(-x) x c2 (mod p)...");
            final int m = (c1_ * c2) % p;
            log.debug("m = ({} x {}) (mod {}) = {}", c1_, c2, p, m);

            log.info("Plaintext m = {}", m);
            return m;
        }

        public PublicKey getPublicKey() {
            log.info("Generating public key...");
            final int y = power(generator, exponent, prime);
            log.info("y = {}^{} (mod {}) = [{}]", generator, exponent, prime, y);
            return PublicKey.from(generator, prime, y);
        }

        @Override
        public String toString() {
            return "( y = " + exponent + ")";
        }
    }

    @Builder(access = AccessLevel.PRIVATE)
    @Getter
    @RequiredArgsConstructor
    public static class PublicKey {

        private static final Random rd = new Random();
        /**
         * A prime number of length greater than 512 bits.
         */
        private final int prime;
        /**
         * A generator of multiples in $\mathbb{Z}_p^*$
         *
         * @see #prime
         */
        private final int generator;
        /**
         * The derived public integer y computed as $y=g^x\pmod{p}$.
         *
         * @see #generator
         * @see #prime
         * @see PrivateKey#exponent
         */
        private final int value;

        private static PublicKey from(final int generator, final int prime, final int y) {
            return builder()
                  .prime(prime)
                  .generator(generator)
                  .value(y)
                  .build();
        }

        /**
         * Encrypt a given plaintext $m$ into a ciphertext $(c1||c2)$.
         *
         * @param m the plaintext to encrypt (must be smaller than ${@link PublicKey#prime p}$)
         * @param k an ephemeral key smaller than ${@link PublicKey#prime p}-1$ (see {@link
         *          #getEphemeralKey() getEphemeralKey}).
         * @return a tuple (c1||c2)
         * @see PrivateKey#decrypt(Ciphertext)
         */
        public Ciphertext encrypt(final int m, final int k) {

            log.info("Encrypting plaintext m = {}", m);
            split(m);

            final int p = prime;
            final int g = generator;
            final int y = value;

            log.info("Calculating C1 = g^k (mod p)...");
            final int c1 = Exponentiation.compute(g, k, p);
            log.debug("C1 = {}^{} (mod {}) = {}", g, k, p, c1);

            log.info("Calculating  y^k (mod p)...");
            final int yk = Exponentiation.compute(y, k, p);
            log.debug("y^k = {}^{} (mod {}) = {}", y, k, p, yk);

            log.info("Calculating C2 = my^k (mod p)...");
            final int c2 = (m * yk) % p;
            log.debug("C2 = {} x ({}^{}) (mod {}) = {}", m, y, k, p, c2);

            log.info("Ciphertext c = [({}||{})]", c1, c2);
            return new Ciphertext(c1, c2);
        }

        /**
         * Split the key in numbers of size $0 < m < p-1$
         *
         * @param m the message to split.
         */
        private void split(final int m) {
            if (m >= prime) {
                // Not supported :grimacing:
                // TODO: 26/11/2021 implement
                throw new UnsupportedOperationException();
            }
        }

        /**
         * Generate a random ephemeral key according to the public key values.
         * <br> i.e. $0 < k < p-1$
         *
         * @return a random integer smaller than $p-1$.
         */
        private int getEphemeralKey() {
            final int p = prime;
            int k = 0;
            do {
                k = rd.nextInt();
            } while (k <= 0 || k >= p);
            return k;
        }

        @Override
        public String toString() {
            return "(p = " + prime + ", g = " + generator + ", y = " + value + ")";
        }
    }
}
