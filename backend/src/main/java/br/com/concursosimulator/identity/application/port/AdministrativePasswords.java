package br.com.concursosimulator.identity.application.port;

public interface AdministrativePasswords {
    String encode(String password);
    boolean matches(String password, String hash);
}
