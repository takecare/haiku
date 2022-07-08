install: requirements.txt
	python3 -m pip install -r requirements.txt

install-dev: requirements_dev.txt
	python3 -m pip install -r requirements_dev.txt
	mypy --install-types

run:
	flask run

typecheck:
	mypy *.py

deploy:
	deta update -e .env.prod
	deta deploy

logs: # enable, disable, open
	deta visor $(filter-out $@,$(MAKECMDGOALS))

%:
	@:
