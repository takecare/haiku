from main import SyllableService
import lxml.html

from unittest.mock import patch


@patch("monitoring.monitor.Monitor")
@patch("requests.get")
def test_fetch_word(mock_monitor, mock_get, mocker):
    service = SyllableService(mock_monitor)
    mock_syllables_element = mocker.Mock()
    mock_syllables_element.text_content.return_value = "pa·la·vra"
    mock_html_element = mocker.Mock()
    mock_html_element.xpath.side_effect = [[], [mock_syllables_element]]

    with patch("lxml.html.fromstring", return_value=mock_html_element):
        result = service.fetch("palavra")
        assert result == "pa·la·vra"


@patch("monitoring.monitor.Monitor")
@patch("requests.get")
def test_fetch_word_no_words_fetched(mock_monitor, mock_get, mocker):
    service = SyllableService(mock_monitor)
    mock_html_element = mocker.Mock()
    mock_html_element.xpath.side_effect = [[], []]

    with patch("lxml.html.fromstring", return_value=mock_html_element):
        result = service.fetch("palavra")
        assert result is None


@patch("monitoring.monitor.Monitor")
@patch("requests.get")
def test_fetch_word_not_found(mock_monitor, mock_get, mocker):
    service = SyllableService(mock_monitor)
    mock_not_found_element = mocker.Mock()
    mock_html_element = mocker.Mock()
    mock_html_element.xpath.return_value = [mock_not_found_element]

    with patch("lxml.html.fromstring", return_value=mock_html_element) as fromstring:
        result = service.fetch("palavra")
        assert result == None
