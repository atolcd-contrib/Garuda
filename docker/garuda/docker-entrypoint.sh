#!/bin/bash

# shellcheck disable=SC2206 # for GC_OPTS and JAVA_OPTS
commande=( java $GC_OPTS $JAVA_MEM_OPTS $JAVA_OPTS $GARUDA_OPTS --enable-preview -cp "/opt/garuda/classes:/opt/garuda/lib/*" play.core.server.ProdServerStart )

if [[ "$(id -u)" == "0" ]]; then
  commande=( setpriv --reuid=garuda --regid=garuda --init-groups "${commande[@]}" )
fi

exec "${commande[@]}" "$@"
