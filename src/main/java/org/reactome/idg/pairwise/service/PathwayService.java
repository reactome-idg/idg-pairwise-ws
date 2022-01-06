package org.reactome.idg.pairwise.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.GKApplicationUtilities;
import org.reactome.data.ReactomeAnalyzer;
import org.reactome.idg.bn.PathwayImpactAnalyzer;
import org.reactome.idg.pairwise.model.Pathway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * The new implementation uses the relational database to build the mapping between pathways and genes so
 * that there is no need to handle static files for each release. The actual implementation is ported from
 * class PathawyGeneSetGenerator in the FI network build project: https://github.com/reactome/fi_network_build/blob/master/src/org/reactome/fi/PathwayGeneSetGenerator.java.
 * @author brunsont, guanming
 *
 */
@Service
public class PathwayService {
	private static final Logger logger = LoggerFactory.getLogger(PathwayService.class);

	@Autowired
	private MySQLAdaptor mysqlDBA;

	//attempt at removing need for pathways and PATHWAY_INDEX collections
	private Map<String, Set<Pathway>> geneToPathwayList;
	private Map<String, Pathway> pathwayStIdToPathway;
	private Map<String, Set<String>> geneToPathwayStId;

	public PathwayService() {/*Nothing Here*/}

	/**
	 * This implementation collects the mapping between genes and pathways directly from a released
	 * MySQL database.
	 */
	@SuppressWarnings("unchecked")
	private void cachePathways(Map<String, String> uniprotToGene) {
		geneToPathwayList = new HashMap<>();
		pathwayStIdToPathway = new HashMap<>();

		try {
			// Use pathways having laid-out ELV as the base pathways for easy comparison and visualization
			List<GKInstance> basePathways = new PathwayImpactAnalyzer().loadPathwaysForAnalysis(mysqlDBA);
			// Pull out all human pathways
			GKInstance human = mysqlDBA.fetchInstance(GKApplicationUtilities.HOMO_SAPIENS_DB_ID);
			Collection<GKInstance> humanPathways = mysqlDBA.fetchInstanceByAttribute(ReactomeJavaConstants.Pathway,
					ReactomeJavaConstants.species, 
					"=",
					human);
			ReactomeAnalyzer reactomeAnalyzer = new ReactomeAnalyzer();
			reactomeAnalyzer.setMySQLAdaptor(mysqlDBA);
			// In this context we want to include candidate members so that the numbers are more consistent
			// with the web application
			reactomeAnalyzer.getTopicHelper().setNeedCandidateRepeatedUnit(true);
			Map<GKInstance, Set<String>> pathway2uniprotIds = reactomeAnalyzer.grepIDsFromTopics(humanPathways);
			// Convert to the required data structures
			for (GKInstance pathway : pathway2uniprotIds.keySet()) {
				Set<String> uniprotIds = pathway2uniprotIds.get(pathway);
				GKInstance stableIdInst = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
				String stableId = (String) stableIdInst.getAttributeValue(ReactomeJavaConstants.identifier);
				Pathway pathwayObj = new Pathway(stableId,
												 pathway.getDisplayName(),
												 basePathways.contains(pathway));
				pathwayStIdToPathway.put(stableId, pathwayObj);
				for (String uniprotId : uniprotIds) {
					// Just in case isoform is used. But most unlikely.
					String gene = uniprotToGene.get(uniprotId.split("-")[0]);
					if (gene == null)
						continue;
					pathwayObj.addGene(gene);
					geneToPathwayList.compute(gene, (k, set) -> {
						if (set == null)
							set = new HashSet<>();
						set.add(pathwayObj);
						return set;
					});
				}
			}
			// Just close MySQLAdaptor. There is no need in other place
			mysqlDBA.cleanUp();
		} 
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public Map<String, Pathway> getPathwayStIdToPathway(Map<String, String> uniprotToGene){
		if(this.pathwayStIdToPathway == null)
			this.cachePathways(uniprotToGene);
		return this.pathwayStIdToPathway;
	}

	public Map<String, Set<Pathway>> getGeneToPathwayList(Map<String, String> uniprotToGene){
		if(this.geneToPathwayList == null)
			this.cachePathways(uniprotToGene);
		return this.geneToPathwayList;
	}  

	public Map<String, Set<String>> getGeneToPathwayStId(Map<String, String> uniprotToGene){
		if(this.geneToPathwayStId == null) {
			this.geneToPathwayStId =  getGeneToPathwayList(uniprotToGene).entrySet().stream()
					.collect(Collectors.toMap(
							Map.Entry::getKey, 
							e -> e.getValue().stream().map(Pathway::getStId).collect(Collectors.toSet())));
		}
		return this.geneToPathwayStId;
	}
}
