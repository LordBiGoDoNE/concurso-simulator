package br.com.concursosimulator.identity.infrastructure.persistence;

import br.com.concursosimulator.identity.application.port.*;
import br.com.concursosimulator.identity.domain.*;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Native JPA statements keep the singleton lock and cross-feature history barrier explicit. */
@Repository
public class JpaAdministrativeAccounts implements AdministrativeAccounts {
    private final EntityManager em;
    private final TransactionTemplate tx;
    public JpaAdministrativeAccounts(EntityManager em,PlatformTransactionManager manager) {
        this.em=em; tx=new TransactionTemplate(manager); tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }
    @Override public <T> T atomic(Function<Session,T> action) {
        try { return tx.execute(status -> {
            em.createNativeQuery("select id from admin_registration where id=1 for update",Integer.class).getSingleResult();
            return action.apply(new Work());
        }); } catch(RuntimeException failure) {
            for(Throwable c=failure;c!=null;c=c.getCause()) if(c instanceof ConstraintViolationException v && "external_identity_key".equals(v.getConstraintName()))
                throw new IdentityAlreadyLinkedException(failure);
            throw failure;
        }
    }
    @Override public Optional<AdministrativeAccount> current() { return tx.execute(s->read()); }
    @Override public boolean active(UUID user) { return tx.execute(s -> Boolean.TRUE.equals(em.createNativeQuery("select active from app_user where id=:id",Boolean.class).setParameter("id",user).getResultStream().findFirst().orElse(true))); }
    private Optional<AdministrativeAccount> read() {
        var r=(Object[])em.createNativeQuery("select owner_id,username,password_hash,must_change,enabled,auth_version,failed_attempts,failure_window,blocked_until from admin_registration where id=1").getSingleResult();
        if(r[0]==null) return Optional.empty();
        return Optional.of(new AdministrativeAccount((UUID)r[0],(String)r[1],(String)r[2],(Boolean)r[3],(Boolean)r[4],((Number)r[5]).longValue(),((Number)r[6]).intValue(),instant(r[7]),instant(r[8])));
    }
    private static Instant instant(Object value) {
        if(value==null)return null;
        if(value instanceof java.time.OffsetDateTime d)return d.toInstant();
        if(value instanceof Instant i)return i;
        return ((java.sql.Timestamp)value).toInstant();
    }
    private final class Work implements Session {
        public Optional<AdministrativeAccount> current() { return read(); }
        public void save(AdministrativeAccount a) {
            em.createNativeQuery("update admin_registration set owner_id=:owner,username=:name,password_hash=:hash,must_change=:change,enabled=:enabled,auth_version=:version,failed_attempts=:failures,failure_window=:window,blocked_until=:blocked where id=1")
                .setParameter("owner",a.owner()).setParameter("name",a.username()).setParameter("hash",a.passwordHash()).setParameter("change",a.mustChange()).setParameter("enabled",a.enabled()).setParameter("version",a.version()).setParameter("failures",a.failures()).setParameter("window",a.failureWindow()).setParameter("blocked",a.blockedUntil()).executeUpdate();
        }
        public void createUser(UUID id) { em.persist(new UserPersistenceEntity(id)); em.flush(); }
        public Optional<UUID> owner(ExternalIdentity identity) { return em.createNativeQuery("select user_id from external_identity where provider=:p and issuer=:i and subject=:s",UUID.class).setParameter("p",identity.provider()).setParameter("i",identity.issuer()).setParameter("s",identity.subject()).getResultStream().findFirst(); }
        public boolean hasGoogle(UUID owner) { return !em.createNativeQuery("select 1 from external_identity where user_id=:u and provider='google'").setParameter("u",owner).getResultList().isEmpty(); }
        public void attach(ExternalIdentity identity,UUID owner) { em.createNativeQuery("insert into external_identity(user_id,provider,issuer,subject) values(:u,:p,:i,:s)").setParameter("u",owner).setParameter("p",identity.provider()).setParameter("i",identity.issuer()).setParameter("s",identity.subject()).executeUpdate(); }
        public void lockUsers(UUID first,UUID second) {
            var ids=new TreeSet<UUID>(); ids.add(first); ids.add(second);
            for(var id:ids) {
                var active=em.createNativeQuery("select active from app_user where id=:id for update",Boolean.class).setParameter("id",id).getSingleResult();
                if(!Boolean.TRUE.equals(active)) throw new br.com.concursosimulator.identity.application.AdministrativeAccessUseCases.Failure("ADMIN_ACCESS_DENIED");
            }
        }
        public boolean hasHistory(UUID user) { return Boolean.TRUE.equals(em.createNativeQuery("select exists(select 1 from me_attempt where account_id=:u) or exists(select 1 from me_command where owner_kind='ACCOUNT' and owner_id=:u) or exists(select 1 from qb_import_receipt where actor=:u)",Boolean.class).setParameter("u",user).getSingleResult()); }
        public void deactivate(UUID user) { em.createNativeQuery("update app_user set active=false where id=:u").setParameter("u",user).executeUpdate(); }
    }
}
