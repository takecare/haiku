from flask import Flask
from .store import DebugStore, DetaStore, Store


class StoreProvider:
    def get_store(self, flask_app: Flask) -> Store:
        return (
            DebugStore()
            if flask_app.debug
            else DetaStore(name=flask_app.name, project_key="")  # TODO key from env
        )
