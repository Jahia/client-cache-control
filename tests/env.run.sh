#!/usr/bin/env bash
# This script can be used to warmup the environment and execute the tests
# It is used by the docker image at startup

source ./set-env.sh

START_TIME=$SECONDS

echo " env.run.sh == Printing the most important environment variables"
echo " MANIFEST: ${MANIFEST}"
echo " TESTS_IMAGE: ${TESTS_IMAGE}"
echo " JAHIA_IMAGE: ${JAHIA_IMAGE}"
echo " JAHIA_CLUSTER_ENABLED: ${JAHIA_CLUSTER_ENABLED}"
echo " MODULE_ID: ${MODULE_ID}"
echo " JAHIA_URL: ${JAHIA_URL}"
echo " JAHIA_PROCESSING_URL: ${JAHIA_PROCESSING_URL}"
echo " JAHIA_PORT_KARAF: ${JAHIA_PORT_KARAF}"
echo " JAHIA_USERNAME: ${JAHIA_USERNAME}"
echo " JAHIA_PASSWORD: ${JAHIA_PASSWORD}"
echo " SUPER_USER_PASSWORD: ${SUPER_USER_PASSWORD}"
echo " TIMEZONE: ${TIMEZONE}"

echo " == Content of the tests folder"
ls -lah

echo " == Waiting for Jahia to startup"
while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' ${JAHIA_PROCESSING_URL}/cms/login)" != "200" ]];
  do sleep 5;
done
ELAPSED_TIME=$(($SECONDS - $START_TIME))
echo " == Jahia became alive in ${ELAPSED_TIME} seconds"

echo "$(date +'%d %B %Y - %k:%M') [JAHIA_CLUSTER_ENABLED] == Value: ${JAHIA_CLUSTER_ENABLED} =="
if [[ "${JAHIA_CLUSTER_ENABLED}" == "true" ]]; then
    echo "$(date +'%d %B %Y - %k:%M') [JAHIA_CLUSTER_ENABLED] == Jahia is running in cluster, waiting for all other nodes =="
    while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' http://jahia-browsing-a:8080/cms/login)" != "200" ]];
      do sleep 5;
    done
    while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' http://jahia-browsing-b:8080/cms/login)" != "200" ]];
      do sleep 5;
    done
    echo "$(date +'%d %B %Y - %k:%M') [JAHIA_CLUSTER_ENABLED] == Jahia is running in cluster, all nodes have started =="
fi

mkdir -p ./run-artifacts
mkdir -p ./results

# Copy manifest file
# If the file doesn't exist, we assume it is a URL and we download it locally
if [[ -e ${MANIFEST} ]]; then
  cp ${MANIFEST} ./run-artifacts
else
  echo "$(date +'%d %B %Y - %k:%M') == Downloading: ${MANIFEST}"
  curl ${MANIFEST} --output ./run-artifacts/curl-manifest
  MANIFEST="curl-manifest"
fi

# Snapshot bundle/feature jar exists in artifacts folder which means we want to uninstall jahia builtin client-cache-control and install the snapshot version
if compgen -G "./artifacts/org.jahia.bundles.client-cache-control-*-SNAPSHOT.jar" > /dev/null; then
    echo "Will uninstall existing client-cache-control and replace it with supplied snapshot"
    curl -u root:${SUPER_USER_PASSWORD} -X POST ${JAHIA_URL}/modules/api/provisioning --form script='[{"uninstallBundle":"org.jahia.bundles.client-cache-control-impl"}]'
    curl -u root:${SUPER_USER_PASSWORD} -X POST ${JAHIA_URL}/modules/api/provisioning --form script='[{"uninstallBundle":"org.jahia.bundles.client-cache-control-api"}]'
    #TODO: this is a workaround due to the fact that uninstall of bundle (not jahia module) is not replicated to other nodes see: https://jira.jahia.org/browse/BACKLOG-23361
    #TODO We uninstall the bundle instead of the feature because no feature provisioning is available for now
    echo "Uninstalling client-cache-control from others nodes if cluster is enabled"
    if [[ "${JAHIA_CLUSTER_ENABLED}" == "true" ]]; then
        curl -u root:${SUPER_USER_PASSWORD} -X POST http://jahia-browsing-a:8080/modules/api/provisioning --form script='[{"uninstallBundle":"org.jahia.bundles.client-cache-control-impl"}]'
        curl -u root:${SUPER_USER_PASSWORD} -X POST http://jahia-browsing-a:8080/modules/api/provisioning --form script='[{"uninstallBundle":"org.jahia.bundles.client-cache-control-impl"}]'
        curl -u root:${SUPER_USER_PASSWORD} -X POST http://jahia-browsing-b:8080/modules/api/provisioning --form script='[{"uninstallBundle":"org.jahia.bundles.client-cache-control-api"}]'
        curl -u root:${SUPER_USER_PASSWORD} -X POST http://jahia-browsing-b:8080/modules/api/provisioning --form script='[{"uninstallBundle":"org.jahia.bundles.client-cache-control-api"}]'
    fi
fi

# jar present in artifacts folder are installed (to install also test module and if needed the previously built bundle/feature when testing in a branch)
cd artifacts/
echo "$(date +'%d %B %Y - %k:%M') == Content of the artifacts/ folder"
ls -lah
echo "$(date +'%d %B %Y - %k:%M') [MODULE_INSTALL] == Will start submitting files"
for file in $(ls -1 *-SNAPSHOT.jar | sort -n)
do
  echo "$(date +'%d %B %Y - %k:%M') [MODULE_INSTALL] == Submitting module from: $file =="
  curl -u root:${SUPER_USER_PASSWORD} -X POST ${JAHIA_URL}/modules/api/provisioning --form script='[{"installAndStartBundle":"'"$file"'", "forceUpdate":true}]' --form file=@$file
  sleep 5;
  echo
  echo "$(date +'%d %B %Y - %k:%M') [MODULE_INSTALL] == Module submitted =="
done

sleep 60;

echo "$(date +'%d %B %Y - %k:%M') == Executing manifest: ${MANIFEST} =="
curl -u root:${SUPER_USER_PASSWORD} -X POST ${JAHIA_URL}/modules/api/provisioning --form script="@./run-artifacts/${MANIFEST};type=text/yaml"
echo
if [[ $? -eq 1 ]]; then
  echo "$(date +'%d %B %Y - %k:%M') == PROVISIONING FAILURE - EXITING SCRIPT, NOT RUNNING THE TESTS"
  echo "failure" > ./results/test_failure
  exit 1
fi

echo "$(date +'%d %B %Y - %k:%M') == Fetching the list of installed modules =="
bash -c "unset npm_config_package; npx --yes @jahia/jahia-reporter@latest utils:modules \
  --moduleId=\"${MODULE_ID}\" \
  --jahiaUrl=\"${JAHIA_URL}\" \
  --jahiaPassword=\"${SUPER_USER_PASSWORD}\" \
  --filepath=\"results/installed-jahia-modules.json\""
echo "$(date +'%d %B %Y - %k:%M') == Modules fetched =="
INSTALLED_MODULE_VERSION=$(cat results/installed-jahia-modules.json | jq '.module.version')
if [[ $INSTALLED_MODULE_VERSION == "UNKNOWN" ]]; then
  echo "$(date +'%d %B %Y - %k:%M') ERROR: Unable to detect module: ${MODULE_ID} on the remote system "
  echo "$(date +'%d %B %Y - %k:%M') ERROR: The Script will exit"
  echo "$(date +'%d %B %Y - %k:%M') ERROR: Tests will NOT run"
  echo "failure" > ./results/test_failure
  exit 1
fi

echo "$(date +'%d %B %Y - %k:%M') == Run tests =="
mkdir -p ./results/reports
rm -rf ./results/reports

if [[ -z "${CYPRESS_CONFIGURATION_FILE}" ]]; then
  CYPRESS_CONFIGURATION_FILE=cypress.config.ts
fi

if [[ "${TESTS_PROFILE}" != "" ]]; then
  CYPRESS_CONFIGURATION_FILE=${TESTS_PROFILE}
else
  CYPRESS_CONFIGURATION_FILE="cypress.config.ts"
fi

echo "$(date +'%d %B %Y - %k:%M') == Running Cypress with configuration file ${CYPRESS_CONFIGURATION_FILE} =="

yarn e2e:ci --config-file "${CYPRESS_CONFIGURATION_FILE}"

if [[ $? -eq 0 ]]; then
  echo "$(date +'%d %B %Y - %k:%M') == Full execution successful =="
  pwd
  echo "success" > ./results/test_success
  ls ./results
  yarn report:merge; yarn report:html
  exit 0
else
  echo "$(date +'%d %B %Y - %k:%M') == One or more failed tests =="
  echo "failure" > ./results/test_failure
  yarn report:merge; yarn report:html
  exit 1
fi
