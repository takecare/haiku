from typing import Any
from unittest.mock import patch
from pytest_mock import mocker
from store.store import DetaStore


class MockDeta:
    def __init__(self) -> None:
        print("init!!")
        pass

    class Base:
        def __init__(self, name: str) -> None:
            pass

        def put(self, key: str, obj: Any):
            pass

        def read(self, key: str):
            pass


# @patch("store.store.Deta")
# def test_write(mocked):
def test_write(mocker):
    # TODO mock out Deta
    mock_Deta = mocker.Mock()
    mock_Base = mocker.Mock()

    # mock_Deta.return_value.Base.side_effect = mock_Base
    mock_Deta.return_value.Base.return_value = mock_Base

    mocker.patch("store.store.Deta", mock_Deta)
    # mocker.patch.object(mock_Deta, "Base", mock_Base)
    # mocker.patch("store.store.Deta.Base", mock_Base)

    deta_store = DetaStore(name="debug", project_key="key")
    deta_store.write("key", {"data": "data"})

    mock_Base.put.assert_called()
    # mock_Deta.Base.put.assert_called()
    # mocked.Base.put.assert_called()
