package io.mods;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

public class Mod implements Parcelable {

    private String id;
    private String name;
    private String version;
    private String description;
    private String features;

    private String modIconUrl;
    private String apkUrl;

    private String architecture;
    private String size;
    private int downloads;
    private int views;
    private String createdAt;

    public Mod() {}

    protected Mod(Parcel in) {
        id = in.readString();
        name = in.readString();
        version = in.readString();
        description = in.readString();
        features = in.readString();
        modIconUrl = in.readString();
        apkUrl = in.readString();
        architecture = in.readString();
        size = in.readString();
        downloads = in.readInt();
        views = in.readInt();
        createdAt = in.readString();
    }

    @Exclude
    public static final Creator<Mod> CREATOR = new Creator<Mod>() {
        @Override
        public Mod createFromParcel(Parcel in) {
            return new Mod(in);
        }

        @Override
        public Mod[] newArray(int size) {
            return new Mod[size];
        }
    };

    @Exclude
    @Override
    public int describeContents() {
        return 0;
    }

    @Exclude
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(version);
        dest.writeString(description);
        dest.writeString(features);
        dest.writeString(modIconUrl);
        dest.writeString(apkUrl);
        dest.writeString(architecture);
        dest.writeString(size);
        dest.writeInt(downloads);
        dest.writeInt(views);
        dest.writeString(createdAt);
    }

    // ======= Getters & Setters =======

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFeatures() {
        return features;
    }

    public void setFeatures(String features) {
        this.features = features;
    }

    @PropertyName("mod_icon_url")
    public String getModIconUrl() {
        return modIconUrl;
    }

    @PropertyName("mod_icon_url")
    public void setModIconUrl(String modIconUrl) {
        this.modIconUrl = modIconUrl;
    }

    @PropertyName("apk_url")
    public String getApkUrl() {
        return apkUrl != null ? apkUrl : "";
    }

    @PropertyName("apk_url")
    public void setApkUrl(String apkUrl) {
        this.apkUrl = apkUrl != null ? apkUrl.trim() : "";
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getDownloads() {
        return downloads;
    }

    public void setDownloads(int downloads) {
        this.downloads = downloads;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    @PropertyName("createdAt")
    public String getCreatedAt() {
        return createdAt;
    }

    @PropertyName("createdAt")
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
