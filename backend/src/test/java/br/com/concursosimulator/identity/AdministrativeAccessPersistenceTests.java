package br.com.concursosimulator.identity;

import br.com.concursosimulator.identity.application.*;
import br.com.concursosimulator.identity.application.port.*;
import br.com.concursosimulator.identity.domain.*;
import br.com.concursosimulator.identity.infrastructure.persistence.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class AdministrativeAccessPersistenceTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    static final String PASSWORD="synthetic-local-password";
    String jdbcUrl;
    JdbcTemplate jdbc;jakarta.persistence.EntityManagerFactory factory;JpaTransactionManager manager;
    AdministrativeAccounts store;AdministrativeAccessUseCases access;
    final AdministrativePasswords passwords=new AdministrativePasswords(){
        public String encode(String p){return "synthetic-only:"+p;}
        public boolean matches(String p,String hash){return hash.equals(encode(p));}
    };
    @BeforeEach void open(){
        String schema="adm_"+UUID.randomUUID().toString().replace("-","");
        jdbcUrl="jdbc:postgresql://"+PG.getHost()+":"+PG.getMappedPort(5432)+"/"+PG.getDatabaseName()+"?currentSchema="+schema+"&logServerErrorDetail=false";
        var ds=new DriverManagerDataSource(jdbcUrl,PG.getUsername(),PG.getPassword());
        jdbc=new JdbcTemplate(ds);Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).load().migrate();
        var bean=new LocalContainerEntityManagerFactoryBean();bean.setDataSource(ds);bean.setPackagesToScan("br.com.concursosimulator.identity.infrastructure.persistence");
        bean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());bean.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));bean.afterPropertiesSet();
        factory=bean.getObject();manager=new JpaTransactionManager(factory);store=new JpaAdministrativeAccounts(SharedEntityManagerCreator.createSharedEntityManager(factory),manager);
        access=new AdministrativeAccessUseCases(store,passwords,Clock.systemUTC());
    }
    @AfterEach void close(){factory.close();}
    ExternalIdentity google(String subject){return new ExternalIdentity("google","https://accounts.google.com",subject);}
    UUID oldGoogle(String subject){var id=UUID.randomUUID();jdbc.update("insert into app_user(id) values (?)",id);jdbc.update("insert into external_identity values (?,'google','https://accounts.google.com',?)",id,subject);return id;}
    AdministrativeAccessUseCases.Access start(){assertThat(access.bootstrap("admin",PASSWORD,false)).isTrue();return access.login("admin",PASSWORD);}
    UUID attempt(UUID owner){var id=UUID.randomUUID();jdbc.update("insert into me_attempt(id,owner_kind,owner_id,account_id,created_at,payload) values (?,'ACCOUNT',?,?,now(),'{\"version\":0}')",id,owner,owner);return id;}
    @Test void concurrentBootstrapHasOneWinnerAndRestartNeverResetsEvenDisabled()throws Exception {
        var old=oldGoogle("old");attempt(old);var before=jdbc.queryForList("select * from me_attempt");
        try(var workers=Executors.newFixedThreadPool(2)){
            var barrier=new CyclicBarrier(2);
            var one=workers.submit(()->{barrier.await();return access.bootstrap("admin",PASSWORD,true);});
            var two=workers.submit(()->{barrier.await();return access.bootstrap("admin","other-synthetic-password",true);});
            assertThat(List.of(one.get(10,TimeUnit.SECONDS),two.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        }
        assertThat(jdbc.queryForObject("select count(*) from app_user",Integer.class)).isEqualTo(2);
        var registration=jdbc.queryForMap("select * from admin_registration");
        assertThat(access.bootstrap("changed","short",false)).isFalse();
        assertThat(jdbc.queryForMap("select * from admin_registration")).isEqualTo(registration);
        jdbc.update("update admin_registration set enabled=false");
        assertThat(access.bootstrap("other",PASSWORD,false)).isFalse();
        assertThat(jdbc.queryForList("select * from me_attempt")).isEqualTo(before);
    }
    @Test void passwordChangeRecoveryAndEpochProtectBothAuthenticationMethods(){
        var a=start();var linked=access.link(a.owner(),a.version(),google("new-google"));
        assertThat(linked.owner()).isEqualTo(a.owner());assertThat(access.linked(a.owner())).isTrue();
        assertThatThrownBy(()->access.current(a.owner(),a.version(),false)).hasMessage("ADMIN_ACCESS_DENIED");
        access.recover("new-temporary-password");
        var recovered=access.login("admin","new-temporary-password");assertThat(recovered.mustChange()).isTrue();
        assertThatThrownBy(()->access.current(recovered.owner(),recovered.version(),false)).hasMessage("ADMIN_ACCESS_DENIED");
        var changed=access.changePassword(recovered.owner(),recovered.version(),"new-temporary-password",PASSWORD);
        assertThat(changed.mustChange()).isFalse();assertThat(changed.owner()).isEqualTo(a.owner());
        assertThat(access.linked(changed.owner())).isTrue();
        assertThatThrownBy(()->access.current(linked.owner(),linked.version(),true)).hasMessage("ADMIN_ACCESS_DENIED");
        assertThat(jdbc.queryForObject("select count(*) from app_user",Integer.class)).isEqualTo(1);
    }
    @Test void existingGoogleKeepsItsUuidHistoryAndPasswordWhileOldLocalIsInactive(){
        var target=oldGoogle("existing-google");var attempt=attempt(target);
        jdbc.update("insert into me_command values (?,'ACCOUNT',?,?,? ,?,'{\"original\":true}')",UUID.randomUUID(),target,UUID.randomUUID(),attempt,"a".repeat(64));
        var attempts=jdbc.queryForList("select * from me_attempt");var commands=jdbc.queryForList("select * from me_command");var identities=jdbc.queryForList("select * from external_identity");
        var local=start();var linked=access.link(local.owner(),local.version(),google("existing-google"));
        assertThat(linked.owner()).isEqualTo(target);assertThat(access.login("admin",PASSWORD).owner()).isEqualTo(target);
        assertThat(access.googleAccess(target).owner()).isEqualTo(target);
        assertThat(jdbc.queryForList("select * from me_attempt")).isEqualTo(attempts);
        assertThat(jdbc.queryForList("select * from me_command")).isEqualTo(commands);
        assertThat(jdbc.queryForList("select * from external_identity")).isEqualTo(identities);
        assertThat(store.active(local.owner())).isFalse();assertThat(store.active(target)).isTrue();
        assertThatThrownBy(()->attempt(local.owner())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("insert into qb_import_receipt values (?,'test',?,now(),'{}')",UUID.randomUUID(),local.owner())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("select count(*) from app_user",Integer.class)).isEqualTo(2);
    }
    @Test void localStudyOrImportHistoryBlocksTransferWithoutChangingEitherAccount(){
        var target=oldGoogle("existing");attempt(target);var local=start();
        jdbc.update("insert into qb_import_receipt values (?,'test',?,now(),'{\"private\":true}')",UUID.randomUUID(),local.owner());
        var before=jdbc.queryForMap("select * from admin_registration");var receipts=jdbc.queryForList("select * from qb_import_receipt");
        assertThatThrownBy(()->access.link(local.owner(),local.version(),google("existing"))).hasMessage("LOCAL_ACCOUNT_HAS_HISTORY");
        assertThat(jdbc.queryForMap("select * from admin_registration")).isEqualTo(before);assertThat(store.active(local.owner())).isTrue();
        assertThat(jdbc.queryForList("select * from qb_import_receipt")).isEqualTo(receipts);
        // Same UUID is always safe: a previously unregistered Google is linked without moving local history.
        var same=access.link(local.owner(),local.version(),google("fresh"));assertThat(same.owner()).isEqualTo(local.owner());
        assertThatThrownBy(()->access.link(same.owner(),same.version(),google("another"))).hasMessage("GOOGLE_ALREADY_LINKED");
    }
    @Test void studyWinningTheAccountBarrierBlocksLinkAfterItsCommit()throws Exception {
        var target=oldGoogle("race-study");var local=start();var locked=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var write=pool.submit(()->new TransactionTemplate(manager).execute(s->{
                jdbc.queryForObject("select active from app_user where id=? for update",Boolean.class,local.owner());
                attempt(local.owner());locked.countDown();await(release);return null;
            }));
            assertThat(locked.await(5,TimeUnit.SECONDS)).isTrue();
            var link=pool.submit(()->access.link(local.owner(),local.version(),google("race-study")));
            try{assertThatThrownBy(()->link.get(200,TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);}
            finally{release.countDown();}
            write.get(5,TimeUnit.SECONDS);assertThatThrownBy(()->link.get(5,TimeUnit.SECONDS)).hasRootCauseMessage("LOCAL_ACCOUNT_HAS_HISTORY");
            assertThat(store.active(local.owner())).isTrue();assertThat(store.current().orElseThrow().owner()).isEqualTo(local.owner());
        }finally{release.countDown();}
    }
    @Test void linkWinningTheAccountBarrierRejectsLateStudyAndReceiptWrites()throws Exception {
        var target=oldGoogle("race-link");var local=start();var deactivated=new CountDownLatch(1);var release=new CountDownLatch(1);
        var hooked=new AdministrativeAccounts(){
            public Optional<AdministrativeAccount> current(){return store.current();}public boolean active(UUID id){return store.active(id);}
            public <T>T atomic(Function<Session,T> action){return store.atomic(s->action.apply(new Session(){
                public Optional<AdministrativeAccount> current(){return s.current();}public void save(AdministrativeAccount a){s.save(a);}
                public void createUser(UUID id){s.createUser(id);}public Optional<UUID> owner(ExternalIdentity i){return s.owner(i);}
                public boolean hasGoogle(UUID id){return s.hasGoogle(id);}public void attach(ExternalIdentity i,UUID id){s.attach(i,id);}
                public void lockUsers(UUID a,UUID b){s.lockUsers(a,b);}public boolean hasHistory(UUID id){return s.hasHistory(id);}
                public void deactivate(UUID id){s.deactivate(id);deactivated.countDown();await(release);}
            }));}
        };
        var linking=new AdministrativeAccessUseCases(hooked,passwords,Clock.systemUTC());
        try(var pool=Executors.newFixedThreadPool(3)){
            var link=pool.submit(()->linking.link(local.owner(),local.version(),google("race-link")));
            assertThat(deactivated.await(5,TimeUnit.SECONDS)).isTrue();
            var write=pool.submit(()->attempt(local.owner()));
            var receipt=pool.submit(()->jdbc.update("insert into qb_import_receipt values (?,'test',?,now(),'{}')",UUID.randomUUID(),local.owner()));
            try{assertThatThrownBy(()->write.get(200,TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);}
            finally{release.countDown();}
            assertThat(link.get(5,TimeUnit.SECONDS).owner()).isEqualTo(target);
            assertThatThrownBy(()->write.get(5,TimeUnit.SECONDS)).hasCauseInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThatThrownBy(()->receipt.get(5,TimeUnit.SECONDS)).hasCauseInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThat(jdbc.queryForObject("select count(*) from me_attempt",Integer.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from qb_import_receipt",Integer.class)).isZero();
        }finally{release.countDown();}
    }
    @Test void concurrentOrdinaryGoogleCreationIsRecoveredAfterRollbackWithoutThirdUser(){
        var local=start();var won=new java.util.concurrent.atomic.AtomicReference<UUID>();
        var em=SharedEntityManagerCreator.createSharedEntityManager(factory);
        var ordinary=new ResolveExternalIdentityUseCase(new JpaIdentityRepository(em),new br.com.concursosimulator.shared.infrastructure.SpringUnitOfWork(manager));
        var hooked=wrapped(s->new Forwarding(s){
            @Override public void attach(ExternalIdentity identity,UUID owner){
                if(won.get()==null)won.set(ordinary.execute(identity));
                super.attach(identity,owner);
            }
        });
        var linking=new AdministrativeAccessUseCases(hooked,passwords,Clock.systemUTC());
        var result=linking.link(local.owner(),local.version(),google("racing-normal-login"));
        assertThat(result.owner()).isEqualTo(won.get());assertThat(store.active(local.owner())).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from app_user",Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from external_identity",Integer.class)).isEqualTo(1);
    }
    @Test void unknownForeignKeyFailureRollsBackAndIsNotIdentityIdempotence(){
        var local=start();var before=jdbc.queryForMap("select * from admin_registration");
        var hooked=wrapped(s->new Forwarding(s){@Override public void attach(ExternalIdentity i,UUID owner){s.attach(i,UUID.randomUUID());}});
        var linking=new AdministrativeAccessUseCases(hooked,passwords,Clock.systemUTC());
        assertThatThrownBy(()->linking.link(local.owner(),local.version(),google("invalid-fk"))).isNotInstanceOf(IdentityAlreadyLinkedException.class);
        assertThat(jdbc.queryForMap("select * from admin_registration")).isEqualTo(before);
        assertThat(store.active(local.owner())).isTrue();assertThat(jdbc.queryForObject("select count(*) from external_identity",Integer.class)).isZero();
    }
    AdministrativeAccounts wrapped(Function<AdministrativeAccounts.Session,AdministrativeAccounts.Session> decorate){
        return new AdministrativeAccounts(){
            public Optional<AdministrativeAccount> current(){return store.current();}public boolean active(UUID id){return store.active(id);}
            public <T>T atomic(Function<Session,T> action){return store.atomic(s->action.apply(decorate.apply(s)));}
        };
    }
    static class Forwarding implements AdministrativeAccounts.Session {
        final AdministrativeAccounts.Session s;Forwarding(AdministrativeAccounts.Session s){this.s=s;}
        public Optional<AdministrativeAccount> current(){return s.current();}public void save(AdministrativeAccount a){s.save(a);}
        public void createUser(UUID id){s.createUser(id);}public Optional<UUID> owner(ExternalIdentity i){return s.owner(i);}
        public boolean hasGoogle(UUID id){return s.hasGoogle(id);}public void attach(ExternalIdentity i,UUID id){s.attach(i,id);}
        public void lockUsers(UUID a,UUID b){s.lockUsers(a,b);}public boolean hasHistory(UUID id){return s.hasHistory(id);}
        public void deactivate(UUID id){s.deactivate(id);}
    }
    @Test void recoveryExecutableRefusesMissingAccountThenChangesOnlyCredentialAfterConfirmation(){
        var env=Map.of("DB_URL",jdbcUrl,"DB_USERNAME",PG.getUsername(),"DB_PASSWORD",PG.getPassword());
        var output=new java.io.ByteArrayOutputStream();var errors=new java.io.ByteArrayOutputStream();
        var confirmation="RESET ADM "+jdbcUrl+"\n";
        assertThat(br.com.concursosimulator.identity.infrastructure.AdministrativeRecovery.launch(new String[]{"--recover-admin"},env,new java.io.BufferedReader(new java.io.StringReader(confirmation)),new java.io.PrintStream(output),new java.io.PrintStream(errors))).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from app_user",Integer.class)).isZero();assertThat(output.toString()).doesNotContain("temporary password");
        var a=start();var linked=access.link(a.owner(),a.version(),google("recovery-google"));attempt(a.owner());
        var identities=jdbc.queryForList("select * from external_identity");var history=jdbc.queryForList("select * from me_attempt");
        output.reset();errors.reset();
        assertThat(br.com.concursosimulator.identity.infrastructure.AdministrativeRecovery.launch(new String[]{"--recover-admin"},env,new java.io.BufferedReader(new java.io.StringReader(confirmation)),new java.io.PrintStream(output),new java.io.PrintStream(errors))).isZero();
        assertThat(output.toString()).contains("Commit confirmed");assertThat(errors.toString()).isEmpty();
        assertThat(jdbc.queryForObject("select password_hash from admin_registration",String.class)).startsWith("{pbkdf2-sha256-600k}");
        assertThat(jdbc.queryForObject("select auth_version from admin_registration",Long.class)).isEqualTo(linked.version()+1);
        assertThat(jdbc.queryForObject("select must_change from admin_registration",Boolean.class)).isTrue();
        assertThat(jdbc.queryForList("select * from external_identity")).isEqualTo(identities);assertThat(jdbc.queryForList("select * from me_attempt")).isEqualTo(history);
        assertThat(jdbc.queryForObject("select count(*) from app_user",Integer.class)).isEqualTo(1);
    }
    @Test void invalidPasswordInputsCannotMatchAnAccidentallyEqualSentinelCredential(){
        access.bootstrap("admin","invalid-password-input",false);
        assertThatThrownBy(()->access.login("admin",null)).hasMessage("INVALID_CREDENTIALS");
        assertThatThrownBy(()->access.login("admin","x".repeat(129))).hasMessage("INVALID_CREDENTIALS");
        assertThat(access.login("admin","invalid-password-input").owner()).isNotNull();
    }
    @Test void failedProofPersistsThrottleAndNeverChangesCredentialOrPermission(){
        var a=start();
        for(int i=0;i<5;i++)assertThatThrownBy(()->access.login("unknown",PASSWORD)).hasMessage("INVALID_CREDENTIALS");
        assertThatThrownBy(()->access.login("admin",PASSWORD)).hasMessage("LOGIN_THROTTLED");
        assertThat(store.current().orElseThrow().failures()).isEqualTo(5);
        assertThat(store.current().orElseThrow().owner()).isEqualTo(a.owner());
    }
    static void await(CountDownLatch latch){try{if(!latch.await(10,TimeUnit.SECONDS))throw new IllegalStateException("fixture timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
}
