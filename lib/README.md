# Libs

Libraries that are not published to maven.

We're having some problems publishing the Openml Apiconnector and OpenmlWeka to Maven Central. See build_instructions.txt in the directory of the ApiConnector (openml-java). I'm not able to make it work (Jos, Sept 2024). oss.sonatype.org throws a "Failed to deploy artifacts: Could not transfer artifact org.openml:apiconnector:kar:1.0.26 from/to sonatype-nexus-staging (https://oss.sonatype.org/service/local/staging/deploy/maven2/): Transfer failed for https://oss.sonatype.org/service/local/staging/deploy/maven2/org/openml/apiconnector/1.0.26/apiconnector-1.0.26.jar 401 Content access is protected by token." I assume that my token (created on https://central.sonatype.com/) is incorrect. I cannot log into https://oss.sonatype.org/. After a while, I decided to give up and to add the libraries here. Ugly, but it works...
