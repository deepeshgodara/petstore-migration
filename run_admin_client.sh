#!/bin/bash
# ==============================================================================
# Java Pet Store 1.3.1_02 - Pet Store Admin Rich Client Launcher (Convenience)
# ==============================================================================
# Delegates to the authentic Pet Store Admin Swing client script located in
# petstore-legacy/run_admin_client.sh.
# ==============================================================================

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "${DIR}/petstore-legacy/run_admin_client.sh" "$@"
