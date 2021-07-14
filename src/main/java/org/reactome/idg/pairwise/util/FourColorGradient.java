package org.reactome.idg.pairwise.util;

/**
 * 
 * @author brunsont
 *
 */
public class FourColorGradient {
	
	private String Tdark = "#f44336";
	private String Tbio = "#ffb259";
	private String Tchem = "#5bc0de";
	private String Tclin = "#337ab7";
	
	private ColorGradient first;
	private ColorGradient second;
	private ColorGradient third;
	
	public FourColorGradient(){
		try {
			this.first = new ColorGradient(Tbio, Tdark);
			this.second = new ColorGradient(Tchem, Tbio);
			this.third = new ColorGradient(Tclin, Tchem);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getColor(double wAvgTDL) {
		if(wAvgTDL < 2d) return this.first.getColor(wAvgTDL -2.0d);
		if(wAvgTDL < 3d) return this.second.getColor(wAvgTDL - 3.0d);
		return this.third.getColor(wAvgTDL);
	}
}
