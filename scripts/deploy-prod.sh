#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIRECTORY="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIRECTORY="$(cd -- "${SCRIPT_DIRECTORY}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${PROJECT_DIRECTORY}/.env.prod}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Production environment file not found: ${ENV_FILE}" >&2
  exit 1
fi

cd "${PROJECT_DIRECTORY}"

BACKEND_IMAGE_TAG="${BACKEND_IMAGE_TAG:-$(git rev-parse HEAD)}"
if [[ ! "${BACKEND_IMAGE_TAG}" =~ ^[0-9a-f]{40}$ ]]; then
  echo "BACKEND_IMAGE_TAG must be a full 40-character Git commit SHA." >&2
  exit 1
fi
export BACKEND_IMAGE_TAG

COMPOSE_ARGUMENTS=(
  -f compose.yaml
  -f compose.prod.yaml
)

if grep -Eq '^GOOGLE_ADC_PATH=.+$' "${ENV_FILE}"; then
  COMPOSE_ARGUMENTS+=(
    -f compose.google-adc.yaml
  )
fi

COMPOSE_ARGUMENTS+=(
  --env-file "${ENV_FILE}"
)

docker compose "${COMPOSE_ARGUMENTS[@]}" config --quiet

if [[ "${DEPLOY_DRY_RUN:-false}" == "true" ]]; then
  echo "Production Compose configuration is valid for commit ${BACKEND_IMAGE_TAG}."
  exit 0
fi

docker compose "${COMPOSE_ARGUMENTS[@]}" pull backend
docker compose "${COMPOSE_ARGUMENTS[@]}" up -d --remove-orphans

echo "Deployed backend commit ${BACKEND_IMAGE_TAG}."
