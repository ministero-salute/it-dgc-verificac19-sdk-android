
package it.ministerodellasalute.verificaC19sdk.data.remote.model;

import java.util.List;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class Delta {

    @SerializedName("deletions")
    private List<String> mDeletions;
    @SerializedName("insertions")
    private List<String> mInsertions;

    public List<String> getDeletions() {
        return mDeletions;
    }

    public void setDeletions(List<String> deletions) {
        mDeletions = deletions;
    }

    public List<String> getInsertions() {
        return mInsertions;
    }

    public void setInsertions(List<String> insertions) {
        mInsertions = insertions;
    }

}
