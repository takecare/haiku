from datetime import datetime
from typing import Union

# https://mypy.readthedocs.io/en/stable/stubs.html#stub-files
# these stubs are based on the source for deta version 1.1.0

class Deta:
    def __init__(self, project_key: str = None, *, project_id: str = None): ...

    class Base:
        def __init__(self, name: str, host: str = None): ...
        def get(self, key: str): ...
        def put(
            self,
            data: Union[dict, list, str, int, bool],
            key: str = None,
            *,
            expire_in: int = None,
            expire_at: Union[int, float, datetime] = None,
        ): ...
