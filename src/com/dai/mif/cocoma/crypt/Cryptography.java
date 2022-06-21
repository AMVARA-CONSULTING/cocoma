/**
 * $Id: Cryptography.java 138 2010-05-17 14:24:07Z rroeber $
 */
package com.dai.mif.cocoma.crypt;

import java.io.File;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.dai.mif.cocoma.CoCoMa;
import com.dai.mif.cocoma.logging.Logging;

/**
 * This class offers data encryption and decryption functionality based on the
 * symmetric AES encryption algorithm. The methods available offer means to
 * initialize the class with a password, loaded from a file or entered directly,
 * and offers en/decryption means for data based on the previous initialization.
 *
 * @author riedchr
 * @author Last change by $Author: rroeber $
 *
 * @since 22.07.2009
 * @version $Revision: 138 $ ($Date:: 2010-05-17 16:24:07 +0200#$)
 *
 */
public class Cryptography {

    /** Define a constant for the algorithm to be used. **/
    private static final String ALGORITHM_NAME = "AES";

    /** Define a static attribute to hold the singleton instance . **/
    private static Cryptography cryptographyInstance;

    /** Key specification for symmetric encryption. **/
    private SecretKeySpec secretKeySpec;

    /** Cipher to perform the crypting operations on. **/
    private Cipher cipher;

    /** Logging object for this class **/
    private Logger log;

    /** Hash for the current passphrase **/
    private String passPhraseHash;

    /**
     * Constructor for the Cryptography class. It prepares a key specification
     * and cipher object to perform the actual crypting activities.
     *
     * @param passPhrase
     *            The pass phrase to be used for initialization
     */
    private Cryptography(String passPhrase) {

        this.log = Logging.getInstance().getLog(this.getClass());

        if (passPhrase.length() == 0) {
            log.warn("An empty password has been specified. "
                    + "Cryptography is now based on this empty password. This is not a good idea!");
        }

        try {
            // update the passPhrase to be used for crypt actions
            updateCurrentPass(passPhrase);

            // and finally create a cipher for the actual crypto actions
            this.cipher = Cipher.getInstance(ALGORITHM_NAME);
        } catch (NoSuchAlgorithmException e) {
            String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
            CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
            log.error(msg);
        } catch (NoSuchPaddingException e) {
            String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
            CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
            log.error(msg);
        }

        cryptographyInstance = this;
    }

    /**
     * Static method to obtain an instance of Cryptography by initializing the
     * class with the given password.
     *
     * @param passPhrase
     *            The password to be used for initialization in string
     *            representation.
     * @return Newly created instance of Cryptography based on the given
     *         password.
     */
    public static Cryptography initialize(String passPhrase) {
        Cryptography cryptInstance;

        // initialize a Cryptography instance with the given passPhrase
        cryptInstance = new Cryptography(passPhrase);
        return cryptInstance;
    }

    /**
     * Singleton style getter for the current Cryptography instance. In case of
     * a non existing instance NULL is returned. The Instance has to be created
     * by calling either of the methods {@link #initialize(File)} or
     * {@link #initialize(String)}.
     *
     * @return Returns the current Cryptography instance or NULL if no such
     *         instance exists yet.
     */
    public static Cryptography getInstance() {
        return cryptographyInstance;
    }

    /**
     * Trigger an encryption of the given text based on the currently
     * initialized passPhrase. This method actually delegates to
     * {@link #doCryptAction(byte[], int)} and sets the proper crypt mode. After
     * this delegation the result is additionally encoded into Base64 encoding.
     *
     * @param clearText
     *            The clear text to be encrypted.
     * @return The encrypted version of the clear text parameter. The encrypted
     *         version is provided in Base64 encoding. Or NULL if there was an
     *         error encrypting the text.
     */
    public String encrypt(String clearText) {
        // delegate the crypt action to doCryptAction
        byte[] encrypted;

        encrypted = doCryptAction(clearText.getBytes(), Cipher.ENCRYPT_MODE);
        // finally perform a Base64 encoding on the result
        encrypted = Base64.encodeBase64(encrypted);
        // create a string from the encoded result
        return (encrypted != null) ? new String(encrypted) : null;
    }

    /**
     * Trigger a decryption of the given encrypted text based on the currently
     * initialized passPhrase. This method actually delegates to
     * {@link #doCryptAction(byte[], int)} and sets the proper crypt mode. The
     * given encrypted text is assumed to be in Base64 encoding.
     *
     * @param encryptedText
     *            The encrypted text in Base64 encoding to be encrypted.
     * @return The decrypted plain text version of the given encrypted text
     *         parameter. Or NULL if an error occured.
     */
    public String decrypt(String encryptedText) {
        byte[] decrypted;
        byte[] input;

        try {
            // decode the given encryptedText from Base64 encoding
            input = Base64.decodeBase64(encryptedText.getBytes());
            // perform the actual (de)crypt action on that result
            decrypted = doCryptAction(input, Cipher.DECRYPT_MODE);
        } catch (Exception e) {
            decrypted = null;
        }
        // create a string from the decrypted result
        return (decrypted != null) ? new String(decrypted) : null;
    }

    /**
     * Perform a crypt operation in the given input byte-array. The crypt
     * operation that is actually performed is denoted by the cryptMode
     * Parameter which can be either Cipher.ENCRYPT_MOD or Ciper.DECRYPT_MODE.
     *
     * @param input
     *            The byte-array that is to be treated as input for the selected
     *            crypt operation.
     * @param cryptMode
     *            Int value denoting the crypt operation to be performed. The
     *            value can either be Cipher.ENCRYPT_MOD or Ciper.DECRYPT_MODE
     * @return Creates a new byte-array representing the result of the selected
     *         crypt operation.
     */
    private byte[] doCryptAction(byte[] input, int cryptMode) {
        // prepare the result array
        byte[] output = null;

        try {
            // Initialize the cipher object for the given cryptMode
            this.cipher.init(cryptMode, this.secretKeySpec);
            // perform the actual crypt action
            output = this.cipher.doFinal(input);
        } catch (InvalidKeyException e) {
            log.debug(e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (IllegalBlockSizeException e) {
            log.debug(e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (BadPaddingException e) {
            log.debug(e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // return the resulting array
        return output;
    }

    /**
     * Getter for the passPhraseHash attribute. This hash is in Base64 encoding
     * and can be used to compare the current instance of this class with a
     * configuration file, to tell whether the passwords in that configuration
     * were actually encrypted with this passPhrase and if they can be decrypted
     * again. This can be used to avoid trying to decrypt passwords with a wrong
     * passPhrase.
     *
     * @return The MD5 hash of the currently set passPhrase for this instance
     */
    public String getPassPhraseHash() {
        return this.passPhraseHash;
    }

    /**
     * Convenience method to convert a byte array into a human readable HEX
     * representation.
     *
     * @param bytes
     *            Byte array to be converted.
     * @return The resulting string of the HEX representation
     */
    private String convertToHex(byte[] bytes) {
        StringBuffer hexString;

        hexString = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            hexString.append(Integer.toHexString(0xFF & bytes[i]));
        }
        return hexString.toString();
    }

    /**
     * Update the currently set passPhrase for this crypt object so that any
     * future encryptions and decryption will be based upon the newly given
     * passPhrase.
     *
     * @param passPhrase
     *            The passOhrase to be used from now on.
     */
    public void updateCurrentPass(String passPhrase) {

        // create an MD5 hash code for the given password
        try {
            MessageDigest messagedigest;
            messagedigest = MessageDigest.getInstance("MD5");
            messagedigest.reset();
            messagedigest.update(passPhrase.getBytes());

            // store the passphrase hash in Base64 encoding for later use
            byte[] md5HashBytes = messagedigest.digest();
            this.passPhraseHash = convertToHex(md5HashBytes);

            // create a keyspec for this passphrase hash
            this.secretKeySpec = new SecretKeySpec(md5HashBytes, ALGORITHM_NAME);

        } catch (NoSuchAlgorithmException e) {
            String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
            CoCoMa.setErrorCode(CoCoMa.COCOMA_ERROR_CRTICAL_ERROR, msg);
            log.error(msg);
        }

    }
}
