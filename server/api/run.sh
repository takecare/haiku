#!/bin/bash -e

activate () {
  . .venv/bin/activate
  . $@
}

activate $@
echo "> flask env: $FLASK_ENV"
flask --app main run
# make dev
