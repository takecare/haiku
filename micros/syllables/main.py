import requests
import lxml.html
from typing import Dict, List, Optional
from flask import Flask, abort, escape, request
from functools import reduce
from lxml.html import HtmlElement
from requests import Response

from monitoring import Monitor
from store import Data, Store, StoreProvider

BASE_URL = "https://dicionario.priberam.org/"

NOT_FOUND_CSS_CLASS = "alert alert-info"
NOT_FOUND_XPATH = f'.//div[@class="{NOT_FOUND_CSS_CLASS}"]'

SYLLABLES_CSS_CLASS = "verbeteh1"
CONTENT_CSS_CLASS = "pb-main-content"
SYLLABLES_XPATH = f'.//div[@class="{CONTENT_CSS_CLASS}"]//span[@class="{SYLLABLES_CSS_CLASS}"]/h2/span/span'

app = Flask(__name__)
app.debug = True  # TODO read from env
monitor = Monitor(app)
store = StoreProvider().get_store(app)


@app.route("/", methods=["GET"])
def root():
    return ""


# TODO add unit tests

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


class SyllableService:
    def __init__(self, monitor: Monitor) -> None:
        self.monitor = monitor

    @staticmethod
    def _word_not_found(doc: HtmlElement) -> bool:
        not_found = doc.xpath(NOT_FOUND_XPATH)
        return True if len(not_found) > 0 else False

    def fetch(self, word: str) -> Optional[str]:
        html: Response = requests.get(f"{BASE_URL}/{escape(word)}")
        doc: HtmlElement = lxml.html.fromstring(html.content)

        if self._word_not_found(doc):
            return None

        # our xpath captures more than one occurrence, including empty strings
        # e.g. for "palavra": ['pa·la·vra', '', 'pa·la·vra']
        x = doc.xpath(SYLLABLES_XPATH)
        words = [e.text_content() for e in x]
        filtered = list(filter(lambda e: len(e) > 0, list(dict.fromkeys(words))))

        if len(filtered) > 1:
            self.monitor.log(
                f'Got more than one result when querying for "{word}": {filtered}'
            )

        if len(filtered) == 0:
            return None

        return filtered[0]  # select first item as we may get other items


class SyllableRepository:
    def __init__(
        self, syllable_store: Store, syllable_service: SyllableService
    ) -> None:
        self.store = syllable_store
        self.service = syllable_service

    def _read_from_store(self, word: str) -> Optional[Data]:
        return self.store.read(key=word)

    def get_syllables(self, word: str) -> Optional[List[str]]:
        stored = self._read_from_store(word)

        if stored is None:
            result = self.service.fetch(word)
            if result is None:
                return None
            syllables = [syllable.strip() for syllable in result.split("·")]
            stored = {"count": len(syllables), "split": syllables}
            store.write(key=word, obj=stored)

        return stored["split"]


service = SyllableService(monitor)
repository = SyllableRepository(store, service)


@app.route("/word/<word>", methods=["GET"])
def word(word) -> Dict:
    # syllables = _query_word(word)
    syllables = result if (result := repository.get_syllables(word)) is not None else []
    return {"count": len(syllables), "split": syllables}


@app.route("/line/<line>", methods=["GET"])
def line(line) -> Dict:
    words = line.split(",")
    filtered = [word for word in words if len(word) > 0]
    # syllables = [_query_word(word) for word in filtered]
    syllables = [
        result if (result := repository.get_syllables(word)) is not None else []
        for word in filtered
    ]
    return {"count": len(reduce(lambda x, y: x + y, syllables)), "split": syllables}


@app.route("/poem", methods=["POST"])
def poem() -> Dict:
    """
    Expected request body format: { body: ["primeira linha", "segunda linha", ...] }
    """
    lines = request.json["body"]
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
