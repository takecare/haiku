from flask import Flask


class Monitor:
    def __init__(self, flask_app: Flask) -> None:
        self.flask_app = flask_app

    def log(self, message: str):
        if self.flask_app.debug:
            print(message)
