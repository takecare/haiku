from typing import List, Optional
from service import SyllableService
from store import Store


class SyllableRepository:
    def __init__(
        self, syllable_store: Store, syllable_service: SyllableService
    ) -> None:
        self.store = syllable_store
        self.service = syllable_service

    def get_syllables(self, word: str) -> Optional[List[str]]:
        stored = self.store.read(key=word)

        if stored is None:
            result = self.service.fetch(word)
            if result is None:
                return None
            syllables = [syllable.strip() for syllable in result.split("·")]
            stored = {"count": len(syllables), "split": syllables}
            self.store.write(key=word, obj=stored)

        return stored["split"]
