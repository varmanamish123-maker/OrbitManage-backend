package com.orbit.portfolio.model;

import com.orbit.portfolio.model.enums.AssetType;
import com.orbit.portfolio.model.enums.AssetUnit;
import jakarta.persistence.*;

@Entity
@Table(name = "assets")
public class Asset extends BaseEntity {

    private String assetName;

    @Enumerated(EnumType.STRING)
    private AssetType assetType;

    private String assetMetadata;

    @Enumerated(EnumType.STRING)
    private AssetUnit unit;

    protected Asset() {}

    public Asset(String assetName, AssetType assetType, String assetMetadata, AssetUnit unit) {
        this.assetName = assetName;
        this.assetType = assetType;
        this.assetMetadata = assetMetadata;
        this.unit = unit;
    }

    public String getAssetName() { return assetName; }
    public void setAssetName(String assetName) { this.assetName = assetName; }

    public AssetType getAssetType() { return assetType; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }

    public String getAssetMetadata() { return assetMetadata; }
    public void setAssetMetadata(String assetMetadata) { this.assetMetadata = assetMetadata; }

    public AssetUnit getUnit() { return unit; }
    public void setUnit(AssetUnit unit) { this.unit = unit; }
}