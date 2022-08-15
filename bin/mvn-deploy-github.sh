#!/usr/bin/env bash

MVN_SETTINGS_TEMPLATE_FILE=.mvn/settings.xml.tmpl
MVN_SETTINGS_FILE=.mvn/settings.xml
POM_FILE=pom.xml

sed -e "s/GH_REPO/${GH_REPO}/g" ${MVN_SETTINGS_TEMPLATE_FILE} > ${MVN_SETTINGS_FILE}
sed -e "s/GH_DEPLOY_USERNAME/${GH_DEPLOY_USERNAME}/g" ${MVN_SETTINGS_TEMPLATE_FILE} > ${MVN_SETTINGS_FILE}
sed -e "s/GH_DEPLOY_TOKEN/${GH_DEPLOY_TOKEN}/g" ${MVN_SETTINGS_TEMPLATE_FILE} > ${MVN_SETTINGS_FILE}

if [[ $GITHUB_REF_NAME == v* ]]; then
  echo "creating release version $GITHUB_REF_NAME"
  sed -i -e "s%\<version>0\.2-SNAPSHOT<\/version>%<version>${GITHUB_REF_NAME}</version>%g" ${POM_FILE}
fi

mvn -s ${MVN_SETTINGS_FILE} --batch-mode deploy -Dlicense.skip=true
