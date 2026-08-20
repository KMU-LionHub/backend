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

GOOGLE_ADC_PATH_VALUE="$(sed -n 's/^GOOGLE_ADC_PATH=//p' "${ENV_FILE}" | tail -n 1)"
GOOGLE_ADC_PATH_VALUE="${GOOGLE_ADC_PATH_VALUE%$'\r'}"

if [[ -n "${GOOGLE_ADC_PATH_VALUE}" ]]; then
  if [[ "${GOOGLE_ADC_PATH_VALUE}" != /* ]]; then
    echo "GOOGLE_ADC_PATH must be an unquoted absolute path." >&2
    exit 1
  fi

  if [[ ! -f "${GOOGLE_ADC_PATH_VALUE}" ]]; then
    echo "Google ADC credential file not found: ${GOOGLE_ADC_PATH_VALUE}" >&2
    exit 1
  fi

  if [[ ! -r "${GOOGLE_ADC_PATH_VALUE}" ]]; then
    echo "Google ADC credential file is not readable by the deployment user: ${GOOGLE_ADC_PATH_VALUE}" >&2
    exit 1
  fi

  if [[ ! -s "${GOOGLE_ADC_PATH_VALUE}" ]]; then
    echo "Google ADC credential file is empty: ${GOOGLE_ADC_PATH_VALUE}" >&2
    exit 1
  fi

  GOOGLE_ADC_PATH_VALUE="$(realpath -e -- "${GOOGLE_ADC_PATH_VALUE}")"
  GOOGLE_ADC_PATH="${GOOGLE_ADC_PATH_VALUE}"
  GOOGLE_ADC_RUNTIME_UID="$(stat -Lc '%u' "${GOOGLE_ADC_PATH_VALUE}")"
  GOOGLE_ADC_RUNTIME_GID="$(stat -Lc '%g' "${GOOGLE_ADC_PATH_VALUE}")"
  if [[ "${GOOGLE_ADC_RUNTIME_UID}" == "0" ]]; then
    echo "Google ADC credential file must be owned by a non-root user." >&2
    exit 1
  fi
  GOOGLE_ADC_RUNTIME_USER="${GOOGLE_ADC_RUNTIME_UID}:${GOOGLE_ADC_RUNTIME_GID}"
  export GOOGLE_ADC_PATH GOOGLE_ADC_RUNTIME_USER

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

if [[ -n "${GOOGLE_ADC_PATH_VALUE}" ]] \
    && ! docker compose "${COMPOSE_ARGUMENTS[@]}" exec -T backend \
      sh -c 'test -r "$GOOGLE_APPLICATION_CREDENTIALS"'; then
  echo "Google ADC credential is not readable inside the backend container." >&2
  exit 1
fi

echo "Deployed backend commit ${BACKEND_IMAGE_TAG}."
