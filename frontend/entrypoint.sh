#!/bin/sh
set -e

envsubst '$GATEWAY_URI $NOTIFICATION_URI' \
  < /etc/nginx/conf.d/default.conf.template \
  > /etc/nginx/conf.d/default.conf

exec nginx -g 'daemon off;'
