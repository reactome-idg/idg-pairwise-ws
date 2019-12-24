package org.reactome.idg.pairwise.model;

import java.util.ArrayList;
import java.util.List;

public class PairwiseRelationship {
    
    private String gene;
    private DataDesc dataDesc;
    private List<Integer> pos;
    private List<Integer> neg;
    
    public PairwiseRelationship() {
    }

    public DataDesc getDataDesc() {
        return dataDesc;
    }

    public void setDataDesc(DataDesc dataDesc) {
        this.dataDesc = dataDesc;
    }

    public String getGene() {
        return gene;
    }

    public void setGene(String gene) {
        this.gene = gene;
    }
    
    public void addPos(Integer gene) {
        if (pos == null)
            pos = new ArrayList<>();
        pos.add(gene);
    }

    public List<Integer> getPos() {
        return pos;
    }

    public void setPos(List<Integer> pos) {
        this.pos = pos;
    }

    public List<Integer> getNeg() {
        return neg;
    }

    public void setNeg(List<Integer> neg) {
        this.neg = neg;
    }
    
    public void addNeg(Integer gene) {
        if (neg == null)
            neg = new ArrayList<>();
        neg.add(gene);
    }
    
    public boolean isEmpty() {
        if ((pos == null || pos.size() == 0) && 
            (neg == null || neg.size() == 0))
            return true;
        return false;
    }

}
