/**
 * Copyright 2011 Google Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package main.redanda.crypt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Arrays;

import org.spongycastle.asn1.*;
import org.spongycastle.asn1.sec.SECNamedCurves;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.agreement.ECDHBasicAgreement;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.ec.CustomNamedCurves;
import org.spongycastle.crypto.generators.ECKeyPairGenerator;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECKeyGenerationParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.crypto.signers.ECDSASigner;
import org.spongycastle.crypto.signers.HMacDSAKCalculator;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECFieldElement;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.math.ec.FixedPointUtil;
import org.spongycastle.util.encoders.Base64;

// TODO: This class is quite a mess by now. Once users are migrated away from Java serialization for the wallets,
// refactor this to have better internal layout and a more consistent API.

/**
 * <p>
 * Represents an elliptic curve public and (optionally) private key, usable for
 * digital signatures but not encryption. Creating a new ECKey with the empty
 * constructor will generate a new random keypair. Other constructors can be
 * used when you already have the public or private parts. If you create a key
 * with only the public part, you can check signatures but not create them.</p>
 *
 * <p>
 * ECKey also provides access to Bitcoin-Qt compatible text message signing, as
 * accessible via the UI or JSON-RPC. This is slightly different to signing raw
 * bytes - if you want to sign your own data and it won't be exposed as text to
 * people, you don't want to use this. If in doubt, ask on the mailing list.</p>
 *
 * <p>
 * The ECDSA algorithm supports <i>key recovery</i> in which a signature plus a
 * couple of discriminator bits can be reversed to find the public key used to
 * calculate it. This can be convenient when you have a message and a signature
 * and want to find out who signed it, rather than requiring the user to provide
 * the expected identity.</p>
 */
public class ECKey implements Serializable {

    private static final ECDomainParameters ecParams;
    private static final SecureRandom secureRandom;
    private static final long serialVersionUID = -728224901792295832L;

    private static final X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");
    public static final ECDomainParameters CURVE;
    public static final BigInteger HALF_CURVE_ORDER;

    static {
        // All clients must agree on the curve to use by agreement. Bitcoin uses secp256k1.
        X9ECParameters params = SECNamedCurves.getByName("secp256k1");
        ecParams = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());
        secureRandom = new SecureRandom();
        FixedPointUtil.precompute(CURVE_PARAMS.getG(), 12);
        CURVE = new ECDomainParameters(CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(), CURVE_PARAMS.getN(),
                CURVE_PARAMS.getH());
        HALF_CURVE_ORDER = CURVE_PARAMS.getN().shiftRight(1);
    }

    // The two parts of the key. If "priv" is set, "pub" can always be calculated. If "pub" is set but not "priv", we
    // can only verify signatures not make them.
    // TODO: Redesign this class to use consistent internals and more efficient serialization.
    private BigInteger priv;
    private byte[] pub;
    // Creation time of the key in seconds since the epoch, or zero if the key was deserialized from a version that did
    // not have this field.
    private long creationTimeSeconds;
    // Transient because it's calculated on demand.
    transient private byte[] pubKeyHash;
    public int database_id = -1;

    /**
     * Generates an entirely new keypair. Point compression is used so the
     * resulting public key will be 33 bytes (32 for the co-ordinate and 1 byte
     * to represent the y bit).
     */
    public ECKey() {
        ECKeyPairGenerator generator = new ECKeyPairGenerator();
        ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(ecParams, secureRandom);
        generator.init(keygenParams);
        AsymmetricCipherKeyPair keypair = generator.generateKeyPair();
        ECPrivateKeyParameters privParams = (ECPrivateKeyParameters) keypair.getPrivate();
        ECPublicKeyParameters pubParams = (ECPublicKeyParameters) keypair.getPublic();
        priv = privParams.getD();
        // Unfortunately Bouncy Castle does not let us explicitly change a point to be compressed, even though it
        // could easily do so. We must re-build it here so the ECPoints withCompression flag can be set to true.
        ECPoint uncompressed = pubParams.getQ();
        ECPoint compressed = compressPoint(uncompressed);
        pub = compressed.getEncoded();

        creationTimeSeconds = Utils.now().getTime() / 1000;
    }

    private static ECPoint compressPoint(ECPoint uncompressed) {
        return new ECPoint.Fp(ecParams.getCurve(), uncompressed.getX(), uncompressed.getY(), true);
    }

    /**
     * Construct an ECKey from an ASN.1 encoded private key. These are produced
     * by OpenSSL and stored by the Bitcoin reference implementation in its
     * wallet. Note that this is slow because it requires an EC point multiply.
     */
    public static ECKey fromASN1(byte[] asn1privkey) {
        return new ECKey(extractPrivateKeyFromASN1(asn1privkey));
    }

    /**
     * Output this ECKey as an ASN.1 encoded private key, as understood by
     * OpenSSL or used by the Bitcoin reference implementation in its wallet
     * storage format.
     */
    public byte[] toASN1() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(400);

            // ASN1_SEQUENCE(EC_PRIVATEKEY) = {
            //   ASN1_SIMPLE(EC_PRIVATEKEY, version, LONG),
            //   ASN1_SIMPLE(EC_PRIVATEKEY, privateKey, ASN1_OCTET_STRING),
            //   ASN1_EXP_OPT(EC_PRIVATEKEY, parameters, ECPKPARAMETERS, 0),
            //   ASN1_EXP_OPT(EC_PRIVATEKEY, publicKey, ASN1_BIT_STRING, 1)
            // } ASN1_SEQUENCE_END(EC_PRIVATEKEY)
            DERSequenceGenerator seq = new DERSequenceGenerator(baos);
            seq.addObject(new ASN1Integer(1)); // version
            seq.addObject(new DEROctetString(priv.toByteArray()));
            seq.addObject(new DERTaggedObject(0, SECNamedCurves.getByName("secp256k1").toASN1Primitive()));
            seq.addObject(new DERTaggedObject(1, new DERBitString(getPubKey())));
            seq.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);  // Cannot happen, writing to memory stream.
        }
    }

    /**
     * Creates an ECKey given either the private key only, the public key only,
     * or both. If only the private key is supplied, the public key will be
     * calculated from it (this is slow). If both are supplied, it's assumed the
     * public key already correctly matches the public key. If only the public
     * key is supplied, this ECKey cannot be used for signing.
     *
     * @param compressed If set to true and pubKey is null, the derived public
     *                   key will be in compressed form.
     */
    public ECKey(BigInteger privKey, byte[] pubKey, boolean compressed) {
        this.priv = privKey;
        this.pub = null;
        if (pubKey == null && privKey != null) {
            // Derive public from private.
            this.pub = publicKeyFromPrivate(privKey, compressed);
        } else if (pubKey != null) {
            // We expect the pubkey to be in regular encoded form, just as a BigInteger. Therefore the first byte is
            // a special marker byte.
            // TODO: This is probably not a useful API and may be confusing.
            this.pub = pubKey;
        }
    }

    /**
     * Creates an ECKey given either the private key only, the public key only,
     * or both. If only the private key is supplied, the public key will be
     * calculated from it (this is slow). If both are supplied, it's assumed the
     * public key already correctly matches the public key. If only the public
     * key is supplied, this ECKey cannot be used for signing.
     */
    private ECKey(BigInteger privKey, byte[] pubKey) {
        this(privKey, pubKey, false);
    }

    /**
     * Creates an ECKey given the private key only. The public key is calculated
     * from it (this is slow)
     */
    public ECKey(BigInteger privKey) {
        this(privKey, (byte[]) null);
    }

    /**
     * A constructor variant with BigInteger pubkey. See
     * {@link ECKey#ECKey(BigInteger, byte[])}.
     */
    public ECKey(BigInteger privKey, BigInteger pubKey) {
        this(privKey, Utils.bigIntegerToBytes(pubKey, 65));
    }

    /**
     * Creates an ECKey given only the private key bytes. This is the same as
     * using the BigInteger constructor, but is more convenient if you are
     * importing a key from elsewhere. If not provided the public key will be
     * automatically derived from the private key.
     */
    public ECKey(byte[] privKeyBytes, byte[] pubKey) {
        this(privKeyBytes == null ? null : new BigInteger(1, privKeyBytes), pubKey);
    }

    /**
     * Returns public key bytes from the given private key. To convert a byte
     * array into a BigInteger, use <tt>
     * new BigInteger(1, bytes);</tt>
     */
    public static byte[] publicKeyFromPrivate(BigInteger privKey, boolean compressed) {
        ECPoint point = ecParams.getG().multiply(privKey);
        if (compressed) {
            point = compressPoint(point);
        }
        return point.getEncoded();
    }

    /**
     * Gets the hash160 form of the public key (as seen in addresses).
     */
    public byte[] getPubKeyHash() {
        if (pubKeyHash == null) {
            pubKeyHash = Utils.sha256hash160(this.pub);
        }
        return pubKeyHash;
    }

    /**
     * Gets the raw public key value. This appears in transaction scriptSigs.
     * Note that this is <b>not</b> the same as the pubKeyHash/address.
     */
    public byte[] getPubKey() {
        return pub;
    }

    /**
     * Returns whether this key is using the compressed form or not. Compressed
     * pubkeys are only 33 bytes, not 64.
     */
    public boolean isCompressed() {
        return pub.length == 33;
    }

    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("pub:").append(Utils.bytesToHexString(pub));
        if (creationTimeSeconds != 0) {
            b.append(" timestamp:" + creationTimeSeconds);
        }
        return b.toString();
    }

    public String toStringWithPrivate() {
        StringBuffer b = new StringBuffer();
        b.append(toString());
        if (priv != null) {
            b.append(" priv:").append(Utils.bytesToHexString(priv.toByteArray()));
        }
        return b.toString();
    }

//    /**
//     * Returns the address that corresponds to the public part of this ECKey. Note that an address is derived from
//     * the RIPEMD-160 hash of the public key and is not the public key itself (which is too large to be convenient).
//     */
//    public Address toAddress(NetworkParameters params) {
//        byte[] hash160 = Utils.sha256hash160(pub);
//        return new Address(params, hash160);
//    }

    /**
     * Groups the two components that make up a signature, and provides a way to
     * encode to DER form, which is how ECDSA signatures are represented when
     * embedded in other data structures in the Bitcoin protocol. The raw
     * components can be useful for doing further EC maths on them.
     */
    public static class ECDSASignature {

        /**
         * The two components of the signature.
         */
        public final BigInteger r, s;

        /**
         * Constructs a signature with the given components. Does NOT
         * automatically canonicalise the signature.
         */
        public ECDSASignature(BigInteger r, BigInteger s) {
            this.r = r;
            this.s = s;
        }

        public byte[] toBytes() {

            byte[] bytes = Utils.bigIntegerToBytes(r, 32);

            BigInteger rN = new BigInteger(bytes);

            if (rN.signum() == -1) {
                byte[] bNew = new byte[33];
                System.arraycopy(bytes, 0, bNew, 1, 32);
                rN = new BigInteger(bNew);
            }


            System.out.println("r: " + rN.equals(r));
            System.out.println(Utils.bytesToHexString(r.toByteArray()));
            System.out.println(Utils.bytesToHexString(rN.toByteArray()));

            System.out.println(r.toString());
            System.out.println(rN.toString());

            ByteBuffer b = ByteBuffer.allocate(64);
            b.put(Utils.bigIntegerToBytes(r, 32));
            b.put(Utils.bigIntegerToBytes(s, 32));
//            b.put(r.toByteArray());
//            b.put(s.toByteArray());


            if (!fromBytes(b.array()).r.equals(r)) {
                throw new RuntimeException("dhwaiuzdwad");
            }

            return b.array();
        }

        public static ECDSASignature fromBytes(byte[] bytes) {

            ByteBuffer b = ByteBuffer.wrap(bytes);

            byte[] r = new byte[32];
            byte[] s = new byte[32];

            b.get(r);
            b.get(s);

            /*
            Utils.bigIntegerToBytes removes the first byte if len is 33, because leading byte is then zero!
            lets check if BigInt is negative, then we have to add again the leading zero byte
             */
            BigInteger rN = new BigInteger(r);

            if (rN.signum() == -1) {
                byte[] bNew = new byte[33];
                System.arraycopy(bytes, 0, bNew, 1, 32);
                rN = new BigInteger(bNew);
            }


            return new ECDSASignature(rN, new BigInteger(s));
        }

        /**
         * Returns true if the S component is "low", that means it is below
         * {@link ECKey#HALF_CURVE_ORDER}. See <a
         * href="https://github.com/bitcoin/bips/blob/master/bip-0062.mediawiki#Low_S_values_in_signatures">BIP62</a>.
         */
        public boolean isCanonical() {
            return s.compareTo(HALF_CURVE_ORDER) <= 0;
        }

        /**
         * Will automatically adjust the S component to be less than or equal to
         * half the curve order, if necessary. This is required because for
         * every signature (r,s) the signature (r, -s (mod N)) is a valid
         * signature of the same message. However, we dislike the ability to
         * modify the bits of a Bitcoin transaction after it's been signed, as
         * that violates various assumed invariants. Thus in future only one of
         * those forms will be considered legal and the other will be banned.
         */
        public ECDSASignature toCanonicalised() {
            if (!isCanonical()) {
                // The order of the curve is the number of valid points that exist on that curve. If S is in the upper
                // half of the number of valid points, then bring it back to the lower half. Otherwise, imagine that
                //    N = 10
                //    s = 8, so (-8 % 10 == 2) thus both (r, 8) and (r, 2) are valid solutions.
                //    10 - 8 == 2, giving us always the latter solution, which is canonical.
                return new ECDSASignature(r, CURVE.getN().subtract(s));
            } else {
                return this;
            }
        }

        /**
         * DER is an international standard for serializing data structures
         * which is widely used in cryptography. It's somewhat like protocol
         * buffers but less convenient. This method returns a standard DER
         * encoding of the signature, as recognized by OpenSSL and other
         * libraries.
         */
        public byte[] encodeToDER() {
            try {
                return derByteStream().toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);  // Cannot happen.
            }
        }

        public static ECDSASignature decodeFromDER(byte[] bytes) {
            ASN1InputStream decoder = null;
            try {
                decoder = new ASN1InputStream(bytes);
                DLSequence seq = (DLSequence) decoder.readObject();
                if (seq == null) {
                    throw new RuntimeException("Reached past end of ASN.1 stream.");
                }
                ASN1Integer r, s;
                try {
                    r = (ASN1Integer) seq.getObjectAt(0);
                    s = (ASN1Integer) seq.getObjectAt(1);
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException(e);
                }
                // OpenSSL deviates from the DER spec by interpreting these values as unsigned, though they should not be
                // Thus, we always use the positive versions. See: http://r6.ca/blog/20111119T211504Z.html
                return new ECDSASignature(r.getPositiveValue(), s.getPositiveValue());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (decoder != null) {
                    try {
                        decoder.close();
                    } catch (IOException x) {
                    }
                }
            }
        }

        protected ByteArrayOutputStream derByteStream() throws IOException {
            // Usually 70-72 bytes.
            ByteArrayOutputStream bos = new ByteArrayOutputStream(72);
            DERSequenceGenerator seq = new DERSequenceGenerator(bos);
            seq.addObject(new ASN1Integer(r));
            seq.addObject(new ASN1Integer(s));
            seq.close();
            return bos;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ECDSASignature other = (ECDSASignature) o;
            return r.equals(other.r)
                    && s.equals(other.s);
        }

        @Override
        public int hashCode() {
            int result = r.hashCode();
            result = 31 * result + s.hashCode();
            return result;
        }
    }

    /**
     * Signs the given hash and returns the R and S components as BigIntegers.
     * In the Bitcoin protocol, they are usually encoded using DER format, so
     * you want {@link ECKey#signToDER(Sha256Hash)} instead. However sometimes
     * the independent components can be useful, for instance, if you're doing
     * to do further EC maths on them.
     *
     * @throws IllegalStateException if this ECKey doesn't have a private part.
     */
    public ECDSASignature sign(Sha256Hash input) {
        if (priv == null) {
            throw new IllegalStateException("This ECKey does not have the private key necessary for signing.");
        }
        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        ECPrivateKeyParameters privKey = new ECPrivateKeyParameters(priv, CURVE);
        signer.init(true, privKey);
        BigInteger[] components = signer.generateSignature(input.getBytes());
        return new ECDSASignature(components[0], components[1]).toCanonicalised();
    }

    /**
     * <p>
     * Verifies the given ECDSA signature against the message bytes using the
     * public key bytes.</p>
     *
     * <p>
     * When using native ECDSA verification, data must be 32 bytes, and no
     * element may be larger than 520 bytes.</p>
     *
     * @param data      Hash of the data to verify.
     * @param signature ASN.1 encoded signature.
     * @param pub       The public key bytes to use.
     */
    public static boolean verify(byte[] data, ECDSASignature signature, byte[] pub) {

        if (NativeSecp256k1.enabled) {
            return NativeSecp256k1.verify(data, signature.encodeToDER(), pub);
        }

        ECDSASigner signer = new ECDSASigner();
        ECPublicKeyParameters params = new ECPublicKeyParameters(CURVE.getCurve().decodePoint(pub), CURVE);
        signer.init(false, params);
        try {
            return signer.verifySignature(data, signature.r, signature.s);
        } catch (NullPointerException e) {
            // Bouncy Castle contains a bug that can cause NPEs given specially crafted signatures. Those signatures
            // are inherently invalid/attack sigs so we just fail them here rather than crash the thread.
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifies the given ASN.1 encoded ECDSA signature against a hash using the
     * public key.
     *
     * @param data      Hash of the data to verify.
     * @param signature ASN.1 encoded signature.
     * @param pub       The public key bytes to use.
     */
    public static boolean verify(byte[] data, byte[] signature, byte[] pub) {
        if (NativeSecp256k1.enabled) {
            return NativeSecp256k1.verify(data, signature, pub);
        }
        return verify(data, ECDSASignature.decodeFromDER(signature), pub);
    }

    /**
     * Verifies the given byte encoded ECDSA signature against a hash using the
     * public key.
     *
     * @param data      Hash of the data to verify.
     * @param signature ASN.1 encoded signature.
     * @param pub       The public key bytes to use.
     */
    public static boolean verifyPrimitive(byte[] data, byte[] signature, byte[] pub) {
        if (NativeSecp256k1.enabled) {
            return NativeSecp256k1.verify(data, signature, pub);
        }
        return verify(data, ECDSASignature.fromBytes(signature), pub);
    }

    /**
     * Verifies the given bytes encoded ECDSA signature against a hash using the
     * public key.
     *
     * @param hash      Hash of the data to verify.
     * @param signature ASN.1 encoded signature.
     */
    public boolean verifyPrimitive(byte[] hash, byte[] signature) {
        return ECKey.verifyPrimitive(hash, signature, getPubKey());
    }

    /**
     * Verifies the given ASN.1 encoded ECDSA signature against a hash using the
     * public key.
     *
     * @param hash      Hash of the data to verify.
     * @param signature ASN.1 encoded signature.
     */
    public boolean verify(byte[] hash, byte[] signature) {
        return ECKey.verify(hash, signature, getPubKey());
    }

    /**
     * Verifies the given R/S pair (signature) against a hash using the public
     * key.
     */
    public boolean verify(Sha256Hash sigHash, ECDSASignature signature) {
        return ECKey.verify(sigHash.getBytes(), signature, getPubKey());
    }

    /**
     * Verifies the given ASN.1 encoded ECDSA signature against a hash using the
     * public key, and throws an exception if the signature doesn't match
     *
     * @throws java.security.SignatureException if the signature does not match.
     */
    public void verifyOrThrow(byte[] hash, byte[] signature) throws SignatureException {
        if (!verify(hash, signature)) {
            throw new SignatureException();
        }
    }

    /**
     * Verifies the given R/S pair (signature) against a hash using the public
     * key, and throws an exception if the signature doesn't match
     *
     * @throws java.security.SignatureException if the signature does not match.
     */
    public void verifyOrThrow(Sha256Hash sigHash, ECDSASignature signature) throws SignatureException {
        if (!ECKey.verify(sigHash.getBytes(), signature, getPubKey())) {
            throw new SignatureException();
        }
    }

    private static BigInteger extractPrivateKeyFromASN1(byte[] asn1privkey) {
        // To understand this code, see the definition of the ASN.1 format for EC private keys in the OpenSSL source
        // code in ec_asn1.c:
        //
        // ASN1_SEQUENCE(EC_PRIVATEKEY) = {
        //   ASN1_SIMPLE(EC_PRIVATEKEY, version, LONG),
        //   ASN1_SIMPLE(EC_PRIVATEKEY, privateKey, ASN1_OCTET_STRING),
        //   ASN1_EXP_OPT(EC_PRIVATEKEY, parameters, ECPKPARAMETERS, 0),
        //   ASN1_EXP_OPT(EC_PRIVATEKEY, publicKey, ASN1_BIT_STRING, 1)
        // } ASN1_SEQUENCE_END(EC_PRIVATEKEY)
        //
        try {
            ASN1InputStream decoder = new ASN1InputStream(asn1privkey);
            DLSequence seq = (DLSequence) decoder.readObject();
            checkArgument(seq.size() == 4, "Input does not appear to be an ASN.1 OpenSSL EC private key");
            checkArgument(((DERInteger) seq.getObjectAt(0)).getValue().equals(BigInteger.ONE),
                    "Input is of wrong version");
            Object obj = seq.getObjectAt(1);
            byte[] bits = ((ASN1OctetString) obj).getOctets();
            decoder.close();
            return new BigInteger(1, bits);
        } catch (IOException e) {
            throw new RuntimeException(e);  // Cannot happen, reading from memory stream.
        }
    }

    /**
     * Signs a text message using the standard Bitcoin messaging signing format
     * and returns the signature as a base64 encoded string.
     *
     * @throws IllegalStateException if this ECKey does not have the private
     *                               part.
     */
    public String signMessage(String message) {
        if (priv == null) {
            throw new IllegalStateException("This ECKey does not have the private key necessary for signing.");
        }
        byte[] data = Utils.formatMessageForSigning(message);
        //Sha256Hash hash = Sha256Hash.createDouble(data);
        Sha256Hash hash = Sha256Hash.create(data); //ToDo: mark changed

        ECDSASignature sig = sign(hash);
        // Now we have to work backwards to figure out the recId needed to recover the signature.
        int recId = -1;
        for (int i = 0; i < 4; i++) {
            ECKey k = ECKey.recoverFromSignature(i, sig, hash, isCompressed());
            if (k != null && Arrays.equals(k.pub, pub)) {
                recId = i;
                break;
            }
        }
        if (recId == -1) {
            throw new RuntimeException("Could not construct a recoverable key. This should never happen.");
        }
        int headerByte = recId + 27 + (isCompressed() ? 4 : 0);
        byte[] sigData = new byte[65];  // 1 header + 32 bytes for R + 32 bytes for S
        sigData[0] = (byte) headerByte;
        System.arraycopy(Utils.bigIntegerToBytes(sig.r, 32), 0, sigData, 1, 32);
        System.arraycopy(Utils.bigIntegerToBytes(sig.s, 32), 0, sigData, 33, 32);
        return new String(Base64.encode(sigData), Charset.forName("UTF-8"));
    }

    /**
     * Given an arbitrary piece of text and a Bitcoin-format message signature
     * encoded in base64, returns an ECKey containing the public key that was
     * used to sign it. This can then be compared to the expected public key to
     * determine if the signature was correct. These sorts of signatures are
     * compatible with the Bitcoin-Qt/bitcoind format generated by
     * signmessage/verifymessage RPCs and GUI menu options. They are intended
     * for humans to verify their communications with each other, hence the
     * base64 format and the fact that the input is text.
     *
     * @param message         Some piece of human readable text.
     * @param signatureBase64 The Bitcoin-format message signature in base64
     * @throws SignatureException If the public key could not be recovered or if
     *                            there was a signature format error.
     */
    public static ECKey signedMessageToKey(String message, String signatureBase64) throws SignatureException {
        byte[] signatureEncoded;
        try {
            signatureEncoded = Base64.decode(signatureBase64);
        } catch (RuntimeException e) {
            // This is what you get back from Bouncy Castle if base64 doesn't decode :(
            throw new SignatureException("Could not decode base64", e);
        }
        // Parse the signature bytes into r/s and the selector value.
        if (signatureEncoded.length < 65) {
            throw new SignatureException("Signature truncated, expected 65 bytes and got " + signatureEncoded.length);
        }
        int header = signatureEncoded[0] & 0xFF;
        // The header byte: 0x1B = first key with even y, 0x1C = first key with odd y,
        //                  0x1D = second key with even y, 0x1E = second key with odd y
        if (header < 27 || header > 34) {
            throw new SignatureException("Header byte out of range: " + header);
        }
        BigInteger r = new BigInteger(1, Arrays.copyOfRange(signatureEncoded, 1, 33));
        BigInteger s = new BigInteger(1, Arrays.copyOfRange(signatureEncoded, 33, 65));
        ECDSASignature sig = new ECDSASignature(r, s);
        byte[] messageBytes = Utils.formatMessageForSigning(message);
        // Note that the C++ code doesn't actually seem to specify any character encoding. Presumably it's whatever
        // JSON-SPIRIT hands back. Assume UTF-8 for now.
        Sha256Hash messageHash = Sha256Hash.create(messageBytes);
        boolean compressed = false;
        if (header >= 31) {
            compressed = true;
            header -= 4;
        }
        int recId = header - 27;
        ECKey key = ECKey.recoverFromSignature(recId, sig, messageHash, compressed);
        if (key == null) {
            throw new SignatureException("Could not recover public key from signature");
        }
        return key;
    }

    /**
     * Convenience wrapper around
     * {@link ECKey#signedMessageToKey(String, String)}. If the key derived from
     * the signature is not the same as this one, throws a SignatureException.
     */
    public void verifyMessage(String message, String signatureBase64) throws SignatureException {
        ECKey key = ECKey.signedMessageToKey(message, signatureBase64);
        if (!Arrays.equals(key.getPubKey(), pub)) {
            throw new SignatureException("Signature did not match for message");
        }
    }

    /**
     * <p>
     * Given the components of a signature and a selector value, recover and
     * return the public key that generated the signature according to the
     * algorithm in SEC1v2 section 4.1.6.</p>
     *
     * <p>
     * The recId is an index from 0 to 3 which indicates which of the 4 possible
     * keys is the correct one. Because the key recovery operation yields
     * multiple potential keys, the correct key must either be stored alongside
     * the signature, or you must be willing to try each recId in turn until you
     * find one that outputs the key you are expecting.</p>
     *
     * <p>
     * If this method returns null it means recovery was not possible and recId
     * should be iterated.</p>
     *
     * <p>
     * Given the above two points, a correct usage of this method is inside a
     * for loop from 0 to 3, and if the output is null OR a key that is not the
     * one you expect, you try again with the next recId.</p>
     *
     * @param recId      Which possible key to recover.
     * @param r          The R component of the signature.
     * @param s          The S component of the signature.
     * @param message    Hash of the data that was signed.
     * @param compressed Whether or not the original pubkey was compressed.
     * @return An ECKey containing only the public part, or null if recovery
     * wasn't possible.
     */
    public static ECKey recoverFromSignature(int recId, ECDSASignature sig, Sha256Hash message, boolean compressed) {
        checkArgument(recId >= 0, "recId must be positive");
        checkArgument(sig.r.compareTo(BigInteger.ZERO) >= 0, "r must be positive");
        checkArgument(sig.s.compareTo(BigInteger.ZERO) >= 0, "s must be positive");
        if (message == null) {
            throw new NullPointerException("argument null");
        }
        // 1.0 For j from 0 to h   (h == recId here and the loop is outside this function)
        //   1.1 Let x = r + jn
        BigInteger n = ecParams.getN();  // Curve order.
        BigInteger i = BigInteger.valueOf((long) recId / 2);
        BigInteger x = sig.r.add(i.multiply(n));
        //   1.2. Convert the integer x to an octet string X of length mlen using the conversion routine
        //        specified in Section 2.3.7, where mlen = ⌈(log2 p)/8⌉ or mlen = ⌈m/8⌉.
        //   1.3. Convert the octet string (16 set binary digits)||X to an elliptic curve point R using the
        //        conversion routine specified in Section 2.3.4. If this conversion routine outputs “invalid”, then
        //        do another iteration of Step 1.
        //
        // More concisely, what these points mean is to use X as a compressed public key.
        ECCurve.Fp curve = (ECCurve.Fp) ecParams.getCurve();
        BigInteger prime = curve.getQ();  // Bouncy Castle is not consistent about the letter it uses for the prime.
        if (x.compareTo(prime) >= 0) {
            // Cannot have point co-ordinates larger than this as everything takes place modulo Q.
            return null;
        }
        // Compressed keys require you to know an extra bit of data about the y-coord as there are two possibilities.
        // So it's encoded in the recId.
        ECPoint R = decompressKey(x, (recId & 1) == 1);
        //   1.4. If nR != point at infinity, then do another iteration of Step 1 (callers responsibility).
        if (!R.multiply(n).isInfinity()) {
            return null;
        }
        //   1.5. Compute e from M using Steps 2 and 3 of ECDSA signature verification.
        BigInteger e = message.toBigInteger();
        //   1.6. For k from 1 to 2 do the following.   (loop is outside this function via iterating recId)
        //   1.6.1. Compute a candidate public key as:
        //               Q = mi(r) * (sR - eG)
        //
        // Where mi(x) is the modular multiplicative inverse. We transform this into the following:
        //               Q = (mi(r) * s ** R) + (mi(r) * -e ** G)
        // Where -e is the modular additive inverse of e, that is z such that z + e = 0 (mod n). In the above equation
        // ** is point multiplication and + is point addition (the EC group operator).
        //
        // We can find the additive inverse by subtracting e from zero then taking the mod. For example the additive
        // inverse of 3 modulo 11 is 8 because 3 + 8 mod 11 = 0, and -3 mod 11 = 8.
        BigInteger eInv = BigInteger.ZERO.subtract(e).mod(n);
        BigInteger rInv = sig.r.modInverse(n);
        BigInteger srInv = rInv.multiply(sig.s).mod(n);
        BigInteger eInvrInv = rInv.multiply(eInv).mod(n);
        ECPoint p1 = ecParams.getG().multiply(eInvrInv);
        ECPoint p2 = R.multiply(srInv);
        ECPoint.Fp q = (ECPoint.Fp) p2.add(p1);
        if (compressed) {
            // We have to manually recompress the point as the compressed-ness gets lost when multiply() is used.
            q = new ECPoint.Fp(curve, q.getX(), q.getY(), true);
        }
        return new ECKey((byte[]) null, q.getEncoded());
    }

    /**
     * Decompress a compressed public key (x co-ord and low-bit of y-coord).
     */
    private static ECPoint decompressKey(BigInteger xBN, boolean yBit) {
        // This code is adapted from Bouncy Castle ECCurve.Fp.decodePoint(), but it wasn't easily re-used.
        ECCurve.Fp curve = (ECCurve.Fp) ecParams.getCurve();
        ECFieldElement x = new ECFieldElement.Fp(curve.getQ(), xBN);
        ECFieldElement alpha = x.multiply(x.square().add(curve.getA())).add(curve.getB());
        ECFieldElement beta = alpha.sqrt();
        // If we can't find a sqrt we haven't got a point on the curve - invalid inputs.
        if (beta == null) {
            throw new IllegalArgumentException("Invalid point compression");
        }
        if (beta.toBigInteger().testBit(0) == yBit) {
            return new ECPoint.Fp(curve, x, beta, true);
        } else {
            ECFieldElement.Fp y = new ECFieldElement.Fp(curve.getQ(), curve.getQ().subtract(beta.toBigInteger()));
            return new ECPoint.Fp(curve, x, y, true);
        }
    }

    /**
     * Returns a 32 byte array containing the private key.
     */
    public byte[] getPrivKeyBytes() {
        return Utils.bigIntegerToBytes(priv, 32);
    }

//    /**
//     * Exports the private key in the form used by the Satoshi client "dumpprivkey" and "importprivkey" commands. Use
//     * the {@link com.google.bitcoin.core.DumpedPrivateKey#toString()} method to get the string.
//     *
//     * @param params The network this key is intended for use on.
//     * @return Private key bytes as a {@link DumpedPrivateKey}.
//     */
//    public DumpedPrivateKey getPrivateKeyEncoded(NetworkParameters params) {
//        return new DumpedPrivateKey(params, getPrivKeyBytes(), isCompressed());
//    }

    /**
     * Returns the creation time of this key or zero if the key was deserialized
     * from a version that did not store that data.
     */
    public long getCreationTimeSeconds() {
        return creationTimeSeconds;
    }

    /**
     * Sets the creation time of this key. Zero is a convention to mean
     * "unavailable". This method can be useful when you have a raw key you are
     * importing from somewhere else.
     *
     * @param newCreationTimeSeconds
     */
    public void setCreationTimeSeconds(long newCreationTimeSeconds) {
        if (newCreationTimeSeconds < 0) {
            throw new IllegalArgumentException("Cannot set creation time to negative value: " + newCreationTimeSeconds);
        }
        creationTimeSeconds = newCreationTimeSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ECKey ecKey = (ECKey) o;

        if (!Arrays.equals(pub, ecKey.pub)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        // Public keys are random already so we can just use a part of them as the hashcode. Read from the start to
        // avoid picking up the type code (compressed vs uncompressed) which is tacked on the end.
        return (pub[0] & 0xFF) | ((pub[1] & 0xFF) << 8) | ((pub[2] & 0xFF) << 16) | ((pub[3] & 0xFF) << 24);
    }

    private static void checkArgument(boolean b, String a) {
        if (!b) {
            throw new IllegalArgumentException(a);
        }
    }


    public BigInteger deffiehelman(byte[] otherPublicBytes) {
        ECPrivateKeyParameters privKey = new ECPrivateKeyParameters(priv, CURVE);
        ECDHBasicAgreement agreementGenerator = new ECDHBasicAgreement();
        agreementGenerator.init(privKey);
        ECPublicKeyParameters params = new ECPublicKeyParameters(CURVE.getCurve().decodePoint(otherPublicBytes), CURVE);
        BigInteger calculateAgreement = agreementGenerator.calculateAgreement(params);
        //System.out.println("asd " + calculateAgreement);
        return calculateAgreement;
    }
}
