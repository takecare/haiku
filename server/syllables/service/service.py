import requests
import lxml.html
from typing import Optional
from monitoring.monitor import Monitor
from flask import escape
from lxml.html import HtmlElement
from requests import Response

BASE_URL = "https://dicionario.priberam.org/"

NOT_FOUND_CSS_CLASS = "alert alert-info"
NOT_FOUND_XPATH = f'.//div[@class="{NOT_FOUND_CSS_CLASS}"]'

SYLLABLES_CSS_CLASS = "verbeteh1"
CONTENT_CSS_CLASS = "pb-main-content"
SYLLABLES_XPATH = f'.//div[@class="{CONTENT_CSS_CLASS}"]//span[@class="{SYLLABLES_CSS_CLASS}"]/h2/span/span'


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
        words = [e.text_content() for e in doc.xpath(SYLLABLES_XPATH)]
        filtered = list(filter(lambda e: len(e) > 0, list(dict.fromkeys(words))))

        if len(filtered) > 1:
            # if after filtering out repeated occurrences and empty ones we log
            # it so we know we have to improve this
            self.monitor.log(
                f'Got more than one result when querying for "{word}": {filtered}'
            )

        if len(filtered) == 0:
            return None

        return filtered[0]  # select first item as we may get other items
