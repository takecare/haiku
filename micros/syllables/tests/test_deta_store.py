from typing import Any
from unittest.mock import patch
from pytest_mock import mocker
from store.store import DetaStore


# https://docs.python.org/3/library/unittest.mock.html#patch-object
@patch("store.store.Deta")
def test_write(mock_Deta, mocker):
    mock_Base = mocker.Mock()

    mock_Deta.return_value.Base.return_value = mock_Base
    # mocker.patch("store.store.Deta", mock_Deta)

    deta_store = DetaStore(name="debug", project_key="key")
    deta_store.write("key", {"data": "data"})

    mock_Base.put.assert_called()


@patch("store.store.Deta")
def test_read(mock_Deta, mocker):
    mock_Base = mocker.Mock()
    mock_Base.get.return_value = {"key": "value"}
    mock_Deta.return_value.Base.return_value = mock_Base

    deta_store = DetaStore(name="debug", project_key="key")
    data = deta_store.read("key")

    mock_Base.get.assert_called()
    assert data == {"key": "value"}
