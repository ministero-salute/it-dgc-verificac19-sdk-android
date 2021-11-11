
package it.ministerodellasalute.verificaC19sdk.data.remote.model;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class CrlStatus {

    @SerializedName("fromVersion")
    private Long mFromVersion;
    @SerializedName("id")
    private String mId;
    @SerializedName("chunk")
    private Long mChunk;
    @SerializedName("numDiAdd")
    private Long mNumDiAdd;
    @SerializedName("numDiDelete")
    private Long mNumDiDelete;
    @SerializedName("sizeSingleChunkInByte")
    private Long mSizeSingleChunkInByte;
    @SerializedName("totalSizeInByte")
    private Long mTotalSizeInByte;
    @SerializedName("version")
    private Long mVersion;
    @SerializedName("totalChunk")
    private Long mTotalChunk;
    @SerializedName("totalNumberUCVI")
    private Long mTotalNumberUCVI;

    public Long getFromVersion() {
        return mFromVersion;
    }

    public void setFromVersion(Long fromVersion) {
        mFromVersion = fromVersion;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public Long getChunk() {
        return mChunk;
    }

    public void setChunk(Long chunk) {
        mChunk = chunk;
    }

    public Long getNumDiAdd() {
        return mNumDiAdd;
    }

    public void setNumDiAdd(Long numDiAdd) {
        mNumDiAdd = numDiAdd;
    }

    public Long getNumDiDelete() {
        return mNumDiDelete;
    }

    public Long getTotalChunk() {
        return mTotalChunk;
    }

    public void setTotalChunk(Long mTotalChunk) {
        this.mTotalChunk = mTotalChunk;
    }

    public Long getTotalNumberUCVI() {
        return mTotalNumberUCVI;
    }

    public void setTotalNumberUCVI(Long mTotalNumberUCVI) {
        this.mTotalNumberUCVI = mTotalNumberUCVI;
    }

    public void setNumDiDelete(Long numDiDelete) {
        mNumDiDelete = numDiDelete;
    }

    public Long getSizeSingleChunkInByte() {
        return mSizeSingleChunkInByte;
    }

    public void setSizeSingleChunkInByte(Long sizeSingleChunkInByte) {
        mSizeSingleChunkInByte = sizeSingleChunkInByte;
    }

    public Long getTotalSizeInByte() {
        return mTotalSizeInByte;
    }

    public void setTotalSizeInByte(Long totalSizeInByte) {
        mTotalSizeInByte = totalSizeInByte;
    }

    public Long getVersion() {
        return mVersion;
    }

    public void setVersion(Long version) {
        mVersion = version;
    }

}
