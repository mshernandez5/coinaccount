package com.mshernandez.coinaccount.service.wallet_rpc.result;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GetAddressInfoResult
{
    /**
     * The address validated.
     */
    private String address;

    /**
     * The hex-encoded scriptPubKey
     * generated by this address.
     */
    private String scriptPubKey;

    /**
     * If the address is yours.
     */
    @JsonProperty("ismine")
    private boolean mine;

    /**
     * If the address is watchonly.
     */
    @JsonProperty("iswatchonly")
    private boolean watchOnly;

    /**
     * If we know how to spend coins
     * sent to this address, ignoring
     * the possible lack of private
     * keys.
     */
    private boolean solvable;

    /**
     * A descriptor for spending
     * coins sent to this address
     * (only when solvable).
     */
    @JsonProperty("desc")
    private String descriptor;

    /**
     * If the key is a script.
     */
    @JsonProperty("isscript")
    private boolean isScript;

    /**
     * If the address was used for
     * change output.
     */
    @JsonProperty("ischange")
    private boolean change;

    /**
     * If the address is a witness
     * address.
     */
    @JsonProperty("iswitness")
    private boolean witness;

    /**
     * The version number of the witness
     * program.
     */
    @JsonProperty("witness_version")
    private int witnessVersion;

    /**
     * The hex value of the witness program.
     */
    @JsonProperty("witness_program")
    private String witnessProgram;

    /**
     * The output script type.
     * Only if isScript is true and the
     * redeemScript is known.
     * <p>
     * Possible types:
     * <ul>
     *     <li>nonstandard</li>
     *     <li>pubkey</li>
     *     <li>pubkeyhash</li>
     *     <li>scripthash</li>
     *     <li>multisig</li
     *     <li>nulldata</li>
     *     <li>witness_v0_keyhash</li>
     *     <li>witness_v0_scripthash</li>
     *     <li>witness_unknown</li>
     * </ul>
     */
    private String script;

    /**
     * The redeem script for the
     * P2SH address.
     */
    @JsonProperty("hex")
    private String redeemScript;

    /**
     * An array of pubkeys associated
     * with the known redeem script
     * (only if script is multisig).
     */
    @JsonProperty("pubkeys")
    private List<String> pubKeys;

    /**
     * The number of signatures required
     * to spend multisig output
     * (only if the script is multisig).
     */
    @JsonProperty("sigsrequired")
    private int sigsRequired;

    /**
     * The hex value of the raw public key
     * for single-key addresses
     * (possibly embedded in P2SH or P2WSH).
     */
    @JsonProperty("pubkey")
    private String pubKey;

    /**
     * If the pubKey is compressed.
     */
    @JsonProperty("iscompressed")
    private boolean compressed;

    /**
     * DEPRECATED. The label associated with
     * the address, defaults to "".
     * Replaced by a labels array.
     */
    private String label;

    /**
     * The creation of the key, if available,
     * expressed in UNIX epoch time.
     */
    private long timestamp;

    /**
     * The HD keypath, if the key is HD
     * and available.
     */
    @JsonProperty("hdkeypath")
    private String hdKeyPath;

    /**
     * The Hash160 of the HD seed.
     */
    @JsonProperty("hdseedid")
    private String hdSeedId;

    /**
     * The fingerprint of the master key.
     */
    @JsonProperty("hdmasterfingerprint")
    private String hdMasterFingerprint;
}