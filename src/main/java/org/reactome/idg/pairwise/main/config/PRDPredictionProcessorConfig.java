package org.reactome.idg.pairwise.main.config;

public class PRDPredictionProcessorConfig {

	String folder;
	String prdProbabilitiesFile;
	String predictionFile;
	
	public PRDPredictionProcessorConfig() {
		super();
	}
	public PRDPredictionProcessorConfig(String folder, String prdProbabilitiesFile, String predictionFile) {
		super();
		this.folder = folder;
		this.prdProbabilitiesFile = prdProbabilitiesFile;
		this.predictionFile = predictionFile;
	}
	public String getFolder() {
		return folder;
	}
	public void setFolder(String folder) {
		this.folder = folder;
	}
	public String getPrdProbabilitiesFile() {
		return prdProbabilitiesFile;
	}
	public void setPrdProbabilitiesFile(String prdProbabilitiesFile) {
		this.prdProbabilitiesFile = prdProbabilitiesFile;
	}
	public String getPredictionFile() {
		return predictionFile;
	}
	public void setPredictionFile(String predictionFile) {
		this.predictionFile = predictionFile;
	}
}
