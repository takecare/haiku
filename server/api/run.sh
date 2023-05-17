#!/bin/bash -e

activate () {
  . venv/bin/activate
  . $@
}

activate $@
flask --app main run
