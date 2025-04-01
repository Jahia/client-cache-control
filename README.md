# Jahia Client Cache Control Feature

This feature manage Cache-Control header for Jahia. It ensure CDN optimisation by setting adapted Cache-Control header for each resource.

## How to build Client Cache Control Feature

The project is composed of : 
- an API bundle 
- an Implementation bundle
- a Feature module
- a test folder containing dedicated Jahia Module for testing purpose and some cypress tests.

There is no specific build required for this feature. As it is a maven based multi-module project, just call:

```bash
mvn clean install 
```

## Installation

The feature is already included in Jahia but you may want to install a specific version or upgrade it.

### Install a kar file (not recommended until specific purpose)

Even if not recommended, you may want to deploy the feature for development or testing purpose using the build .kar file.
```bash
docker cp ./client-cache-control-feature/target/client-cache-control-<version>.kar jahia:/var/jahia/karaf/deploy
```
Be aware that after install kar file, behavior regarding bundles can be unexpected as different can be present togethers.
  
### Install using provisioning

Not yet available

### Install using karaf 

The feature elements can be updated independently.

#### API's bundle
```bash
docker cp ./client-cache-control-api/target/org.jahia.bundles.client-cache-control-api-<version>.jar jahia:/var/jahia/karaf/deploy
```

#### Implementation's bundle
```bash
docker cp ./client-cache-control-bundle/target/org.jahia.bundles.client-cache-control-impl-<version>.jar jahia:/var/jahia/karaf/deploy
```

#### Configuration file

You can install also configuration file or edit them directly from Jahia's Karaf /etc folder.

```bash
docker cp ./client-cache-control-bundle/target/classes/META-INF/configurations/org.jahia.bundles.cache.client.ruleset-default.yml jahia:/var/jahia/karaf/deploy
```

## Testing

Use a Jahia instance with version >= 8.2.2.0 

Build and deploy the required testing module : 

```bash
cd tests/jahia-module
mvn clean package
docker cp ./target/client-cache-control-test-template-<version>.jar jahia:/var/jahia/modules
cd ..
./set-env.sh
yarn install
yarn run e2e:debug
```

