#!/bin/sh
set -eu

# Named volumes are initially owned by root. Give the development user access
# before dropping privileges; source files remain owned according to the bind mount.
mkdir -p /home/dev/.m2 /home/dev/.npm /workspace/web/node_modules
chown -R dev:dev /home/dev/.m2 /home/dev/.npm /workspace/web/node_modules

exec gosu dev "$@"
