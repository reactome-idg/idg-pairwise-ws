package org.reactome.idg.pairwise.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class PairwiseRelationship {
    
    private String gene;
    private DataDesc dataDesc;
    @JsonIgnore
    private List<Integer> pos;
    @JsonIgnore
    private List<Integer> neg;
    private List<String> posGenes;
    private List<String> negGenes;
    // Provide numbers of partners
    private Integer posNum;
    private Integer negNum;
    
    public List<String> getPosGenes() {
        return posGenes;
    }

    public Integer getPosNum() {
        return posNum;
    }

    public void setPosNum(Integer posNum) {
        this.posNum = posNum;
    }

    public Integer getNegNum() {
        return negNum;
    }

    public void setNegNum(Integer negNum) {
        this.negNum = negNum;
    }

    public void setPosGenes(List<String> posGenes) {
        this.posGenes = posGenes;
    }

    public List<String> getNegGenes() {
        return negGenes;
    }

    public void setNegGenes(List<String> negGenes) {
        this.negGenes = negGenes;
    }

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
    
    @JsonIgnore
    public boolean isEmpty() {
        if ((pos == null || pos.size() == 0) && 
            (neg == null || neg.size() == 0))
            return true;
        return false;
    }

}
