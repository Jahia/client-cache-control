## To test on Jahia with existing feature deployed (temporarly) : 

Uninstall bundles related to clien-cache-control using tools OSGI 

#### Install local bundles manually : 

```bash
mvn package  
docker cp ./client-cache-control-api/target/org.jahia.bundles.client-cache-control-api-9.0.0-SNAPSHOT.jar jahia0:/var/jahia/karaf/deploy
docker cp ./client-cache-control-bundle/target/org.jahia.bundles.client-cache-control-impl-9.0.0-SNAPSHOT.jar jahia0:/var/jahia/karaf/deploy
docker cp ./client-cache-control-bundle/src/main/resources/META-INF/configurations/org.jahia.bundles.cache.client.ruleset-default.yml jahia0:/var/jahia/karaf/deploy
docker cp ./client-cache-control-graphql/target/org.jahia.bundles.client-cache-control-graphql-9.0.0-SNAPSHOT.jar jahia0:/var/jahia/karaf/deploy
```

#### Install Jahia module for testing


```bash
cd tests/jahia-module/
mvn clean package
docker cp ./target/client-cache-control-test-template-9.0.0-SNAPSHOT.jar jahia0:/var/jahia/modules
```

#### Provision test rules 
(You may encounter dublin in rules, in that case use tools to remove all ruleset and provision again)

```bash
curl -v -u root:root1234 -X POST http://localhost:8080/modules/api/provisioning --form script="@provisioning-manifest-snapshot.yml;type=text/yaml"
```

