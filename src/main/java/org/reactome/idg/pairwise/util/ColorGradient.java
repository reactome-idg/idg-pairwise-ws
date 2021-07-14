package org.reactome.idg.pairwise.util;

/**
 * Copied from org.reactome.web.diagram.util.gradient
 */
public class ColorGradient {

    private Color from;
    private Color to;

    public ColorGradient(String hexFrom, String hexTo) throws Exception {
        this.from = new Color(hexFrom);
        this.to = new Color(hexTo);
    }

    public String getColor(double p){
        int r = getValue(this.from.getRed(), this.to.getRed(), p);
        int g = getValue(this.from.getGreen(), this.to.getGreen(), p);
        int b = getValue(this.from.getBlue(), this.to.getBlue(), p);
        try {
            Color rtn = new Color(r,g,b);
            return "#" + rtn.getHex();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private int getValue(int v1, int v2, double p){
    	if(p < 0d) p = 0d;
    	if(p > 1d) p = 1d;
        return (int) Math.round (v1 * p + v2 * (1 - p));
    }
}
