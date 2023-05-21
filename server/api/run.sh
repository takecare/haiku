#!/bin/bash -e

activate () {
  . .venv/bin/activate
  . $@
}

activate $@
flask run # flask --app main run
