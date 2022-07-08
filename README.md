## Haiku

## Server

The enterity of the infrastructure is on Deta, requiring little to no
intervention on our end.

At the moment we have only one micro/cloud function, named `syllables`. At the
moment it requires no authentication whatsoever (although we may be adding
API keys in the future). It is served over HTTPS, managed by Deta.

It's our whole API, providing endpoints to determine the number of syllables in
a word, line or set of lines.

### Local development

#### Venv

- Run `python3 -m venv venv` to create the virtual environment
- Activate it: `source venv/bin/activate`
- Make sure you're picking up the virtual environment: `which python3`,
  `which pip` - these should print out results from your local virtual
  environment

#### Environment variables

We make use of `.env` files to store environment variables. `.env` is used
for local development, whereas `.env.prod` is used to track environment
variables for the production environment.

#### Makefile

All the make commands you see referenced here are relative to the one function
described above.

- Run `make install-dev` to install all dependencies needed for local
  development

`make install` is also available (production dependencies only) but since Deta
only looks at `requirements.txt` you will likely won't ever need to run this
command (unless you're deplyoing to some other target).

- Run `make run` to run the server locally

#### Typechecking

This project uses mypy. Run it with `make typecheck`.

### Deployments

We deploy on Deta. Deta will look for dependencies declared in
`requirements.txt` so make sure you have this file (it's already in the repo).

To deploy a micro run `make deploy` in the micro's directory. This will also
update enviroment variables on Deta - these are stored in `.env.prod`.

### Logging

We rely on Deta's logging (aka "visor").

- Run `make logs enable` to enable visor (`make logs disable` to disable them).

- Run `make logs open` to open the logs in your default web browser.

## Android

TODO

- Coroutines + StateFlow
- ViewModel
- Compose
- How UI is modelled
- Navigation
