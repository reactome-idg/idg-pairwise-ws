package org.reactome.idg.pairwise.test;

import org.bson.Document;
import org.junit.Test;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;

public class MongoDBTests {

    @Test
    public void testConnection() {
//        MongoCredential credential = MongoCredential.createCredential("root",
//                                                                      "idg_pairwise",
//                                                                      "macmongodb01".toCharArray());
//        MongoClient client = new MongoClient(new ServerAddress("localhost"), Arrays.asList(credential));
        String uri = String.format(
                "mongodb://%s:%s@%s/%s",
                "root",
                "macmongodb01",
                "localhost",
                "admin"
            );
        MongoClient client = MongoClients.create(uri);
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
