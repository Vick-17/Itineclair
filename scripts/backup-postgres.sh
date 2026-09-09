#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
compose_file="${ITINECLAIR_COMPOSE_FILE:-${project_root}/compose.prod.yaml}"
environment_file="${1:-${ITINECLAIR_ENV_FILE:-${project_root}/.env.production}}"
backup_directory="${2:-${ITINECLAIR_BACKUP_DIR:-${project_root}/backups}}"

if [[ ! -f "${compose_file}" ]]; then
    echo "Fichier Compose introuvable : ${compose_file}" >&2
    exit 1
fi

if [[ ! -f "${environment_file}" ]]; then
    echo "Fichier d’environnement introuvable : ${environment_file}" >&2
    exit 1
fi

mkdir -p -- "${backup_directory}"

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
archive="${backup_directory}/itineclair-${timestamp}.dump"
checksum="${archive}.sha256"

if [[ -e "${archive}" || -e "${checksum}" ]]; then
    echo "Une sauvegarde existe déjà pour ${timestamp}. Réessaie dans une seconde." >&2
    exit 1
fi

temporary_archive="$(mktemp "${backup_directory}/.itineclair-${timestamp}.XXXXXX")"

cleanup() {
    rm -f -- "${temporary_archive}"
}

trap cleanup EXIT

compose=(
    docker compose
    --env-file "${environment_file}"
    --file "${compose_file}"
)

"${compose[@]}" exec -T postgres sh -ceu '
    exec pg_dump \
        --username="$POSTGRES_USER" \
        --dbname="$POSTGRES_DB" \
        --format=custom \
        --no-owner \
        --no-privileges
' > "${temporary_archive}"

if [[ ! -s "${temporary_archive}" ]]; then
    echo "La sauvegarde produite est vide." >&2
    exit 1
fi

"${compose[@]}" exec -T postgres \
    pg_restore --list < "${temporary_archive}" > /dev/null

mv -- "${temporary_archive}" "${archive}"
trap - EXIT

(
    cd -- "${backup_directory}"
    sha256sum -- "$(basename -- "${archive}")" \
        > "$(basename -- "${checksum}")"
)

chmod 600 -- "${archive}" "${checksum}"

echo "Sauvegarde créée et lisible par pg_restore :"
echo "${archive}"
echo "${checksum}"
