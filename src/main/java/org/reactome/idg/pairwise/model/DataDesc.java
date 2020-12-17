package org.reactome.idg.pairwise.model;

import org.reactome.idg.model.FeatureType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class DataDesc {
    
    // Used as the primary index, which should be unique
    private String id;
    // Used as short key for server calls
    private int digitalKey;
    // The original data source, e.g. GTEx or TCGA
    private String provenance; 
    // Data type: e.g. co-expression or PPI
    private FeatureType dataType;
    // Tissue or cancer type
    private String bioSource;
    // For harmonizome data, this field is used to track the original
    // data source, e.g., motifmap or ctddisease
    private String origin;
    
    public DataDesc() {
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getId() {
        if (id == null) { // Generate an id based on three values. Note: Since this will be used as a primary index
                          // key, make sure the cocatenated string is not too long (should be less than 250 bytes).
            id = provenance + "|" + bioSource + "|" + dataType;
            if (origin != null)
                id += "|" + origin; // This is an optional field
            // Hard-coded to 100 characters, assuming on character takes 2 bytes
            if (id.length() > 100)
                id = id.substring(0, 100);
        }
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProvenance() {
        return provenance;
    }

    public void setProvenance(String provenance) {
        this.provenance = provenance;
    }

    public FeatureType getDataType() {
        return dataType;
    }

    public void setDataType(FeatureType featureType) {
        this.dataType = featureType;
    }

    public String getBioSource() {
        return bioSource;
    }

    public void setBioSource(String bioSource) {
        this.bioSource = bioSource;
    }
    
    public int getDigitalKey() {
    	return this.digitalKey;
    }
    
    public void setDigitalKey(int digitalKey) {
    	this.digitalKey = digitalKey;
    }

}
