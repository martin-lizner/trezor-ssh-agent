package com.trezoragent.struct;

/**
 *
 * @author martin.lizner
 */
public class PublicKeyDTO {
    private String sComment;
    private String sPublicKey;
    private byte[] bComment;
    private byte[] bPublicKey;

    public PublicKeyDTO() {
        this.bComment = null;
        this.bPublicKey = null;
        this.bComment = null;
        this.bPublicKey = null;
    }

    public PublicKeyDTO(byte[] bComment, byte[] bPublicKey) {
        this.bComment = bComment;
        this.bPublicKey = bPublicKey;
        this.bComment = null;
        this.bPublicKey = null;
    }

    public PublicKeyDTO(String sComment, String sPublicKey, byte[] bComment, byte[] bPublicKey, String owner) {
        this.sComment = sComment;
        this.sPublicKey = sPublicKey;
        this.bComment = bComment;
        this.bPublicKey = bPublicKey;
    }

    public String getsComment() {
        return sComment;
    }

    public void setsComment(String sComment) {
        this.sComment = sComment;
    }

    public String getsPublicKey() {
        return sPublicKey;
    }

    public void setsPublicKey(String sPublicKey) {
        this.sPublicKey = sPublicKey;
    }

    public byte[] getbComment() {
        return bComment;
    }

    public void setbComment(byte[] bComment) {
        this.bComment = bComment;
    }

    public byte[] getbPublicKey() {
        return bPublicKey;
    }

    public void setbPublicKey(byte[] bPublicKey) {
        this.bPublicKey = bPublicKey;
    }
     
}
