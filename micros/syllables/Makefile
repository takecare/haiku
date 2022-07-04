install: requirements.txt
	python3 -m pip install -r requirements.txt

install-dev: requirements_dev.txt
	python3 -m pip install -r requirements_dev.txt
	mypy --install-types

run:
	flask run

deploy:
	deta deploy

typecheck:
	mypy *.py
