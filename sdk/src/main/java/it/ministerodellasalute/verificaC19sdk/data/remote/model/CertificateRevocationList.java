
package it.ministerodellasalute.verificaC19sdk.data.remote.model;

import java.util.List;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class CertificateRevocationList {

    @SerializedName("chunk")
    private Long mChunk;
    @SerializedName("creationDate")
    private String mCreationDate;
    @SerializedName("delta")
    private Delta mDelta;
    @SerializedName("firstElementInChunk")
    private String mFirstElementInChunk;
    @SerializedName("id")
    private String mId;
    @SerializedName("lastChunk")
    private Long mLastChunk;
    @SerializedName("lastElementInChunk")
    private String mLastElementInChunk;
    @SerializedName("revokedUcvi")
    private List<String> mRevokedUcvi;
    @SerializedName("sizeSingleChunkInByte")
    private Long mSizeSingleChunkInByte;
    @SerializedName("version")
    private Long mVersion;
    @SerializedName("totalNumberUCVI")
    private Long mTotalNumberUCVI;

    public Long getChunk() {
        return mChunk;
    }

    public void setChunk(Long chunk) {
        mChunk = chunk;
    }

    public String getCreationDate() {
        return mCreationDate;
    }

    public void setCreationDate(String creationDate) {
        mCreationDate = creationDate;
    }

    public Delta getDelta() {
        return mDelta;
    }

    public void setDelta(Delta delta) {
        mDelta = delta;
    }

    public String getFirstElementInChunk() {
        return mFirstElementInChunk;
    }

    public void setFirstElementInChunk(String firstElementInChunk) {
        mFirstElementInChunk = firstElementInChunk;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public Long getLastChunk() {
        return mLastChunk;
    }

    public void setLastChunk(Long lastChunk) {
        mLastChunk = lastChunk;
    }

    public String getLastElementInChunk() {
        return mLastElementInChunk;
    }

    public void setLastElementInChunk(String lastElementInChunk) {
        mLastElementInChunk = lastElementInChunk;
    }

    public List<String> getRevokedUcvi() {
        return mRevokedUcvi;
    }

    public void setRevokedUcvi(List<String> revokedUcvi) {
        mRevokedUcvi = revokedUcvi;
    }

    public Long getSizeSingleChunkInByte() {
        return mSizeSingleChunkInByte;
    }

    public void setSizeSingleChunkInByte(Long sizeSingleChunkInByte) {
        mSizeSingleChunkInByte = sizeSingleChunkInByte;
    }

    public Long getVersion() {
        return mVersion;
    }

    public void setVersion(Long version) {
        mVersion = version;
    }

    public Long getTotalNumberUCVI() {
        return mTotalNumberUCVI;
    }

    public void setTotalNumberUCVI(Long mTotalNumberUCVI) {
        this.mTotalNumberUCVI = mTotalNumberUCVI;
    }

}
