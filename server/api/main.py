from typing import Dict
from flask import Flask, abort, current_app, g, request
from functools import reduce

from monitoring import Monitor
from repository import SyllableRepository
from service.service import SyllableService
from store import StoreProvider


def create_app():
    app = Flask(__name__)

    # this is not needed as flask picks up on the env var FLASK_DEBUG
    with app.app_context():
        app.debug = True  # TODO read from env

    return app


app = create_app()
monitor = Monitor(app)
store = StoreProvider().get_store(app)
service = SyllableService(monitor)
repository = SyllableRepository(store, service)


@app.route("/", methods=["GET"])
def root():
    return {"debug": app.debug}


# TODO handle suffixes and prefixes?
# for https://dicionario.priberam.org/ola the first result is a suffix but there
# is a second result that seems valid. we need a way to parse through results
# when there's more than one

# TODO consider returning unrecognised words
# returning unrecognised words will allow the frontend to signal them to the
# user so they can understand what's happening - e.g. if user types "amigoz",
# it is an urecognised word but it is also a typo. the backend can return it
# so it is flagged on the frontend

# TODO consider returning suggestions
# when querying for some words priberam returns suggestions (e.g. "onibus" and
# "amigoz"). we should return a list of suggestions for each unrecognised word
# that has them


@app.route("/word/<word>", methods=["GET"])
def word(word) -> Dict:
    syllables = result if (result := repository.get_syllables(word)) is not None else []
    return {"count": len(syllables), "split": syllables}


@app.route("/line/<line>", methods=["GET"])
def line(line) -> Dict:
    words = line.split(",")
    filtered = [word for word in words if len(word) > 0]
    syllables = [
        result if (result := repository.get_syllables(word)) is not None else []
        for word in filtered
    ]
    return {"count": len(reduce(lambda x, y: x + y, syllables)), "split": syllables}


@app.route("/poem", methods=["POST"])
def poem() -> Dict:
    """
    Expected request body format:
     { body: ["primeira linha", "segunda linha", ...] }
    """
    lines = request.json["body"]  # type: ignore
    poem_syllables: list[list[list[str]]] = []

    # determine all syllables in all lines of the poem, keeping the correct number of lines
    for line in lines:
        words_in_line = line.split(" ")
        line_syllables = [
            syllables
            for word in words_in_line
            if len(word) > 0
            and len(
                syllables := result
                if (result := repository.get_syllables(word)) is not None
                else []
            )
            > 0
        ]
        poem_syllables.append(line_syllables)

    initial: list[str] = []
    initial_line: list[str] = []
    return {
        "count": len(
            reduce(
                lambda acc, flattened_line: acc + flattened_line,
                [
                    reduce(lambda acc, s: acc + s, line, initial_line)
                    for line in poem_syllables
                ],
                initial,
            )
        ),
        "split": poem_syllables,
    }


@app.errorhandler(404)
def page_not_found(error):
    return error, 404
