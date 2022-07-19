from abc import ABC, abstractmethod
from datetime import datetime
from typing import Any, Dict, Optional

# TODO https://mypy.readthedocs.io/en/stable/stubs.html#stub-files
from deta import Deta

Data = Dict[str, Any]


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
