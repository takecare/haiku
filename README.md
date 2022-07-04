## Haiku

## Server

At the moment we have only one micro/cloud function. It's our whole API,
providing endpoints to determine the number of syllables in a word, line or
set of lines.

### Local development

#### Venv

- Run `python3 -m venv venv` to create the virtual environment
- Activate it: `source venv/bin/activate`
- Make sure you're picking up the virtual environment: `which python3`,
  `which pip` - these should print out results from your local virtual
  environment

#### Makefile

- Run `make install-dev`

#### Typechecking

This project uses mypy. Run it with `make typecheck`.

### Deployments

We deploy on deta. Deta will look for dependencies declared in
`requirements.txt` so make sure you have this file (it's already in the repo).

To deploy a micro run `make deploy`. This will also update enviroment variables
on deta. These are stored in `.env.prod`.

## Android
