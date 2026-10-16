import copy
import json
from pathlib import Path
import sys
import unittest
import tempfile
import shutil
import hashlib
from fractions import Fraction

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from audit_parameterized_content import audit, audit_variant
from question_package import fingerprint

DIRECTORY = Path(__file__).resolve().parents[2] / 'editorial/parameterized-questions/2026-10'


class ParameterizedContentAuditTests(unittest.TestCase):
    def test_partial_pack_reports_missing_family_and_cannot_claim_complete(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            for name in ['direct-proportion.json', 'direct-proportion-preview.json', 'percentage.json', 'percentage-preview.json', 'bases.json']:
                shutil.copyfile(DIRECTORY / name, directory / name)
            summary, report = audit(directory, allow_partial=True)
            self.assertEqual(summary['validated'], 84)
            self.assertEqual(summary['missingFamilies'], ['SIMPLE_INTEREST'])
            self.assertEqual(summary['editorialApproval'], 'NOT_GRANTED_BY_AUDIT')
            self.assertIn('não concede aprovação editorial', report)
            with self.assertRaisesRegex(ValueError, 'Famílias pendentes'):
                audit(directory)

    def test_all_111_prepared_but_draft_base_cannot_pass_as_eligible(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            for file in DIRECTORY.glob('*.json'):
                shutil.copyfile(file, directory / file.name)
            bases = json.loads((directory / 'bases.json').read_text(encoding='utf-8'))
            base = next(b for b in bases if b['family'] == 'SIMPLE_INTEREST')
            base['status'] = 'DRAFT'; base['eligible_revision_id'] = None
            (directory / 'bases.json').write_text(json.dumps(bases), encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'Base pendente de aprovação editorial'):
                audit(directory)
            summary, report = audit(directory, allow_pending_bases=True)
            self.assertEqual(summary['validated'], 111)
            self.assertEqual(summary['missingFamilies'], [])
            self.assertEqual(summary['pendingBaseFamilies'], ['SIMPLE_INTEREST'])
            self.assertFalse(summary['allBasesEligible'])
            self.assertEqual(summary['editorialApproval'], 'NOT_GRANTED_BY_AUDIT')
            self.assertIn('NÃO ELEGÍVEL', report)

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
        summary, report = audit(DIRECTORY)
        self.assertEqual(summary, json.loads((DIRECTORY / 'audit.json').read_text(encoding='utf-8')))
        self.assertEqual(report, (DIRECTORY / 'variants.md').read_text(encoding='utf-8'))

    def test_pending_selection_binds_exact_base_and_three_template_manifests(self):
        plan_file = DIRECTORY / 'approval-selection.json'
        plan = json.loads(plan_file.read_text(encoding='utf-8'))
        self.assertEqual(plan['status'], 'PENDING_USER_DECISION')
        self.assertTrue(plan['requiresExplicitApproval'])
        self.assertEqual(len(plan['templates']), 3)
        self.assertEqual(sum(t['variants'] for t in plan['templates']), 111)
        self.assertIn(hashlib.sha256(plan_file.read_bytes()).hexdigest(), (DIRECTORY / 'content-approval.md').read_text(encoding='utf-8'))
        base = plan['newBase']; package_file = DIRECTORY / base['packageFile']
        self.assertEqual(base['packageSha256'], hashlib.sha256(package_file.read_bytes()).hexdigest())
        package = json.loads(package_file.read_text(encoding='utf-8'))
        self.assertEqual(len(package['items']), 1)
        self.assertEqual(package['items'][0]['origin']['key'], 'M04-Q05')
        self.assertEqual(package['items'][0]['fingerprint'], base['fingerprint'])
        for t in plan['templates']:
            self.assertEqual(t['fileSha256'], hashlib.sha256((DIRECTORY / t['definitionFile']).read_bytes()).hexdigest())
            preview = json.loads((DIRECTORY / t['definitionFile'].replace('.json', '-preview.json')).read_text(encoding='utf-8'))
            for field in ['definitionDigest', 'domainDigest', 'manifestDigest']:
                self.assertEqual(t[field], preview[field])

    def test_real_base_package_is_exact_and_preparation_never_claims_approval(self):
        pack = json.loads((DIRECTORY / 'base-m04-q05-package.json').read_text(encoding='utf-8'))
        item = pack['items'][0]; content = item['content']
        expected = sum(Fraction(2000, 100) * 3 for _ in range(4))
        correct = next(o for o in content['alternatives'] if o['id'] == content['correctAlternativeId'])
        self.assertEqual(correct['content']['text'], f'R$ {expected},00')
        self.assertEqual(fingerprint(content), item['fingerprint'])
        evidence = json.loads((DIRECTORY / 'base-preparation.json').read_text(encoding='utf-8'))
        self.assertEqual(evidence['fingerprint'], item['fingerprint'])
        self.assertEqual(evidence['status'], 'DRAFT'); self.assertFalse(evidence['eligible'])
        self.assertTrue(evidence['reimportReused']); self.assertEqual(evidence['decisionsForNewBase'], 0)
        self.assertEqual(evidence['sourceApprovedBefore'], evidence['sourceApprovedAfter'])
        self.assertEqual(evidence['isolatedApprovedBefore'], evidence['isolatedApprovedAfter'])

    def test_recorded_approval_matches_selection_and_probe_cannot_change_approved_target(self):
        plan = json.loads((DIRECTORY / 'approval-selection.json').read_text(encoding='utf-8'))
        release = json.loads((DIRECTORY / 'eligibility-after.json').read_text(encoding='utf-8'))
        probe = json.loads((DIRECTORY / 'withdrawal-probe.json').read_text(encoding='utf-8'))
        self.assertEqual(release['approvalSelectionSha256'], hashlib.sha256((DIRECTORY / 'approval-selection.json').read_bytes()).hexdigest())
        self.assertEqual(release['eligibleTemplates'], 3); self.assertEqual(release['templateDecisions'], 6)
        self.assertEqual(release['base']['status'], 'APPROVED'); self.assertTrue(release['templateReplayUnchanged'])
        for t in plan['templates']:
            row = next(r for r in release['families'] if r['kind'] == t['family'])
            self.assertEqual(row['eligible_version'], 1); self.assertEqual(row['status'], 'APPROVED')
            for field, key in [('definition_digest', 'definitionDigest'), ('domain_digest', 'domainDigest'), ('manifest_digest', 'manifestDigest')]:
                self.assertEqual(row[field], t[key])
            for action in ['SUBMIT', 'APPROVE']:
                decision = json.loads((DIRECTORY / 'decisions' / (t['family'] + '-' + action + '.json')).read_text(encoding='utf-8'))['decision']
                self.assertEqual(decision['action'], action); self.assertEqual(decision['manifestDigest'], t['manifestDigest'])
                self.assertEqual(set(decision['checklist']), set(plan['templateChecklist']))
        self.assertEqual(probe['approvedCopyBefore'], probe['approvedCopyAfter'])
        self.assertEqual(probe['approvedCopyAfter']['eligible'], 3)
        self.assertEqual(probe['probeAfterWithdrawal']['eligible'], 0)
        self.assertEqual(probe['probeAfterRegistrationAndApprovalReplay']['eligible'], 0)


if __name__ == '__main__':
    unittest.main()
