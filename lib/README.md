Jar files kept in this folder should be added into a local maven repository by running the following:

```bash
mvn install:install-file -Dfile={jar file name} -DgroupId=org.reactome.idg -DartifactId={name before the version} -Dversion={text after the artifactId} -Dpackaging=jar 

```
You may also create jar files directly from the original projects hosted in this repo using maven package and install.
