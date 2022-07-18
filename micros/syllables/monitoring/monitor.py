from typing import Optional, Union
from unicodedata import name
from flask import Flask
import logging  # https://docs.python.org/3/howto/logging.html

Log = Union[str, Exception]


class Monitor:
    name: str
    debug: bool

    def __init__(
        self,
        flask_app: Optional[Flask] = None,
        name: Optional[str] = None,
        debug: Optional[bool] = None,
    ) -> None:
        """If provided, flask_app takes precedence over any other parameters."""
        tmp_name = flask_app.name if flask_app is not None else "root"
        tmp_debug = debug if debug is not None else False
        self.debug = flask_app.debug if flask_app is not None else tmp_debug
        self.name = name if name is not None else tmp_name
        logging.basicConfig(
            format="[%(asctime)s][%(name)s][%(levelname)s] %(message)s",
            datefmt="%m/%d/%Y %I:%M:%S",
            level=logging.DEBUG,
        )
        self.logger = logging.getLogger(self.name)

    def log(self, message: Log):
        if not self.debug:
            pass
        # FIXME can log exceptions just fine. these checks are not needed
        self.logger.debug(message if message is str else message)

    def warn(self, message: Log):
        if not self.debug:
            pass
        self.logger.warn(message if message is str else message)

    def error(self, message: Log):
        if not self.debug:
            pass
        self.logger.error(message if message is str else message)


# TODO proper monitoring (https://docs.deta.sh/docs/micros/visor)
# maybe consider deta base to store logs and a cron to clear them?
