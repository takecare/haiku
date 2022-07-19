from unittest.mock import ANY, patch

from repository.repository import SyllableRepository


@patch("store.Store")
@patch("service.SyllableService")
def test_reads_stored_word(mock_store, mock_service):
    repository = SyllableRepository(mock_store, mock_service)

    repository.get_syllables("palavra")

    mock_store.read.assert_called_with(key="palavra")


@patch("store.Store")
@patch("service.SyllableService")
def test_fetch_word_if_not_stored(mock_store, mock_service):
    repository = SyllableRepository(mock_store, mock_service)
    mock_store.read.return_value = None

    repository.get_syllables("palavra")

    mock_service.fetch.assert_called_with("palavra")


@patch("store.Store")
@patch("service.SyllableService")
def test_store_word_if_not_stored(mock_store, mock_service):
    repository = SyllableRepository(mock_store, mock_service)
    mock_store.read.return_value = None

    repository.get_syllables("palavra")

    mock_store.write.assert_called_with(key="palavra", obj=ANY)
