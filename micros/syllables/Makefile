install: requirements.txt
	python3 -m pip install -r requirements.txt

install-dev: requirements_dev.txt
	python3 -m pip install -r requirements_dev.txt
	mypy --install-types

run:
	flask run

test:
	python3 -m pytest

typecheck:
	mypy *.py

format:
	black ./

deploy:
	deta update -e .env.prod
	deta deploy

logs: # enable, disable, open
	deta visor $(filter-out $@,$(MAKECMDGOALS))

# catch-all to avoid make printing out errors for commands with parameters
# e.g. "make logs enable"
%:
	@:
