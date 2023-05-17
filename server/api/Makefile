setup:
	python3 -m venv venv
	. venv/bin/activate
	python3 -m pip install --upgrade pip
	make install-dev
	make lock

install: requirements.txt
	python3 -m pip install -r requirements.txt

install-dev: requirements_dev.txt
	python3 -m pip install -r requirements_dev.txt
	mypy ./ --install-types --non-interactive || true

lock:
	python3 -m pip freeze > requirements_lock.txt

run:
	. .env.local
	flask run

test:
	python3 -m pytest

typecheck:
	export MYPYPATH=./stubs
	mypy ./

format:
	black ./

deploy:
	deta update -e .env.prod
	space push

logs:
	space open

# catch-all to avoid make printing out errors for commands with parameters
# e.g. "make logs enable"
%:
	@:
