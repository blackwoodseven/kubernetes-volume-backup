#!/bin/bash

set -e

if [ -f /var/run/secrets/kubernetes.io/serviceaccount/ca.crt ]; then
    keytool -import -noprompt -storepass changeit -alias dev-server -keystore $JAVA_HOME/jre/lib/security/cacerts -file /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
fi
exec "$@"
