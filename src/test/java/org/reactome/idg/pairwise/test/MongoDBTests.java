package org.reactome.idg.pairwise.test;

import java.util.Arrays;

import org.bson.Document;
import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.junit.Test;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;

public class MongoDBTests {
	
	@Test
	public void testMySQL() throws Exception {
		MySQLAdaptor dba = new MySQLAdaptor("localhost",
				"reactome", 
				"root",
				"B2u$6ger");
		GKInstance instance = dba.fetchInstance(21L);
		System.out.println("Release: " + instance);
	}

    @Test
    public void testConnection() {
        MongoCredential credential = MongoCredential.createCredential("beaversd",
                                                                      "idg_pairwise",
                                                                      "B2u$6ger".toCharArray());
        MongoClient client = new MongoClient(new ServerAddress("localhost"), Arrays.asList(credential));
        MongoDatabase database = client.getDatabase("idg_pairwise");
        MongoCollection<Document> collection = database.getCollection("relationships");
        String[] genes = new String[] {
                "EGF",
                "NOTCH1",
                "OR4F29"
        };
        FindIterable<Document> geneDocs = collection.find(Filters.in("_id", genes))
                .projection(Projections.fields(Projections.include("GTEx|Ovary|Gene_Coexpression", "GTEx|Breast-MammaryTissue|Gene_Coexpression")));
        for (Document geneDoc : geneDocs) {
            System.out.println();
            printDocument(geneDoc);
        }
        client.close();
    }
    
    private void printDocument(Document geneDoc) {
        for (String key : geneDoc.keySet()) {
            Object value = geneDoc.get(key);
            if (value instanceof Document) {
                System.out.println(key + ":");
                printDocument((Document)value);
            }
            else
                System.out.println(key + ": " + value);
        }
    }

}
