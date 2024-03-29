## Haiku

A tool to help you write Haikus in 🇵🇹 PT-PT and 🇧🇷 PT-BR.

Note: Portuguese makes heavy usage of characters that are not in the English
alphabet - e.g. "ó", "ç", "é", etc. Writing "olá" or "ónibus" is therefore
different from writing "ola" and "onibus". So, words missing accents where they
are required are fundamentally different from the ones that have them ("ola" vs
"olá"), or they might not even be words at all (like "onibus"). This service is
not yet ready to support this sort of cases, so accents are required.

## Server

The enterity of the infrastructure is on Deta, requiring little to no
intervention on our end.

At the moment we have only one micro/cloud function, named `api`. It
currently requires no authentication whatsoever (although we may be adding
API keys in the future). It is served over HTTPS, managed by Deta.

It's our whole API, providing endpoints to determine the number of syllables in
a word, line or set of lines.

We rely on [gunicorn](https://gunicorn.org/) as our WSGI server in production,
and on [Werkzeug](https://pypi.org/project/Werkzeug/) for local development.

### Local development

As we only have one micro at the moment, these section focus on that single
micro/function. If we deploy more micros in the future they'll likely follow
the same exact setup as our `api` micro.

#### Virtual Environment

We use Python v3.9.13 (this is because Deta Space [only supports versions 3.8
and 3.9](https://deta.space/docs/en/quickstart-guides/python)). We use `pyenv`
to manage Python versions locally (but you don't have to).

- Go to `server/api`
- Run `python3 -m venv .venv` to create the virtual environment
- Activate it: `source .venv/bin/activate`
- Make sure you're picking up the virtual environment: `which python3`,
  `which pip` - these should print out results from your local virtual
  environment

#### Environment variables

We make use of `.env` files to store environment variables. `.env` is used
for local development, whereas `.env.prod` is used to track environment
variables for the production environment.

All this is already setup and you shouldn't need to worry about it, except
making sure you have the `.env.prod` file when deploying the app.

#### Makefile

- Run `make install-dev` to install all dependencies needed for local
  development

`make install` is also available (production dependencies only) but since Deta
only looks at `requirements.txt` you will likely won't ever need to run this
command (unless you're deplyoing to some other target).

- Run `make run` to run the server locally

#### Typechecking

This project uses mypy. Run it with `make typecheck`.

#### Formatting

This project uses black to format code. Run it with `make format`.

### Deployments

We deploy on Deta Space, now that Deta Cloud is deprecated. Deta will look for
dependencies declared in `requirements.txt` so make sure you have this file
(it's already in the repo).

To deploy a micro run `make deploy` in the micro's directory.

### Logging

Deta no longer supports VISOR, after migrating from "Deta Cloud" to
"Deta Spaces". This means there is only one way to check the logs and that is
in the web browser, as such `make logs` will open the Builder UI in a browser.

## Android

TODO

- Coroutines + StateFlow
- ViewModel
- Compose
- How UI is modelled
- Navigation
