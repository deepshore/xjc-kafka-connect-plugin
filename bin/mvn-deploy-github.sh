#!/usr/bin/env bash

MVN_SETTINGS_TEMPLATE_FILE=.mvn/settings.xml.tmpl
MVN_SETTINGS_FILE=.mvn/settings.xml
POM_FILE=pom.xml
DEPLOY_CMD=

sed -e "s/GH_REPO/${GH_REPO}/g" ${MVN_SETTINGS_TEMPLATE_FILE} > ${MVN_SETTINGS_FILE}
sed -e "s/GH_DEPLOY_USERNAME/${GH_DEPLOY_USERNAME}/g" ${MVN_SETTINGS_TEMPLATE_FILE} > ${MVN_SETTINGS_FILE}
sed -e "s/GH_DEPLOY_TOKEN/${GH_DEPLOY_TOKEN}/g" ${MVN_SETTINGS_TEMPLATE_FILE} > ${MVN_SETTINGS_FILE}

if [[ $GITHUB_REF_NAME == v* ]]; then
  echo "creating release version ${GITHUB_REF_NAME#v}"
  mvn -s ${MVN_SETTINGS_FILE} release:prepare -DreleaseVersion=${GITHUB_REF_NAME#v} || true
fi

mvn -s ${MVN_SETTINGS_FILE} --batch-mode deploy -Dlicense.skip=true
