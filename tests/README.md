## To test on Jahia with existing feature deployed (temporarly) : 

Uninstall bundles related to client-cache-control using tools OSGI 

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

#### Provisioning test rules 
(You may encounter dublin in rules, in that case use tools to remove all ruleset and provision again)

```bash
curl -v -u root:root1234 -X POST http://localhost:8080/modules/api/provisioning --form script="@provisioning-manifest-snapshot.yml;type=text/yaml"
```

## Jahia Nightly testing strategy 

This features (and its bundles) will be integrated in the Jahia Core. 
Thus the nightly test strategy is to ensure that the feature works as expected on Jahia Release and Snapshot, ensuring no regression occurs.

The current client-cache-control feature is NOT tested in the nightly, only on branches PRs and on merge to main.
For those tests, a specific piece of the `env.run.sh` script takes in charge the uninstall of builtin feature bundles (also over cluster) and the installation of local built bundles (line 58 and above).

After Jahia 8.2.3 will be released, we may think about running Nightly also for snapshot version of Client-Cache-Control feature (over Jahia SN only at first as Jahia RL should never integrate a new version of the feature until a backport need).

