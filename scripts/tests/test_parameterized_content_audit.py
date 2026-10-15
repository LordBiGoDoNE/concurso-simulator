import copy
import json
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from audit_parameterized_content import audit, audit_variant

DIRECTORY = Path(__file__).resolve().parents[2] / 'editorial/parameterized-questions/2026-10'


class ParameterizedContentAuditTests(unittest.TestCase):
    def test_partial_pack_reports_missing_family_and_cannot_claim_complete(self):
        summary, report = audit(DIRECTORY, allow_partial=True)
        self.assertEqual(summary['validated'], 84)
        self.assertEqual(summary['missingFamilies'], ['SIMPLE_INTEREST'])
        self.assertEqual(summary['editorialApproval'], 'PENDING')
        self.assertIn('Aprovação editorial PENDENTE', report)
        with self.assertRaisesRegex(ValueError, 'Famílias pendentes'):
            audit(DIRECTORY)

    def test_independent_oracle_rejects_changed_answer_and_duplicate_options(self):
        preview = json.loads((DIRECTORY / 'percentage-preview.json').read_text(encoding='utf-8'))
        original = preview['variants'][0]
        changed = copy.deepcopy(original); changed['correctIndex'] = 1
        with self.assertRaisesRegex(ValueError, 'oráculo independente'):
            audit_variant(changed)
        changed = copy.deepcopy(original); changed['alternatives'][1] = changed['alternatives'][0]
        with self.assertRaisesRegex(ValueError, 'Alternativas inválidas'):
            audit_variant(changed)

    def test_closed_domain_rejects_extra_keys_floats_and_numeric_strings(self):
        preview = json.loads((DIRECTORY / 'direct-proportion-preview.json').read_text(encoding='utf-8'))
        original = preview['variants'][0]
        for value in [2.0, '2', True, 100]:
            changed = copy.deepcopy(original); changed['parameters']['values']['a'] = value
            with self.assertRaises(ValueError):
                audit_variant(changed)
        changed = copy.deepcopy(original); changed['parameters']['values']['seed'] = 1
        with self.assertRaises(ValueError):
            audit_variant(changed)

    def test_versioned_report_matches_all_prepared_variants_without_approval(self):
        summary, report = audit(DIRECTORY, allow_partial=True)
        self.assertEqual(summary, json.loads((DIRECTORY / 'audit.json').read_text(encoding='utf-8')))
        self.assertEqual(report, (DIRECTORY / 'variants.md').read_text(encoding='utf-8'))


if __name__ == '__main__':
    unittest.main()
