package org.reactome.idg.pairwise.main;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Run this class to generate a map from UniProt to genes using the TCRD database. The generated mapping file is used
 * in class org.reactome.idg.pairwise.service.PairwiseService.
 * @author wug
 *
 */
public class GeneToUniprotMapGenerator {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Provide parameters in the order: tcrdDbName tcrdDbUser tcrdDbPwd");
            return;
        }
        String url = "jdbc:mysql://localhost:3306/" + args[0] + "?user=" + args[1] + "&password=" + args[2];
        Connection connection = DriverManager.getConnection(url);
        Statement stat = connection.createStatement();
        ResultSet results = stat.executeQuery("SELECT uniprot, sym FROM protein");
        String fileName = "src/main/resources/GeneToUniProt.txt";
        PrintWriter writer = new PrintWriter(fileName);
        writer.println("UniProt\tGeneSym");
        while (results.next()) {
            String uniprot = results.getString(1);
            String sym = results.getString(2);
            writer.println(uniprot + "\t" + sym);
        }
        writer.close();
        results.close();
        stat.close();
        connection.close();
    }
    
}
