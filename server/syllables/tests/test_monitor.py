import logging
from flask import Flask
from monitoring import Monitor


def test_monitor_logs(caplog):
    caplog.set_level(logging.DEBUG)

    flask = Flask("test")
    flask.debug = True

    monitor = Monitor(flask_app=flask)
    monitor.log("HELLO!")

    for record in caplog.records:
        assert record.levelname != "CRITICAL"
        assert "HELLO" in caplog.text
