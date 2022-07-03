from cgitb import reset
from flask import Flask, abort, escape, request
from functools import reduce
import requests
import lxml.html

BASE_URL = "https://dicionario.priberam.org/"

NOT_FOUND_CSS_CLASS = "alert alert-info"
NOT_FOUND_XPATH = f'.//div[@class="{NOT_FOUND_CSS_CLASS}"]'

SYLLABLES_CSS_CLASS = "verbeteh1"
SYLLABLES_XPATH = f'.//span[@class="{SYLLABLES_CSS_CLASS}"]/h2/span/span'

app = Flask(__name__)
app.debug = True  # TODO read from env


@app.route("/", methods=["GET"])
def root():
    return ""


# TODO handle suffixes and prefixes?
# for https://dicionario.priberam.org/ola the first result is a suffix but there
# is a second result that seems valid. we need a way to parse through results
# when there's more than one


def _word_not_found(doc) -> bool:
    not_found = doc.xpath(NOT_FOUND_XPATH)
    return True if len(not_found) > 0 else False


def _query_word(word) -> int:
    # TODO cache words to avoid hitting priberam
    html = requests.get(f"{BASE_URL}/{escape(word)}")
    doc = lxml.html.fromstring(html.content)
    syallables = doc.xpath(SYLLABLES_XPATH)
    if _word_not_found(doc):
        return 0
    return [s.strip() for s in syallables[0].text_content().split("·")]


@app.route("/word/<word>", methods=["GET"])
def word(word):
    syllables = _query_word(word)
    return {"count": len(syllables), "split": syllables}


@app.route("/line/<line>", methods=["GET"])
def line(line):
    words = line.split(",")
    syllables = [_query_word(word) for word in words]
    return {"count": len(reduce(lambda x, y: x + y, syllables)), "split": syllables}


@app.route("/poem", methods=["POST"])
def poem():
    """
    Expected request body format: { body: ["first line", ["second line"], ... }
    """
    lines = request.json["body"]
    syllables = []
    for line in lines:
        words = line.split(" ")
        syllables.append([_query_word(word) for word in words])
    return {
        "count": len(
            reduce(
                lambda x, y: x + y, [reduce(lambda x, y: x + y, s) for s in syllables]
            )
        ),
        "split": syllables,
    }


@app.route("/", methods=["POST"])
def teste():
    return request.json


@app.errorhandler(404)
def page_not_found(error):
    return error, 404
