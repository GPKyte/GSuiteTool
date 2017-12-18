#!/bin/bash
# This will remove any previous versions and build the current package
# Note the && and || following most lines, these stop the program early
# in case of failure
DIR="$( cd "$(dirname "$0")" ; pwd -P )"
if [[ -f ${DIR}/build-gsuite/src/main/resources/client_secret.json ]]; then
    rm -rf ${DIR}/gsuite-?.?.?/
    cd ${DIR}/build-gsuite/ &&
    gradle run -P myArgs="['--reset']" &&
    gradle clean &&
    gradle buildNeeded &&
    cd ${DIR}/build-gsuite/build/distributions/ &&
    mv gsuite*.zip ${DIR}/ &&
    cd ${DIR} &&
    unzip -q ${DIR}/gsuite*.zip &&
    rm ${DIR}/gsuite*.zip ||
    echo "Build failed"
else
    echo "${DIR}/build-gsuite/src/main/resources/client_secret.json is missing. Abort."
fi
