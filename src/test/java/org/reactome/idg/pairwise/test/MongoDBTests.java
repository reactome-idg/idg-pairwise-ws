package org.reactome.idg.pairwise.test;

import java.util.Arrays;

import org.bson.Document;
import org.junit.Test;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoDBTests {

    @Test
    public void testConnection() {
        MongoCredential credential = MongoCredential.createCredential("root",
                                                                      "idg_pairwise",
                                                                      "macmongodb01".toCharArray());
        MongoClient client = new MongoClient(new ServerAddress("localhost"), Arrays.asList(credential));
//        MongoClient client = new MongoClient();
        MongoDatabase database = client.getDatabase("idg_pairwise");
        MongoCollection<Document> collection = database.getCollection("test");
        Document doc = new Document("name", "MongoDB")
                .append("type", "database")
                .append("count", 1)
                .append("versions", Arrays.asList("v3.2", "v3.0", "v2.6"))
                .append("info", new Document("x", 203).append("y", 102));
        //        collection.insertOne(doc);
        //        collection.insertMany(Arrays.asList(
        //                                     Document.parse("{ item: 'journal', qty: 25, size: { h: 14, w: 21, uom: 'cm' }, status: 'A' }"),
        //                                     Document.parse("{ item: 'notebook', qty: 50, size: { h: 8.5, w: 11, uom: 'in' }, status: 'A' }"),
        //                                     Document.parse("{ item: 'paper', qty: 100, size: { h: 8.5, w: 11, uom: 'in' }, status: 'D' }"),
        //                                     Document.parse("{ item: 'planner', qty: 75, size: { h: 22.85, w: 30, uom: 'cm' }, status: 'D' }"),
        //                                     Document.parse("{ item: 'postcard', qty: 45, size: { h: 10, w: 15.25, uom: 'cm' }, status: 'A' }")
        //                             ));
        //        
        for (Document index : collection.listIndexes())
            System.out.println(index.toJson());
        client.close();
    }

}
