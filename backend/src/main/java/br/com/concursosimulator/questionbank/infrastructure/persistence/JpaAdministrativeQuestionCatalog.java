package br.com.concursosimulator.questionbank.infrastructure.persistence;

import br.com.concursosimulator.questionbank.application.AdministrativeQuestionQueries.Criteria;
import br.com.concursosimulator.questionbank.application.port.AdministrativeQuestionCatalog;
import br.com.concursosimulator.questionbank.domain.*;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Projeções administrativas privadas (#177). SQL nativo confinado a esta porta, com parâmetros
 * vinculados; leitura repeatable-read sem travas. O agregado completo reutiliza a validação de carga
 * do {@link JpaQuestionStore}.
 */
public final class JpaAdministrativeQuestionCatalog implements AdministrativeQuestionCatalog {
    private final EntityManager entities;
    private final TransactionTemplate reads;
    private final JpaQuestionStore store;

    public JpaAdministrativeQuestionCatalog(EntityManager entities, PlatformTransactionManager manager, JpaQuestionStore store) {
        this.entities = entities; this.store = store; reads = new TransactionTemplate(manager);
        reads.setReadOnly(true); reads.setTimeout(15);
        reads.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        reads.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    private static final String FROM = """
             from qb_question q
             join lateral (select r.id, r.revision_number, r.status, r.fingerprint, r.discipline_id, r.topic_id
                             from qb_revision r where r.question_id=q.id order by r.revision_number desc limit 1) l on true
             left join qb_revision e on e.id=q.eligible_revision_id
             left join lateral (select d.review_kind from qb_decision d
                                 where d.question_id=q.id and d.revision_id=q.eligible_revision_id and d.action='APPROVE'
                                 order by d.sequence desc limit 1) k on true
             left join lateral (select count(*) as n, max(d.decided_at) as at from qb_decision d where d.question_id=q.id) s on true
             left join lateral (select max(rc.committed_at) as at from qb_import_revision ir
                                 join qb_import_receipt rc on rc.nonce=ir.nonce where ir.question_id=q.id) i on true
            """;
    private static final String ACTIVITY = "greatest(s.at, i.at)";

    @Override public Page search(Criteria c) {
        var where = new StringBuilder(" where true");
        var parameters = new LinkedHashMap<String, Object>();
        if (c.purpose() != null) where.append(c.purpose() == QuestionPurpose.PRATICA_AULA ? " and q.origin_namespace=:practice" : " and q.origin_namespace<>:practice");
        if (c.purpose() != null) parameters.put("practice", QuestionPurpose.PRACTICE_NAMESPACE);
        if (c.namespace() != null) { where.append(" and q.origin_namespace=:namespace"); parameters.put("namespace", c.namespace()); }
        if (c.keyPrefix() != null) { where.append(" and left(q.origin_key, :prefixLength)=:prefix"); parameters.put("prefix", c.keyPrefix()); parameters.put("prefixLength", c.keyPrefix().length()); }
        if (c.disciplineId() != null) { where.append(" and l.discipline_id=:discipline"); parameters.put("discipline", c.disciplineId()); }
        if (c.topicId() != null) { where.append(" and l.topic_id=:topic"); parameters.put("topic", c.topicId()); }
        if (c.latestStatus() != null) { where.append(" and l.status=:status"); parameters.put("status", c.latestStatus().name()); }
        if (c.hasEligible() != null) where.append(c.hasEligible() ? " and q.eligible_revision_id is not null" : " and q.eligible_revision_id is null");
        if (c.eligibleReviewKind() != null) { where.append(" and q.eligible_revision_id is not null and k.review_kind=:kind"); parameters.put("kind", c.eligibleReviewKind().name()); }
        if (c.activityFrom() != null) { where.append(" and " + ACTIVITY + ">=:from"); parameters.put("from", OffsetDateTime.ofInstant(c.activityFrom(), java.time.ZoneOffset.UTC)); }
        if (c.activityTo() != null) { where.append(" and " + ACTIVITY + "<=:to"); parameters.put("to", OffsetDateTime.ofInstant(c.activityTo(), java.time.ZoneOffset.UTC)); }
        return reads.execute(status -> {
            var count = entities.createNativeQuery("select count(*)" + FROM + where, Long.class);
            parameters.forEach(count::setParameter);
            long total = ((Number) count.getSingleResult()).longValue();
            var page = entities.createNativeQuery("select q.id, q.origin_namespace, q.origin_key, s.n, l.id, l.revision_number, l.status, l.fingerprint, "
                    + "l.discipline_id, l.topic_id, e.id, e.fingerprint, k.review_kind, " + ACTIVITY + FROM + where
                    + " order by q.origin_namespace, q.origin_key, q.id limit :limit offset :offset");
            parameters.forEach(page::setParameter);
            page.setParameter("limit", c.size()).setParameter("offset", (long) c.page() * c.size());
            var items = new ArrayList<Summary>();
            for (var raw : page.getResultList()) {
                var row = (Object[]) raw;
                var eligible = row[10] == null ? null : new EligibleRevision(uuid(row[10]), (String) row[11],
                        row[12] == null ? null : EditorialDecision.ReviewKind.valueOf((String) row[12]));
                items.add(new Summary(uuid(row[0]), new QuestionOrigin((String) row[1], (String) row[2]), ((Number) row[3]).intValue(),
                        new LatestRevision(uuid(row[4]), ((Number) row[5]).intValue(), Question.Status.valueOf((String) row[6]), (String) row[7], (String) row[8], (String) row[9]),
                        eligible, instant(row[13])));
            }
            return new Page(items, total);
        });
    }

    @Override public Optional<Question> question(UUID questionId) {
        return store.read(questionId);
    }

    @Override public List<ImportLink> imports(UUID questionId) {
        return reads.execute(status -> {
            var links = new ArrayList<ImportLink>();
            for (var raw : entities.createNativeQuery("select rc.nonce, rc.actor, rc.destination, rc.committed_at, ir.revision_id, ir.reused "
                    + "from qb_import_revision ir join qb_import_receipt rc on rc.nonce=ir.nonce where ir.question_id=:id order by rc.committed_at, rc.nonce")
                    .setParameter("id", questionId).getResultList()) {
                var row = (Object[]) raw;
                links.add(new ImportLink(uuid(row[0]), uuid(row[1]), (String) row[2], instant(row[3]), uuid(row[4]), (Boolean) row[5]));
            }
            return List.copyOf(links);
        });
    }

    @Override public Optional<List<ImportedRevision>> receipt(UUID receiptId) {
        return reads.execute(status -> {
            var exists = ((Number) entities.createNativeQuery("select count(*) from qb_import_receipt where nonce=:nonce", Long.class)
                    .setParameter("nonce", receiptId).getSingleResult()).longValue() > 0;
            if (!exists) return Optional.<List<ImportedRevision>>empty();
            var revisions = new ArrayList<ImportedRevision>();
            for (var raw : entities.createNativeQuery("select question_id, revision_id from qb_import_revision where nonce=:nonce order by question_id")
                    .setParameter("nonce", receiptId).getResultList()) {
                var row = (Object[]) raw;
                revisions.add(new ImportedRevision(uuid(row[0]), uuid(row[1])));
            }
            return Optional.of(List.copyOf(revisions));
        });
    }

    @Override public Optional<StoredFile> file(String path, String sha256) {
        return reads.execute(status -> entities.createNativeQuery("select f.media_type, b.content from qb_import_file f "
                        + "join qb_import_blob b on b.sha256=f.sha256 where f.path=:path and f.sha256=:hash order by f.nonce limit 1")
                .setParameter("path", path).setParameter("hash", sha256).getResultStream().findFirst()
                .map(raw -> { var row = (Object[]) raw; return new StoredFile(path, (String) row[0], sha256, (byte[]) row[1]); }));
    }

    private static UUID uuid(Object value) { return value instanceof UUID id ? id : UUID.fromString(value.toString()); }
    private static Instant instant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant at) return at;
        if (value instanceof OffsetDateTime at) return at.toInstant();
        if (value instanceof java.sql.Timestamp at) return at.toInstant();
        throw new IllegalStateException("Tipo temporal inesperado");
    }
}
