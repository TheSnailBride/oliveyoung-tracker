import sys
import types
import unittest


sys.modules.setdefault("requests", types.SimpleNamespace())
sys.modules.setdefault(
    "scrapling",
    types.SimpleNamespace(StealthyFetcher=types.SimpleNamespace(fetch=None)),
)

import scraper


class ScraperCategoryMergeTest(unittest.TestCase):
    def test_dermo_category_replaces_existing_general_category(self):
        collected = {
            "A001": {
                "oliveYoungId": "A001",
                "category": "스킨/토너",
                "name": "일반 카테고리 상품",
            }
        }

        scraper.merge_collected_product(collected, {
            "oliveYoungId": "A001",
            "category": "더모_스킨케어",
            "name": "더모 카테고리 상품",
        })

        self.assertEqual(collected["A001"]["category"], "더모_스킨케어")
        self.assertEqual(collected["A001"]["name"], "더모 카테고리 상품")

    def test_general_category_does_not_replace_existing_dermo_category(self):
        collected = {
            "A001": {
                "oliveYoungId": "A001",
                "category": "더모_스킨케어",
                "name": "더모 카테고리 상품",
            }
        }

        scraper.merge_collected_product(collected, {
            "oliveYoungId": "A001",
            "category": "스킨/토너",
            "name": "일반 카테고리 상품",
        })

        self.assertEqual(collected["A001"]["category"], "더모_스킨케어")
        self.assertEqual(collected["A001"]["name"], "더모 카테고리 상품")


if __name__ == "__main__":
    unittest.main()
