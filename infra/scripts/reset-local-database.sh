#!/usr/bin/env bash
set -euo pipefail

readonly expected_confirmation="RESET_PERSONAL_FINANCE_LOCAL"
readonly repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
readonly compose_file="${repository_root}/infra/compose.yml"
readonly compose_project="personal-finance-local"

if [[ $# -ne 0 ]]; then
    echo "This command does not accept a database URL or other arguments." >&2
    exit 2
fi

if [[ "${CONFIRM_LOCAL_RESET:-}" != "${expected_confirmation}" ]]; then
    echo "Refusing to delete the local volume without explicit confirmation." >&2
    echo "Run: CONFIRM_LOCAL_RESET=${expected_confirmation} just local-reset" >&2
    exit 2
fi

readonly docker_context="$(docker context show)"
readonly docker_endpoint="$(docker context inspect "${docker_context}" --format '{{.Endpoints.docker.Host}}')"

case "${docker_endpoint}" in
    unix://* | npipe://*) ;;
    *)
        echo "Refusing reset because Docker context '${docker_context}' is not local." >&2
        exit 2
        ;;
esac

docker compose \
    --project-name "${compose_project}" \
    --file "${compose_file}" \
    down --volumes --remove-orphans

docker compose \
    --project-name "${compose_project}" \
    --file "${compose_file}" \
    up --detach --wait postgres

echo "Local PostgreSQL volume was recreated and the service is ready."
