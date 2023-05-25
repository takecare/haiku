MAKEFLAGS += --silent

define load_env_file
    $(eval ENV_FILE := $(1))
    $(eval include $(1))
    $(eval export)
endef

setup:
	python3 -m venv .venv
	. ./.venv/bin/activate
	python3 -m pip install --upgrade pip
	make install-dev

install: requirements.txt
	python3 -m pip install -r requirements.txt

install-dev: requirements_dev.txt
	python3 -m pip install -r requirements_dev.txt
	# true to avoid mypy erroring out if there are type violations
	mypy ./ --install-types --non-interactive || true

lock:
	python3 -m pip freeze > requirements_lock.txt

run:
	$(call load_env_file, .env.prod)
	gunicorn main:app

dev:
	$(call load_env_file, .env)
	flask run

test:
	python3 -m pytest

typecheck:
	export MYPYPATH=./stubs
	mypy ./

format:
	black ./

deploy:
# make sure .env.prod exists before deploying
	@test -f .env.prod
	@space push

logs:
	space open

# catch-all to avoid make printing out errors for commands with parameters
# e.g. "make logs enable"
%:
	@:
