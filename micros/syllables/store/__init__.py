from abc import ABC, abstractmethod
from datetime import date, datetime
from typing import Any, Dict, Optional, Union
from deta import Deta
from flask import Flask

Data = dict[str, Any]


class Store(ABC):
    @abstractmethod
    def write(self, key: str, obj: Any) -> Any:
        pass

    @abstractmethod
    def read(self, key: str) -> Optional[Any]:
        pass


class DetaStore(Store):
    expire_at: datetime = datetime.today().replace(year=datetime.now().year + 1)

    def __init__(self, name: str, project_key: str) -> None:
        self.name = name
        self.deta = Deta(project_key)
        self.db = self.deta.Base(name)

    def write(self, key: str, obj: Data) -> Data:
        # https://docs.deta.sh/docs/base/sdk#put
        return self.db.put(
            key=key,
            data=obj,
            # expire_at=self.expire_at
        )

    def read(self, key: str) -> Optional[Data]:
        return self.db.get(key)


class DebugStore(Store):
    name: str
    store: Dict[str, Data]

    def __init__(self, name: str = "Debug") -> None:
        self.name = name
        self.store = {}

    def write(self, key: str, obj: Data) -> Data:
        obj["key"] = key
        self.store[key] = obj
        return obj

    def read(self, key: str) -> Optional[Data]:
        try:
            return self.store[key]
        except KeyError:
            return None


class StoreProvider:
    def get_store(self, flask_app: Flask) -> Store:
        return (
            DebugStore()
            if flask_app.debug
            else DetaStore(name=flask_app.name, project_key="")  # TODO
        )
