from flask import Flask


class Monitor:
    def __init__(self, flask_app: Flask) -> None:
        self.flask_app = flask_app

    def log(self, message: str):
        if self.flask_app.debug:
            print(message)


# TODO proper monitoring (https://docs.deta.sh/docs/micros/visor)
# maybe consider deta base to store logs and a cron to clear them?